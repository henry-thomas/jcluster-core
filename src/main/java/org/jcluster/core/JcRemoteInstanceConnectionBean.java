/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import static java.lang.System.currentTimeMillis;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcluster.core.bean.JcAppDescriptor;
import org.jcluster.core.bean.JcConnectionMetrics;
import org.jcluster.core.bean.JcHandhsakeFrame;
import org.jcluster.core.bean.jcCollections.RingConcurentList;
import org.jcluster.core.config.JcAppConfig;
import org.jcluster.core.exception.cluster.JcIOException;
import org.jcluster.core.messages.JcMessage;
import org.jcluster.core.messages.JcMsgResponse;
import org.jcluster.core.proxy.JcProxyMethod;

/**
 *
 * @author platar86
 */
public class JcRemoteInstanceConnectionBean {

    private static final Logger LOG = Logger.getLogger(JcRemoteInstanceConnectionBean.class.getName());

    //all connections must contain same instance as inbound + outbound
    private final List<JcClientConnection> allConnections = new ArrayList<>();
    //contains pointers only to outbound connection for quick access
    private final RingConcurentList<JcClientConnection> outboundList = new RingConcurentList<>();
    private final JcAppDescriptor desc;
    private boolean outboundEnabled = true;

    public JcRemoteInstanceConnectionBean(JcAppDescriptor desc, boolean outboundEnabled) {
        this.desc = desc;
        this.outboundEnabled = outboundEnabled;
    }

    public JcRemoteInstanceConnectionBean(JcAppDescriptor desc) {
        this.desc = desc;
        this.outboundEnabled = true;
    }

    public boolean isOutboundEnabled() {
        return outboundEnabled;
    }

    public List<JcConnectionMetrics> getAllMetrics() {
        List<JcConnectionMetrics> metricList = new ArrayList<>();
        for (JcClientConnection conn : allConnections) {
            metricList.add(conn.getMetrics());
        }
        return metricList;
    }

    public String getAppName() {
        return desc.getAppName();
    }

    protected void validateOutboundConnectionCount(int minCount) {
        if (!outboundEnabled) {
            return;
        }
        int actualCount = outboundList.size();
        if (actualCount < minCount) {
            for (int i = 0; i < (minCount - actualCount); i++) {
                JcClientConnection conn = startClientConnection();
                if (conn != null) {
                    JcManager.getInstance().getThreadFactory().newThread(conn).start();
                    addConnection(conn);
                } else {
                    return;
                }
            }
        }
    }

    private synchronized JcClientConnection startClientConnection() {
        try {
            SocketAddress socketAddress = new InetSocketAddress(desc.getIpAddress(), desc.getIpPort());
            Socket socket = new Socket();
            try {
                socket.connect(socketAddress, 2000);
            } catch (IOException e) {
                LOG.log(Level.INFO, "Attempt to connect fail: {0}", this);
                return null;
            }

            //after socket gets connected we have to receive first Handshake from the other site.
            //in case where socket gets broken, ois.readObject can freezes the current thread. This is why handshaking must happen always in separate thread to avoid 
            //JcManager main thread locking!
            FutureTask<JcClientConnection> futureHanshake = new FutureTask<>(() -> {
                try {
                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                 
                    JcMessage request = (JcMessage) ois.readObject();
                    if (request.getMethodSignature().equals("handshake")) {
                        JcAppDescriptor handshakeDesc = (JcAppDescriptor) request.getArgs()[0];
                        if (!handshakeDesc.getInstanceId().equals(desc.getInstanceId())) {
                            LOG.log(Level.INFO, "Handshake Response with Invalid for instanceId: " + desc.getInstanceId() + " found: " + handshakeDesc.getInstanceId());
                            return null;
                        }

                        LOG.log(Level.INFO, "Handshake Request from instance: {0}", desc.getAppName());

                        JcHandhsakeFrame hf = new JcHandhsakeFrame(JcManager.getInstance().getInstanceAppDesc());
                        hf.setRequestedConnType(JcConnectionTypeEnum.INBOUND); //send opposite connection type to the node

                        JcMsgResponse response = JcMsgResponse.createResponseMsg(request, hf);
                        oos.writeObject(response);
                        oos.flush();
                        return new JcClientConnection(socket, desc, JcConnectionTypeEnum.OUTBOUND);

                    }
                } catch (IOException | ClassNotFoundException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
                return null;
            });

            JcManager.getInstance().getExecutorService().execute(futureHanshake);
            try {
                JcClientConnection conn = futureHanshake.get(5, TimeUnit.SECONDS);
                if (conn == null) {
                    socket.close();
                } else {
                    return conn;
                }

            } catch (InterruptedException | ExecutionException | TimeoutException ex) {
                Logger.getLogger(JcRemoteInstanceConnectionBean.class.getName()).log(Level.SEVERE, null, ex);
            }

        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public void destroy() {
        for (JcClientConnection conn : allConnections) {
            conn.destroy();
        }
        outboundList.clear();
        allConnections.clear();
    }

    public int removeAllConnection() {
        int count = allConnections.size();
        synchronized (this) {
            for (JcClientConnection conn : allConnections) {
                conn.destroy();
            }
            allConnections.clear();
            outboundList.clear();
        }
        return count;
    }

    protected boolean removeConnection(JcClientConnection conn) {
        synchronized (this) {

            if (conn != null && conn.getConnType() != null) {
                allConnections.remove(conn);

                if (conn.getConnType() == JcConnectionTypeEnum.OUTBOUND) {
                    outboundList.remove(conn);
                }
                return true;
            }
            return false;
        }
    }

    protected boolean addConnection(JcClientConnection conn) {
        synchronized (this) {
            if (conn != null && conn.getConnType() != null) {
                allConnections.add(conn);
                if (conn.getConnType() == JcConnectionTypeEnum.OUTBOUND) {
                    outboundList.add(conn);
                }
                return true;
            }
            return false;
        }
    }

    public void pingAllOutbound() {
        synchronized (this) {
            for (JcClientConnection conn : outboundList) {
                conn.sendPing();
            }
        }
    }

    public void validateTimeoutsAllConn() {
        synchronized (this) {

            List<JcClientConnection> toRemove = new ArrayList<>();

            for (JcClientConnection conn : allConnections) {
                if ((currentTimeMillis() - conn.getLastDataTimestamp()) > JcAppConfig.getConnMaxTimeout()) {
                    toRemove.add(conn);
                }
            }
            if (!toRemove.isEmpty()) {
                for (JcClientConnection conn : toRemove) {
                    removeConnection(conn);
                    conn.destroy();
                }
            }
        }
    }

    public boolean isOutboundAvailable() {
        return !outboundList.isEmpty();
    }

    public Object send(JcProxyMethod proxyMethod, Object[] args) throws JcIOException {
        JcClientConnection conn = outboundList.getNext();
        if (conn == null) {
            throw new JcIOException("No outbound connections for: " + this.toString());
        }
        try {
            JcMessage msg = new JcMessage(proxyMethod.getMethodSignature(), proxyMethod.getClassName(), args);
            return conn.send(msg, proxyMethod.getTimeout()).getData();
        } catch (IOException ex) {
            throw new JcIOException(ex.getMessage());
        }
    }

    @Override
    public String toString() {
        return "JcRemoteInstanceConnectionBean{" + "appName=" + desc.getAppName() + ", instanceId=" + desc.getInstanceId() + '}';
    }

}
