/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

import ch.qos.logback.classic.Level;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import static java.lang.System.currentTimeMillis;
import java.net.Socket;
import java.util.Objects;
import org.jcluster.core.bean.JcHandhsakeFrame;
import org.jcluster.core.bean.JcAppDescriptor;
import ch.qos.logback.classic.Logger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang3.RandomStringUtils;
import org.jcluster.core.messages.JcMessage;
import org.jcluster.core.messages.JcMsgResponse;
import org.jcluster.core.exception.sockets.JcResponseTimeoutException;
import org.slf4j.LoggerFactory;
import org.jcluster.core.bean.JcConnectionListener;
import org.jcluster.core.bean.SerializedConnectionBean;

/**
 *
 * @autor Henry Thomas
 */
public abstract class JcClientConnection implements Runnable {

    protected JcCoreService core = JcCoreService.getInstance();

    private static int connCounter = 0;

    private final Logger LOG = (Logger) LoggerFactory.getLogger(this.getClass());
//    private static final Logger LOG = (Logger) LoggerFactory.getLogger(JcClientConnection.class);

    protected JcAppDescriptor remoteAppDesc;
    protected JcConnectionListener onConnectListener = null;
    protected JcConnectionListener onCloseListener = null;

    protected Socket socket;
    protected ObjectOutputStream oos;
    protected ObjectInputStream ois;

    protected final String connId = RandomStringUtils.random(32, true, true);

    protected boolean initComplete = false;

    protected boolean running = true;
    private final JcConnectionTypeEnum connType;
    protected JcMember member;
    protected final long startTimestamp = System.currentTimeMillis();
    protected String closeReason = "Graceful shutdown";

//    private final JcRemoteInstanceConnectionBean memberRemConn;
    protected long lastDataTimestamp = currentTimeMillis();

    protected long lastIODataSend = currentTimeMillis();

    private final Object writeLock = new Object();

    private final Map<Long, JcMessage> reqRespMap = new HashMap<>();

    //when connection get accepted at server
    public JcClientConnection(JcConnectionTypeEnum connType) throws IOException {
        this.connType = connType;
//        LOG.setLevel(Level.ALL);
    }

    public JcMember getMember() {
        return member;
    }

    public void setMember(JcMember member) {
        this.member = member;
    }

    protected final void startSelf() {
        Thread t = JcCoreService.getInstance().getThreadFactory().newThread(this);
        t.setName("JC-ClientCon-" + (++connCounter));
        t.start();
    }

    protected final void setSocket(SerializedConnectionBean scb) throws IOException {
        this.socket = scb.getSocket();
        this.oos = scb.getOos();
        this.ois = scb.getOis();
    }

    protected final void setSocket(Socket sock) throws IOException {
        this.socket = sock;
        this.oos = new ObjectOutputStream(socket.getOutputStream());
        this.ois = new ObjectInputStream(socket.getInputStream());
    }

    //Called from Client Thread
    protected void writeAndFlushToOOS(Object msg) throws IOException {
        synchronized (writeLock) {
            oos.writeUnshared(msg);
            oos.flush();
            oos.reset();
        }

        //this is not working, lastDataTimestamp must indicate only receive pings
//        lastDataTimestamp = System.currentTimeMillis();
        if (member != null) {
            member.getMetrics().getOutbound().incTxCount();
        }
    }

    public JcMsgResponse send(JcMessage msg) throws IOException {
        return send(msg, null);
    }

    private int parallelExecutionWaterMark = 0;
    private volatile int parallelExecutionCount = 0;

    public int getParallelExecutionCount() {
        return parallelExecutionCount;
    }

    public JcMsgResponse send(JcMessage request, Integer timeoutMs) throws IOException {
        if (timeoutMs == null) {
            timeoutMs = 2000;
        }
        try {
            long start = System.currentTimeMillis();
            reqRespMap.put(request.getRequestId(), request);
            synchronized (this) {
                writeAndFlushToOOS(request);
            }

            lastIODataSend = start;
            parallelExecutionCount++;
            if (parallelExecutionCount > parallelExecutionWaterMark) {
                parallelExecutionWaterMark = parallelExecutionCount;
                LOG.debug("Paralel execution change to: {}", parallelExecutionWaterMark);
            }

            synchronized (request.getLock()) {
                request.getLock().wait(timeoutMs);
            }

//            long duration = System.currentTimeMillis() - start;
//            long durationMsg = request.getResponse().getTimeStamp() - request.getTimeStamp();
//            if (Math.abs(duration - durationMsg) > 5) {
//                LOG.warn("Message synchronization time is out wait duration: {}, message duration: {}, request: {}", duration, durationMsg, request);
//            }
//            System.out.println("Sending from: " + Thread.currentThread().getName());
//            LOG.log(Level.FINE, "ReqResp Map Size for: {} is [{}]", new Object[]{getConnId(),member.getMetrics().getReqRespMapSize()});
            if (request.getResponse() == null) {
                reqRespMap.remove(request.getRequestId());

                member.getMetrics().getOutbound().incTimeoutCount();
                LOG.warn(connId + " Timeout req-resp: {}ms Message ID:{} Thread-ID: {} Exec:{}", new Object[]{System.currentTimeMillis() - start, request.getRequestId(), Thread.currentThread().getName(), request.getMethodSignature()});

                throw new JcResponseTimeoutException("No response received, timeout=" + timeoutMs + "ms. APP_NAME: ["
                        + remoteAppDesc.getAppName() + "] ADDRESS: ["
                        + remoteAppDesc.getIpAddress() + ":" + remoteAppDesc.getIpPort()
                        + "] METHOD: [" + request.getMethodSignature()
                        + "] INSTANCE_ID: [" + remoteAppDesc.getInstanceId() + "]", request);

            } else {
                return request.getResponse();
            }

        } catch (InterruptedException ex) {
            reqRespMap.remove(request.getRequestId());
            LOG.warn(connId, ex);
            JcMsgResponse resp = new JcMsgResponse(request.getRequestId(), ex);
            request.setResponse(resp);
//            reconnect();
            return resp;
        } catch (JcResponseTimeoutException ex) {
            //Forwarding the exception to whoever was calling this method.
            reqRespMap.remove(request.getRequestId());
            JcMsgResponse resp = new JcMsgResponse(request.getRequestId(), ex);
            request.setResponse(resp);

            return resp;
        }
    }

    public JcConnectionTypeEnum getType() {
        return connType;
    }

    protected void setThreadName() {

        Thread.currentThread().setName("JC-ClientCon-" + getType()
                + "@" + getRemoteAppDesc().getAppName()
                + "_" + getRemoteAppDesc().getInstanceId()
                + "_" + (isServer() ? "S" : "C"));
    }

    protected final void startConnectionProccessor() throws IOException {
        setThreadName();

        if (null != this.connType) {
            switch (this.connType) {
                case OUTBOUND:
                    startOutboundReader();
                    break;
                case INBOUND:
                    startInboundReader();
                    break;

                default:
                    break;
            }
        }
    }

    private void startOutboundReader() throws IOException {
        while (running) {
            try {
                if (ois == null) {
                    return;
                }
                lastDataTimestamp = currentTimeMillis();

                Object readObject = ois.readObject();
                member.getMetrics().getOutbound().incRxCount();

                JcMsgResponse response = (JcMsgResponse) readObject;

                JcMessage request;
                synchronized (this) {
                    request = reqRespMap.remove(response.getRequestId());
                }
                if (request != null) {
                    synchronized (request.getLock()) {
                        request.setResponse(response);
                        request.getLock().notifyAll();
                    }
                } else {
                    LOG.warn(connId + " Request is not in Map: {}", response.getRequestId());
                }

                parallelExecutionCount--;

//                } catch (IOException ex) {
//                    member.getMetrics().getOutbound().incErrCount();
             ////                    if (socket.isClosed() || socket.isInputShutdown() || socket.isOutputShutdown() || !socket.isBound() || !socket.isConnected()) {
//                    destroy("Input stream IO Exception: " + ex.getMessage());
////                    LOG.warn(connId + " Destroying JcClientConnection because: " + ex.getMessage(), ex);
////                    } else {
////                        LOG.warn(null, ex);
////                    }
                } catch (ClassNotFoundException ex) {
                parallelExecutionCount--;
                //this is standard 
                LOG.warn(connId, ex);
            }
        }
    }

    private void startInboundReader() throws IOException {
        while (running) {
            try {
                if (socket.isConnected()) {
                    JcMessage request = (JcMessage) ois.readObject();

                    member.getMetrics().getInbound().incRxCount();
                    lastDataTimestamp = currentTimeMillis();
                    if (request.getMethodSignature().equals("ping")) {
                        JcMsgResponse response = JcMsgResponse.createResponseMsg(request, "pong");
                        writeAndFlushToOOS(response);
                    } else {
                        try {
                            JcCoreService.getInstance().getExecutorService()
                                    .submit(new JcInboundMethodExecutor(request, this, JcCoreService.getInstance().isEnterprise()));
                        } catch (RejectedExecutionException e) {
                            JcMsgResponse response = JcMsgResponse.createResponseMsg(request, e);
                            writeAndFlushToOOS(response);
                            LOG.error(null, e);
                        }
                    }

                }
//            } catch (IOException ex) {
//                LOG.warn(connId + " Destroying JcClientConnection because: " + ex.getMessage(), ex);
//                destroy("Input stream IO Exception: " + ex.getMessage());
//                member.getMetrics().getInbound().incErrCount();

            } catch (ClassNotFoundException ex) {
                //can not send response with failure, because we can not extract the message ID 
//                JcMsgResponse response = JcMsgResponse.createResponseMsg(request, ex.getCause());
//                sendResponse(response);
                LOG.warn(connId, ex);
            }
        }

    }

    protected void destroy(String reason) {
        running = false;
        try {
            if (socket != null) {
                LOG.error("Socket closed at: {} on thread {}  Reason {}", System.currentTimeMillis(), Thread.currentThread().getName(), reason);
                socket.close();
            }
        } catch (IOException e) {
        }
    }

    public boolean isClosed() {
        if (socket != null) {
            return socket.isClosed();
        }
        return false;
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

    public long getLastIODataSend() {
        return lastIODataSend;
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
        return "JcClientConnection{" + "connId=" + connId + ", remote=" + remoteAppDesc + '}';
    }

    protected final JcHandhsakeFrame getHandshakeFromSocket(int timeoutSec) throws ExecutionException, TimeoutException, InterruptedException {
        return getHandshakeFromSocket(timeoutSec, ois);
    }

    protected final static JcHandhsakeFrame getHandshakeFromSocket(int timeoutSec, ObjectInputStream ois) throws ExecutionException, TimeoutException, InterruptedException {
        FutureTask<JcHandhsakeFrame> futureHanshake = new FutureTask<>(() -> {
            try {
                Object handshakeResponse = ois.readObject();

                if (handshakeResponse instanceof JcHandhsakeFrame) {
                    return (JcHandhsakeFrame) handshakeResponse;
                } else {
                    Logger l = (Logger) LoggerFactory.getLogger(JcClientConnection.class);
                    l.warn("Unknown Message Type on Handshake: {}", handshakeResponse.getClass().getName());
                }

            } catch (IOException | ClassNotFoundException ex) {
                Logger l = (Logger) LoggerFactory.getLogger(JcClientConnection.class);
                l.error("Can not get handshage from socket: {}", ex);
            }
            return null;
        });

        JcCoreService.getInstance().getExecutorService().execute(futureHanshake);
        JcHandhsakeFrame hf = futureHanshake.get(timeoutSec, TimeUnit.SECONDS);
        if (hf != null) {
            return hf;
        }
        return null;
    }

    /**
     *
     * @return true if the connection has been initiated remotely and this is
     * from server.accept() false if this instance initiated this connection
     */
    public abstract boolean isServer();

}
