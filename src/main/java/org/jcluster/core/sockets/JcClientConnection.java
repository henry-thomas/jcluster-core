/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.sockets;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import static java.lang.System.currentTimeMillis;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcluster.core.bean.JcHandhsakeFrame;
import org.jcluster.core.bean.JcAppDescriptor;
import org.jcluster.core.bean.JcAppInstanceData;
import org.jcluster.core.bean.JcConnectionMetrics;
import org.jcluster.core.cluster.ClusterManager;
import org.jcluster.core.cluster.JcFactory;
import org.jcluster.core.messages.JcMessage;
import org.jcluster.core.messages.JcMsgResponse;
import org.jcluster.core.exception.sockets.JcResponseTimeoutException;
import org.jcluster.core.exception.sockets.JcSocketConnectException;

/**
 *
 * @author henry
 */
public class JcClientConnection implements Runnable {

    private static final Logger LOG = Logger.getLogger(JcClientConnection.class.getName());
    private final ClusterManager manager = JcFactory.getManager();
    private static int inboundConnCount = 0;
    private static int outboundConnCount = 0;

    private final JcAppDescriptor remoteAppDesc;
    private String connId;
    private final int port;
    private final String hostName;
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;

    private static int parallelConnectionCount = 0;
    private int paralConnWaterMark = 0;

    private boolean running = true;
    private final ConnectionType connType;
    private final JcConnectionMetrics metrics;
    private long lastResponseReceivedTimestamp = currentTimeMillis();

    private final Object writeLock = new Object();

    private final ConcurrentHashMap<Long, JcMessage> reqRespMap = new ConcurrentHashMap<>();

    public JcClientConnection(Socket sock, JcHandhsakeFrame handshakeFrame) throws IOException {
        this(sock, handshakeFrame.getRemoteAppDesc(), handshakeFrame.getRequestedConnType());
    }

    public JcClientConnection(Socket sock, JcAppDescriptor desc, ConnectionType conType) throws IOException {

        this.socket = sock;
        oos = new ObjectOutputStream(socket.getOutputStream());
        ois = new ObjectInputStream(socket.getInputStream());

        this.remoteAppDesc = desc;
        this.connType = conType;

        this.port = this.remoteAppDesc.getIpPort();
        this.hostName = this.remoteAppDesc.getIpAddress();

        metrics = new JcConnectionMetrics(desc.getAppName(), desc.getInstanceId(), hostName + ":" + port);

        this.connId = remoteAppDesc.getAppName()
                + "-" + hostName + ":" + port
                + "-" + (this.connType == ConnectionType.INBOUND ? "INBOUND" : "OUTBOUND")
                + "-" + (this.connType == ConnectionType.INBOUND ? (++inboundConnCount) : (++outboundConnCount));

        //        parallelConnectionCount++;
//        if (parallelConnectionCount > paralConnWaterMark) {
//            paralConnWaterMark = parallelConnectionCount;
//            System.out.println("Number of connections reached: " + paralConnWaterMark);
//        }
    }

    //Called from Client Thread
    protected void writeAndFlushToOOS(Object msg) throws IOException {
        synchronized (writeLock) {
            oos.writeObject(msg);
            oos.flush();
            metrics.incTxCount();
        }
    }

    public JcMsgResponse send(JcMessage msg) throws IOException {
        return send(msg, 5000);
    }

    public JcMsgResponse send(JcMessage msg, int timeoutMs) throws IOException {
        try {
            long start = System.currentTimeMillis();

            metrics.incTxCount();
            reqRespMap.put(msg.getRequestId(), msg);

            writeAndFlushToOOS(msg);

            synchronized (msg) {
                msg.wait(timeoutMs);
            }

//            System.out.println("Sending from: " + Thread.currentThread().getName());
            metrics.setReqRespMapSize(reqRespMap.size());
            LOG.log(Level.INFO, "ReqResp Map Size for: {0} is [{1}]", new Object[]{getConnId(), metrics.getReqRespMapSize()});

            if (msg.getResponse() == null) {
                reqRespMap.remove(msg.getRequestId());
                metrics.incTimeoutCount();
                LOG.log(Level.WARNING, "Timeout req-resp: {0}ms Message ID:{1} Thread-ID: {2}", new Object[]{System.currentTimeMillis() - start, msg.getRequestId(), Thread.currentThread().getName()});

                throw new JcResponseTimeoutException("No response received, timeout. APP_NAME: ["
                        + remoteAppDesc.getAppName() + "] ADDRESS: ["
                        + remoteAppDesc.getIpAddress() + ":" + String.valueOf(remoteAppDesc.getIpPort())
                        + "] METHOD: [" + msg.getMethodSignature()
                        + "] INSTANCE_ID: [" + remoteAppDesc.getInstanceId() + "]", msg);

            } else {
                lastResponseReceivedTimestamp = System.currentTimeMillis();
                return msg.getResponse();
            }

        } catch (InterruptedException ex) {
            reqRespMap.remove(msg.getRequestId());
            Logger.getLogger(JcClientConnection.class.getName()).log(Level.SEVERE, null, ex);
            JcMsgResponse resp = new JcMsgResponse(msg.getRequestId(), ex);
            msg.setResponse(resp);
//            reconnect();
            return resp;
        } catch (JcResponseTimeoutException ex) {
            //Forwarding the exception to whoever was calling this method.
            reqRespMap.remove(msg.getRequestId());
            JcMsgResponse resp = new JcMsgResponse(msg.getRequestId(), ex);
            msg.setResponse(resp);

            return resp;

        }
    }

    public ConnectionType getConnType() {
        return connType;
    }

    @Override
    public void run() {
        Thread.currentThread().setName(this.connId);

        if (this.connType == ConnectionType.OUTBOUND) {
            startOutboundReader();
        } else {
            startInboundReader();
        }
    }

    private void startOutboundReader() {
        while (running) {

            try {
                if (ois == null) {
                    return;
                }
                Object readObject = ois.readObject();
                metrics.incRxCount();

                JcMsgResponse response = (JcMsgResponse) readObject;

                JcMessage request = reqRespMap.remove(response.getRequestId());
                if (request != null) {
                    synchronized (request) {
                        request.setResponse(response);
                        request.notifyAll();
                    }
                } else {
                    LOG.log(Level.WARNING, "Request is not in Map: {0}", response.getRequestId());
                }

            } catch (IOException ex) {
                metrics.incErrCount();
                destroy();
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(JcClientConnection.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    private void startInboundReader() {
        while (running) {
            try {
                if (socket.isConnected()) {
                    JcMessage request = (JcMessage) ois.readObject();
                    metrics.incRxCount();
                    manager.getExecutorService().submit(new JcInboundMethodExecutor(request, this));
                }
            } catch (IOException ex) {
                destroy();
                running = false;
//                    Logger.getLogger(JcClientConnection.class.getName()).log(Level.SEVERE, null, ex);
                metrics.incErrCount();

            } catch (ClassNotFoundException ex) {
                Logger.getLogger(JcClientConnection.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        JcAppInstanceData.getInstance().getInboundConnections().remove(connId);
    }

    public void destroy() {
        running = false;
        parallelConnectionCount--;
        try {
            socket.close();
        } catch (IOException e) {
        }

    }

    public JcAppDescriptor getRemoteAppDesc() {
        return remoteAppDesc;
    }

    public String getConnId() {
        return connId;
    }

    public boolean isRunning() {
        return running;
    }

    public long getLastSuccessfulSend() {
        return lastResponseReceivedTimestamp;
    }

    public JcConnectionMetrics getMetrics() {
        return metrics;
    }

}
