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
 *
 * Bean that handles connections with another app. Uses the appDescriptor to
 * figure out how to manage that connection.
 */
public class JcRemoteInstanceConnectionBean {

    private static final Logger LOG = Logger.getLogger(JcRemoteInstanceConnectionBean.class.getName());

    //all connections must contain same instance as inbound + outbound
    private final List<JcClientConnection> allConnections = new ArrayList<>();

    //contains pointers only to outbound connection for quick access
    private final RingConcurentList<JcClientConnection> outboundList = new RingConcurentList<>();

    //Inbound list is managed by an isolated instance, where the remote instance can't make the connection over the network.
    private final RingConcurentList<JcClientConnection> inboundList = new RingConcurentList<>();

    private final JcAppDescriptor remoteAppDesc;
    private boolean outboundEnabled = true;

    public JcRemoteInstanceConnectionBean(JcAppDescriptor desc, boolean outboundEnabled) {
        this.remoteAppDesc = desc;
        this.outboundEnabled = outboundEnabled;
    }

    public JcRemoteInstanceConnectionBean(JcAppDescriptor desc) {
        this.remoteAppDesc = desc;
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
        return remoteAppDesc.getAppName();
    }

    protected void validateInboundConnectionCount(int minCount) {

        int actualCount = inboundList.size();

        //Incase there is another isolated app connected, don't create an inbound connection to that app
        if (actualCount < minCount && !remoteAppDesc.isIsolated()) {
            for (int i = 0; i < (minCount - actualCount); i++) {
                JcClientConnection conn = startClientConnection(true);
                if (conn != null) {
                    JcManager.getInstance().getThreadFactory().newThread(conn).start();
                    addConnection(conn);
                } else {
                    return;
                }
            }
        }

    }

    protected void validateOutboundConnectionCount(int minCount) {
        if (!outboundEnabled) {
            return;
        }

        int actualCount = outboundList.size();

        //The isolated app will take care of the connection count.
        if (actualCount < minCount && !remoteAppDesc.isIsolated()) {
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
        return startClientConnection(false);
    }

    private synchronized JcClientConnection startClientConnection(boolean fromIsolated) {
        SocketAddress socketAddress = new InetSocketAddress(remoteAppDesc.getIpAddress(), remoteAppDesc.getIpPort());
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
        //Handles incoming handshake requests
        FutureTask<JcClientConnection> futureHanshake = new FutureTask<>(() -> {
            try {
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

                JcMessage request = (JcMessage) ois.readObject();
                if (request.getMethodSignature().equals("handshake")) {
                    JcAppDescriptor handshakeDesc = (JcAppDescriptor) request.getArgs()[0];
                    if (!handshakeDesc.getInstanceId().equals(remoteAppDesc.getInstanceId())) {
                        LOG.log(Level.INFO, "Handshake Response with Invalid for instanceId: {0} found: {1}",
                                new Object[]{remoteAppDesc.getInstanceId(), handshakeDesc.getInstanceId()});
                        return null;
                    }

                    LOG.log(Level.INFO, "Handshake Request from instance: {0} -> {1}", new Object[]{remoteAppDesc.getAppName(), remoteAppDesc.getIpAddress()});

                    JcHandhsakeFrame hf = new JcHandhsakeFrame(JcManager.getInstance().getInstanceAppDesc());

                    if (fromIsolated) {
                        LOG.log(Level.INFO, "Creating INBOUND connection from instance: {0} -> {1}", new Object[]{remoteAppDesc.getAppName(), remoteAppDesc.getIpAddress()});
                        hf.setRequestedConnType(JcConnectionTypeEnum.OUTBOUND); //send opposite connection type to the node
                    } else {
                        LOG.log(Level.INFO, "Creating OUTBOUND connection from instance: {0} -> {1}", new Object[]{remoteAppDesc.getAppName(), remoteAppDesc.getIpAddress()});
                        hf.setRequestedConnType(JcConnectionTypeEnum.INBOUND); //send opposite connection type to the node
                    }

                    JcMsgResponse response = JcMsgResponse.createResponseMsg(request, hf);
                    oos.writeObject(response);
                    oos.flush();
                    if (fromIsolated) {
                        return new JcClientConnection(socket, remoteAppDesc, JcConnectionTypeEnum.INBOUND);
                    } else {
                        return new JcClientConnection(socket, remoteAppDesc, JcConnectionTypeEnum.OUTBOUND);
                    }

                }
            } catch (IOException | ClassNotFoundException ex) {
                socket.close();
                LOG.log(Level.SEVERE, null, ex);
            }
            return null;
        });
        JcManager.getInstance().getExecutorService().execute(futureHanshake);
        try {
            JcClientConnection conn = futureHanshake.get(5, TimeUnit.SECONDS);
            if (conn == null) {
                LOG.log(Level.SEVERE, "Failed to connect to: {0}InstanceID: {1} ServerID: {2}", new Object[]{remoteAppDesc.getAppName(), remoteAppDesc.getInstanceId(), remoteAppDesc.getServerName()});

                socket.close();
            } else {
                return conn;
            }

        } catch (InterruptedException | ExecutionException | TimeoutException | IOException ex) {
            Logger.getLogger(JcRemoteInstanceConnectionBean.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public void destroy() {
        for (JcClientConnection conn : allConnections) {
            conn.destroy();
        }
        outboundList.clear();
        inboundList.clear();
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
            inboundList.clear();
        }
        return count;
    }

    protected boolean removeConnection(JcClientConnection conn) {
        synchronized (this) {

            if (conn != null && conn.getConnType() != null) {
                allConnections.remove(conn);

                if (conn.getConnType() == JcConnectionTypeEnum.OUTBOUND) {
                    outboundList.remove(conn);
                } else {
                    inboundList.remove(conn);
                }

                conn.destroy();
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
                } else {
                    inboundList.add(conn);
                }
                return true;
            }
            return false;
        }
    }

    public void pingAllOutbound() {
        synchronized (this) {
            List<JcClientConnection> toRemove = new ArrayList<>();
            for (JcClientConnection conn : outboundList) {
                if (!conn.sendPing()) {
                    toRemove.add(conn);
                }
            }

            for (JcClientConnection jcClientConnection : toRemove) {

                removeConnection(jcClientConnection);
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

    public JcAppDescriptor getRemoteAppDesc() {
        return remoteAppDesc;
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
            removeConnection(conn);
            throw new JcIOException(ex.getMessage());
        }
    }

    @Override
    public String toString() {
        return "JcRemoteInstanceConnectionBean{" + "appName=" + 
                remoteAppDesc.getAppName() + ", instanceId=" + 
                remoteAppDesc.getInstanceId() + " address=" + 
                remoteAppDesc.getIpAddress() + '}';
    }

}
