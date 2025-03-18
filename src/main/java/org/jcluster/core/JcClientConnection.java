/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import static java.lang.System.currentTimeMillis;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.jcluster.core.bean.JcHandhsakeFrame;
import org.jcluster.core.bean.JcAppDescriptor;
import ch.qos.logback.classic.Logger;
import org.apache.commons.lang3.RandomStringUtils;
import org.jcluster.core.messages.JcMessage;
import org.jcluster.core.messages.JcMsgResponse;
import org.jcluster.core.exception.sockets.JcResponseTimeoutException;
import org.jcluster.core.monitor.JcMemberMetrics;
import org.slf4j.LoggerFactory;

/**
 *
 * @autor Henry Thomas
 */
public class JcClientConnection implements Runnable {

    private static final Logger LOG = (Logger) LoggerFactory.getLogger(JcClientConnection.class);
    private static int conIdUniqueCounter = 0;
//    private static int outboundConnCount = 0;

    private final JcAppDescriptor remoteAppDesc;
    private String connId;
    private final int port;
    private final String hostName;
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private String id = RandomStringUtils.random(8, true, true);

    private boolean running = true;
    private final JcConnectionTypeEnum connType;
    private final JcMemberMetrics metrics;
    protected long lastDataTimestamp = currentTimeMillis();

    private final Object writeLock = new Object();

    private final ConcurrentHashMap<Long, JcMessage> reqRespMap = new ConcurrentHashMap<>();

    public JcClientConnection(Socket sock, JcHandhsakeFrame handshakeFrame, JcMemberMetrics met) throws IOException {
        this(sock, handshakeFrame.getRemoteAppDesc(), handshakeFrame.getRequestedConnType(), met);
    }

    public JcClientConnection(Socket sock, JcAppDescriptor desc, JcConnectionTypeEnum conType, JcMemberMetrics met) throws IOException {
        LOG.setLevel(ch.qos.logback.classic.Level.ALL);

        this.socket = sock;
        oos = new ObjectOutputStream(socket.getOutputStream());
        ois = new ObjectInputStream(socket.getInputStream());

        this.remoteAppDesc = desc;
        this.connType = conType;

        this.port = this.remoteAppDesc.getIpPortListenUDP();
        this.hostName = this.remoteAppDesc.getIpAddress();

        this.connId = remoteAppDesc.getAppName()
                + "-" + hostName + ":" + port
                + "-" + (this.connType == JcConnectionTypeEnum.INBOUND ? "INBOUND" : "OUTBOUND")
                + "-" + (conIdUniqueCounter++);

        LOG.trace(id + " New JcClientConnection: {}", connId);
        this.metrics = met;
    }

    //Called from Client Thread
    protected void writeAndFlushToOOS(Object msg) throws IOException {
//        ByteArrayOutputStream bos = new ByteArrayOutputStream();
//        ObjectOutputStream out = new ObjectOutputStream(bos);
//        synchronized (writeLock) {
//            out.writeObject(msg);
//            out.flush();
//            out.reset();
//            bos.size();
//            bos.writeTo(socket.getOutputStream());
//        }
        synchronized (writeLock) {
            oos.writeUnshared(msg);
            oos.flush();
            oos.reset();
        }
        metrics.getOutbound().incTxCount();
    }

   

    public JcMsgResponse send(JcMessage msg, Integer timeoutMs) throws IOException {
        if (timeoutMs == null) {
            timeoutMs = 2000;
        }
        try {
            long start = System.currentTimeMillis();

            reqRespMap.put(msg.getRequestId(), msg);

            writeAndFlushToOOS(msg);

            synchronized (msg) {
                msg.wait(timeoutMs);
            }

//            System.out.println("Sending from: " + Thread.currentThread().getName());
//            LOG.log(Level.FINE, "ReqResp Map Size for: {} is [{}]", new Object[]{getConnId(), metrics.getReqRespMapSize()});
            if (msg.getResponse() == null) {
                reqRespMap.remove(msg.getRequestId());
                metrics.getOutbound().incTimeoutCount();
                LOG.warn(id + " Timeout req-resp: {}ms Message ID:{} Thread-ID: {}", new Object[]{System.currentTimeMillis() - start, msg.getRequestId(), Thread.currentThread().getName()});

                throw new JcResponseTimeoutException("No response received, timeout=" + timeoutMs + "ms. APP_NAME: ["
                        + remoteAppDesc.getAppName() + "] ADDRESS: ["
                        + remoteAppDesc.getIpAddress()
                        + "] METHOD: [" + msg.getMethodSignature()
                        + "] INSTANCE_ID: [" + remoteAppDesc.getInstanceId() + "]", msg);

            } else {
                lastDataTimestamp = System.currentTimeMillis();
                return msg.getResponse();
            }

        } catch (InterruptedException ex) {
            reqRespMap.remove(msg.getRequestId());
            LOG.warn(id, ex);
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

    public JcConnectionTypeEnum getConnType() {
        return connType;
    }

    @Override
    public void run() {
        Thread.currentThread().setName(this.connId);
        long now = System.currentTimeMillis();
        if (this.connType == JcConnectionTypeEnum.OUTBOUND) {
            startOutboundReader();
        } else {
            startInboundReader();
        }

        destroy();
        LOG.info(id + " JcClientConnection Closed. Lasted  {}ms", System.currentTimeMillis() - now);
    }

    private void startOutboundReader() {
        while (running) {
            try {

                try {
                    if (ois == null) {
                        return;
                    }
                    lastDataTimestamp = currentTimeMillis();

                    Object readObject = ois.readObject();
                    metrics.getOutbound().incRxCount();

                    JcMsgResponse response = (JcMsgResponse) readObject;

                    JcMessage request = reqRespMap.remove(response.getRequestId());
                    if (request != null) {
                        synchronized (request) {
                            request.setResponse(response);
                            request.notifyAll();
                        }
                    } else {
                        LOG.warn(id + " Request is not in Map: {}", response.getRequestId());
                    }

                } catch (IOException ex) {
                    metrics.getOutbound().incErrCount();
                    destroy();
                } catch (ClassNotFoundException ex) {
                    LOG.warn(id, ex);
                }
            } catch (Exception e) {
                LOG.warn(id, e);
            }
        }

    }

    private void startInboundReader() {
        while (running) {
            try {
                if (socket.isConnected()) {
                    JcMessage request = (JcMessage) ois.readObject();
                    metrics.getInbound().incRxCount();
                    lastDataTimestamp = currentTimeMillis();
                    JcCoreService.getInstance().getExecutorService()
                            .submit(new JcInboundMethodExecutor(request, this, JcCoreService.getInstance().isEnterprise()));
                }
            } catch (IOException ex) {
                destroy();
//                LOG.error(id, ex);
                metrics.getInbound().incErrCount();

            } catch (ClassNotFoundException ex) {
                LOG.warn(id, ex);
            }
        }

    }

    public void destroy() {
        running = false;
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

    public long getLastDataTimestamp() {
        return lastDataTimestamp;
    }

    public JcMemberMetrics getMetrics() {
        return metrics;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof JcClientConnection)) {
            return false;
        }
        JcClientConnection c = (JcClientConnection) obj;
        return Objects.equals(this.connId, c.connId);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 23 * hash + Objects.hashCode(this.connId);
        return hash;
    }

    @Override
    public String toString() {
        return "JcClientConnection{" + "connId=" + connId + ", connType=" + connType + '}';
    }

}
