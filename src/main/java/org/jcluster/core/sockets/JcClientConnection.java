/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.sockets;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcluster.core.bean.ConnectionParam;
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

    private final JcAppDescriptor desc;
    private String connId;
    private final int port;
    private final String hostName;
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private static int parallelConnectionCount = 0;
    private int paralConnWaterMark = 0;
    private boolean secure;
    private boolean running;
    private final ConnectionType connType;
    private final JcConnectionMetrics metrics;
    private long lastSuccessfulSend = 0l;
    private boolean accepted = false;

    private final Object writeLock = new Object();

    private final ConcurrentHashMap<Long, JcMessage> reqRespMap = new ConcurrentHashMap<>();

    public JcClientConnection(Socket sock) {

        this.desc = JcFactory.getManager().getThisDescriptor();
        this.connType = ConnectionType.INBOUND;
        this.socket = sock;
        this.port = sock.getLocalPort();
        this.hostName = sock.getInetAddress().getHostAddress();

        parallelConnectionCount++;
        if (parallelConnectionCount > paralConnWaterMark) {
            paralConnWaterMark = parallelConnectionCount;
            System.out.println("Number of connections reached: " + paralConnWaterMark);
        }

        this.connId = desc.getAppName() + "-" + hostName + ":" + port + "-INBOUND-" + (++inboundConnCount);
        metrics = new JcConnectionMetrics(this.connId);
    }

    public JcClientConnection(JcAppDescriptor desc) {

        this.desc = desc;

        this.connType = ConnectionType.OUTBOUND;
        this.port = this.desc.getIpPort();
        this.hostName = this.desc.getIpAddress();
        parallelConnectionCount++;
        if (parallelConnectionCount > paralConnWaterMark) {
            paralConnWaterMark = parallelConnectionCount;
            System.out.println("Number of connections reached: " + paralConnWaterMark);
        }
        this.connId = desc.getAppName() + "-" + hostName + ":" + port + "-OUTBOUND";
        metrics = new JcConnectionMetrics(this.connId);
    }

    private void reconnect() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ex) {
                Logger.getLogger(JcClientConnection.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (!socket.isConnected() || socket.isClosed()) {
            connect();
        }
    }

    private boolean connect() throws JcSocketConnectException {
        if (socket == null || !socket.isConnected() || socket.isClosed()) {
            try {
                //            try {
                SocketAddress socketAddress = new InetSocketAddress(this.hostName, this.port);
                this.socket = new Socket();
//                this.socket.setTcpNoDelay(true);

                //try connect with timeout of 2000ms
                socket.connect(socketAddress, 2000);

                LOG.log(Level.INFO, "Connected to: {0}:{1}", new Object[]{this.hostName, this.port});

            } catch (IOException ex) {
//                Logger.getLogger(JcClientConnection.class.getName()).log(Level.SEVERE, null, ex);
                throw new JcSocketConnectException("Could not connect to: "
                        + desc.getAppName()
                        + " at " + desc.getIpAddress()
                        + ": " + desc.getIpPort()
                        + " Exception: "
                        + ex.getMessage());
            }
        }

        return true;
    }

    public void initHandshake() {

        Thread hsThread = new Thread(this::getResponse);
        hsThread.start();

        JcMessage jcMessage = new JcMessage("handshake", null, new Object[]{desc});
        JcMsgResponse send = null;

        try {
            send = send(jcMessage);
        } catch (IOException ex) {
            Logger.getLogger(JcClientConnection.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (send != null) {
            ConnectionParam connectionParam = (ConnectionParam) send.getData();
            if (connectionParam.isAccepted()) {
                accepted = true;
            } else {
                LOG.severe("Handshake failed");
            }
        }

    }

    //Called from Client Thread
    public JcMsgResponse send(JcMessage msg) throws IOException {
        return send(msg, 2000);
    }

    public JcMsgResponse send(JcMessage msg, int timeoutMs) throws IOException {
        try {

            long start = System.currentTimeMillis();

            metrics.incTxCount();
            reqRespMap.put(msg.getRequestId(), msg);

//            if (reqRespMap.get(msg.getRequestId()) == null) {
//            }
            synchronized (writeLock) {
                oos.writeObject(msg);
                oos.flush();
            }

            synchronized (msg) {
                msg.wait(timeoutMs);
            }

//            System.out.println("Sending from: " + Thread.currentThread().getName());
            if (msg.getResponse() == null) {
                reqRespMap.remove(msg.getRequestId());
                metrics.incTimeoutCount();
                LOG.log(Level.WARNING, "Timeout req-resp: {0}ms Message ID:{1} Thread-ID: {2}", new Object[]{System.currentTimeMillis() - start, msg.getRequestId(), Thread.currentThread().getName()});

                throw new JcResponseTimeoutException("No response received, timeout. APP_NAME: ["
                        + desc.getAppName() + "] ADDRESS: ["
                        + desc.getIpAddress() + ":" + String.valueOf(desc.getIpPort())
                        + "] METHOD: [" + msg.getMethodSignature()
                        + "] INSTANCE_ID: [" + desc.getInstanceId() + "]", msg);
            }
            lastSuccessfulSend = System.currentTimeMillis();
            return msg.getResponse();

        } catch (InterruptedException ex) {
            Logger.getLogger(JcClientConnection.class.getName()).log(Level.SEVERE, null, ex);
            JcMsgResponse resp = new JcMsgResponse(msg.getRequestId(), ex);
            msg.setResponse(resp);
//            reconnect();
            return resp;
        } catch (JcResponseTimeoutException ex) {
            //Forwarding the exception to whoever was calling this method.
            JcMsgResponse resp = new JcMsgResponse(msg.getRequestId(), ex);
            msg.setResponse(resp);

            return resp;

        }
    }

    @Override
    public void run() {

        Thread.currentThread().setName(this.connId);

        try {
            connect();
        } catch (JcSocketConnectException ex) {
            metrics.incErrCount();
//            Logger.getLogger(JcClientConnection.class.getName()).log(Level.SEVERE, null, ex);
        }

        running = true;
        try {
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());
        } catch (IOException ex) {
//            Logger.getLogger(JcClientConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (this.connType == ConnectionType.OUTBOUND) {
            //Handshake
            initHandshake();

            while (running) {
                getResponse();
            }

        } else {
            while (running) {
                try {
                    if (socket.isConnected()) {
                        JcMessage request = (JcMessage) ois.readObject();
                        metrics.incRxCount();
                        manager.getExecutorService().submit(new MethodExecutor(oos, request));
                    }
                } catch (IOException ex) {
//                    reconnect();
                    running = false;
//                    Logger.getLogger(JcClientConnection.class.getName()).log(Level.SEVERE, null, ex);
                    metrics.incErrCount();
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(JcClientConnection.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            JcAppInstanceData.getInstance().getInboundConnections().remove(connId);
        }

    }

    private void getResponse() {
        try {

            Object readObject = ois.readObject();
            metrics.incRxCount();

            if (readObject instanceof JcMessage) {

                JcMessage response = (JcMessage) readObject;
//                            JcMsgResponse respMsg = new JcMsgResponse(request.getRequestId(), readObject);

                JcMessage request = reqRespMap.remove(response.getRequestId());
                if (request != null) {
                    synchronized (request) {
                        request.setResponse(response.getResponse());
                        request.notifyAll();
                    }
                } else {
                    LOG.log(Level.WARNING, "Request is not in Map: {0}", response.getRequestId());
                }
            }
        } catch (IOException ex) {
//            running = false;
            metrics.incErrCount();
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(JcClientConnection.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void stopSocket() {
        try {
            socket.close();
        } catch (IOException e) {
        }
    }

    public void destroy() {

        running = false;

        try {
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(JcClientConnection.class.getName()).log(Level.SEVERE, null, ex);
        }

        parallelConnectionCount--;
    }

    public JcAppDescriptor getDesc() {
        return desc;
    }

    public String getConnId() {
        return connId;
    }

    public boolean isRunning() {
        return running;
    }

    public long getLastSuccessfulSend() {
        return lastSuccessfulSend;
    }

    public JcConnectionMetrics getMetrics() {
        return metrics;
    }

}
