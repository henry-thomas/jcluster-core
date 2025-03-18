/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

import ch.qos.logback.classic.Logger;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import static java.lang.System.currentTimeMillis;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jcluster.core.bean.JcAppDescriptor;
import org.jcluster.core.monitor.JcMemberMetricsInOut;
import org.jcluster.core.bean.JcHandhsakeFrame;
import org.jcluster.core.bean.jcCollections.RingConcurentList;
//import org.jcluster.core.config.JcAppConfig;
import org.jcluster.core.exception.cluster.JcIOException;
import org.jcluster.core.messages.JcMessage;
import org.jcluster.core.messages.JcMsgResponse;
import org.jcluster.core.monitor.JcMemberMetrics;
import org.jcluster.core.proxy.JcProxyMethod;
import org.slf4j.LoggerFactory;

/**
 *
 * @author platar86
 *
 * Bean that handles connections with another app. Uses the appDescriptor to
 * figure out how to manage that connection.
 */
public class JcRemoteInstanceConnectionBean {

    private boolean onDemandConnection = true;
    private boolean conRequested = true;

    private static final Logger LOG = (Logger) LoggerFactory.getLogger(JcRemoteInstanceConnectionBean.class);

    //all connections must contain same instance as inbound + outbound
    private final List<JcClientConnection> allConnections = new ArrayList<>();

    //contains pointers only to outbound connection for quick access
    private final RingConcurentList<JcClientConnection> outboundList = new RingConcurentList<>();

    //Inbound list is managed by an isolated instance, where the remote instance can't make the connection over the network.
    private final RingConcurentList<JcClientConnection> inboundList = new RingConcurentList<>();

    private final JcMemberMetrics metrics;

    private JcAppDescriptor desc = null;

    public JcRemoteInstanceConnectionBean(JcMemberMetrics metrics) {
        this.metrics = metrics;
    }

    public boolean isOnDemandConnection() {
        return onDemandConnection;
    }

    public void setOnDemandConnection(boolean onDemandConnection) {
        this.onDemandConnection = onDemandConnection;
    }

    public void setDesc(JcAppDescriptor desc) {
        this.desc = desc;
    }

    protected void validateInboundConnectionCount(int minCount) {
        if (desc == null) {
            return;
        }
        int actualCount = inboundList.size();

        //Incase there is another isolated app connected, don't create an inbound connection to that app
        if (actualCount < minCount) {
            for (int i = 0; i < (minCount - actualCount); i++) {
                JcClientConnection conn = startClientConnection(true);
                if (conn != null) {
                    JcCoreService.getInstance().getThreadFactory().newThread(conn).start();
                    addConnection(conn);
                } else {
                    return;
                }
            }
        }

    }

    public void validateOutboundConnectionCount(int minCount) {
        if (desc == null) {
            return;
        }

        if (onDemandConnection && !conRequested) {
            return;
        }
        int actualCount = outboundList.size();

        //The isolated app will take care of the connection count.
        if (actualCount < minCount) {
            for (int i = 0; i < (minCount - actualCount); i++) {
                JcClientConnection conn = startClientConnection();
                if (conn != null) {
                    JcCoreService.getInstance().getThreadFactory().newThread(conn).start();
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
        if (desc == null) {
            return null;
        }

        SocketAddress socketAddress = new InetSocketAddress(desc.getIpAddress(), desc.getIpPortListenUDP());
        Socket socket = new Socket();
        try {
            socket.connect(socketAddress, 2000);
        } catch (IOException e) {
            LOG.info("Attempt to connect fail: {}", this);
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

                    if (!handshakeDesc.getInstanceId().equals(desc.getInstanceId())) {
                        LOG.info("Handshake Response with Invalid for instanceId: {} found: {}",
                                new Object[]{desc.getInstanceId(), handshakeDesc.getInstanceId()});
                        return null;
                    }

                    LOG.info("Handshake Request from instance: {} -> {}", new Object[]{desc.getAppName(), desc.getIpAddress()});

                    JcHandhsakeFrame hf = new JcHandhsakeFrame(JcCoreService.getInstance().getSelfDesc());

                    LOG.info("Creating OUTBOUND connection from instance: {} -> {}", new Object[]{desc.getAppName(), desc.getIpAddress()});
                    //Send with the type of connection the other side needs to be
                    hf.setRequestedConnType(JcConnectionTypeEnum.INBOUND); //send opposite connection type to the node

                    JcMsgResponse response = JcMsgResponse.createResponseMsg(request, hf);

                    oos.writeObject(response);
                    oos.flush();
                    LOG.info("Responding handshake frame: {}", hf);
//                    if (hf.getRequestedConnType()) {
                    return new JcClientConnection(socket, desc, JcConnectionTypeEnum.OUTBOUND, metrics);
//                    } else {
//                        return new JcClientConnection(socket, desc, JcConnectionTypeEnum.OUTBOUND);
//                    }

                }
            } catch (IOException | ClassNotFoundException ex) {
                socket.close();
                LOG.error(null, ex);
            }
            return null;
        });
        JcCoreService.getInstance().getExecutorService().execute(futureHanshake);
        try {
            JcClientConnection conn = futureHanshake.get(5, TimeUnit.SECONDS);
            if (conn == null) {
                LOG.error("Failed to connect to: {} ", new Object[]{desc});

                socket.close();
            } else {

                return conn;
            }

        } catch (InterruptedException | ExecutionException | TimeoutException | IOException ex) {
            LOG.error(null, ex);
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

    public void validateTimeoutsAllConn() {
        synchronized (this) {

            List<JcClientConnection> toRemove = new ArrayList<>();

            for (JcClientConnection conn : allConnections) {
                if ((currentTimeMillis() - conn.getLastDataTimestamp()) > 60_000) {
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
            conRequested = true;
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
        return "JcRemoteInstanceConnectionBean{" + "appName="
                + desc.getAppName() + ", instanceId="
                + desc.getInstanceId() + " address="
                + desc.getIpAddress() + '}';
    }

}
