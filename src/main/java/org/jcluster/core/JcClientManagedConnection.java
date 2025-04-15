/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import static org.jcluster.core.JcClientConnection.LOG;
import org.jcluster.core.bean.JcConnectionListener;
import org.jcluster.core.bean.JcHandhsakeFrame;
import org.jcluster.core.bean.SerializedConnectionBean;
import org.jcluster.core.exception.JcRuntimeException;
import org.jcluster.core.messages.JcDistMsg;
import org.jcluster.core.messages.JcDistMsgType;
import static org.jcluster.core.messages.JcDistMsgType.PING;
import static org.jcluster.core.messages.JcDistMsgType.PUBLISH_FILTER;
import static org.jcluster.core.messages.JcDistMsgType.SUBSCRIBE;
import static org.jcluster.core.messages.JcDistMsgType.SUBSCRIBE_RESP;
import org.jcluster.core.monitor.JcMemberMetrics;
import org.jcluster.core.monitor.JcMetrics;

/*
            Managed Con
 Client                   Server 
    |                       |
    |   ----REQ Desc ---->  |
    |                       |
    |                       |
    |  <--Resp Desc -----   |
    |                       |
    |                       |
    |   ---Req Auth ----->  |
    |                       |
    |                       |
    |   <--- Resp Auth----  |
    |      Success of fail  |
    |     send ConnId       |
    |                       |
    |                       |
    |  -- Send ConId ---->  |
    |                       |
            COMPLETE 

Server = connection has been accepted
Client = connection has been initiaed 

            1. To avoid concurrent creation of managed connection ones connection
               has been created, durrin member is put into core map,we need to check
               if conenciton already exist, if yes one of the connection needs to be destroy,
               if we always destroy older conneciton this can lead to race condition.
               To avoid this the two sides instance ID must be compared and higher ID 
               must always drop server and lower must always drop client.
 */
/**
 *
 *
 *
 * @author platar86
 */
public class JcClientManagedConnection extends JcClientConnection {

    private static final HashMap<String, List<JcClientManagedConnection>> remInstMngConMap = new HashMap<>();
    private final boolean server;

    private String ipAddress;
    private String password;
    private String remoteConnectionId;
    private int ipPort;
    private JcHandhsakeFrame incominHandshake;

    private boolean useEncryption = false;

    private boolean mustClose = false;
    private static int mngConnCounter = 100;
    private int counterId = 0;
    private int ioClientFailCounter = 0;

    private JcClientManagedConnection(SerializedConnectionBean scb, JcHandhsakeFrame handShakeReq) throws Exception {
        super(JcConnectionTypeEnum.MANAGED);
        this.setSocket(scb);

        this.incominHandshake = handShakeReq;
        this.server = true;
        this.counterId = mngConnCounter++;
        this.startSelf();
    }

    private JcClientManagedConnection(String ipAddress, int ipPort, String pass) throws Exception {
        super(JcConnectionTypeEnum.MANAGED);
        this.ipAddress = ipAddress;
        this.ipPort = ipPort;
        this.password = pass;

        this.server = false;
        this.counterId = mngConnCounter++;
        this.startSelf();
    }

    public static JcClientManagedConnection createNew(String ipAddress, int ipPort) throws Exception {
        return createNew(ipAddress, ipPort, null);
    }

//    public static JcClientManagedConnection createNew(String ipAddress, int ipPort, String pass) throws Exception {
//        return createNew(ipAddress, ipPort, pass, null);
//
//    }
    public static JcClientManagedConnection createNew(String ipAddress, int ipPort, String pass) throws Exception {
        JcClientManagedConnection con = new JcClientManagedConnection(ipAddress, ipPort, pass);
//        con.onConnectListener = onConnectCb;
        return con;
    }

//    public static JcClientManagedConnection createFormIncomingConnection(SerializedConnectionBean clientCon, JcHandhsakeFrame handShakeReq) throws Exception {
//        return createFormIncomingConnection(clientCon, handShakeReq);
//    }
    public static JcClientManagedConnection createFormIncomingConnection(SerializedConnectionBean scb, JcHandhsakeFrame handShakeReq) throws Exception {
        JcClientManagedConnection con = new JcClientManagedConnection(scb, handShakeReq);
//        con.onConnectListener = onConnectCb;
        return con;
    }

    private void registerJcMemberWithCon() {
        JcMetrics allMetrics = core.getAllMetrics();
        JcMemberMetrics memMetric = allMetrics.getOrCreateMemMetric(remoteAppDesc.getInstanceId());

        JcMember m = new JcMember(this, core, memMetric);
        setMember(m);

        core.onMemberAdd(m);
    }

    @Override
    protected void setThreadName() {

        Thread.currentThread().setName("JC-MngCon"
                + "@" + getRemoteAppDesc().getAppName()
                + "_" + getRemoteAppDesc().getInstanceId()
                + "_" + (isServer() ? "S" : "C")
                + "_" + this.counterId
        );
    }

    @Override
    public void run() {
        try {
            if (server) {
                onIncomingHandshake();
            } else {
                startNewConnection();
            }
            setThreadName();

            registerJcMemberWithCon();
            LOG.warn("Managed connection established to {}[{}] - {} in: {}ms",
                    remoteAppDesc.getAppName(),
                    remoteAppDesc.getInstanceId(),
                    (isServer() ? "S" : "C"),
                    System.currentTimeMillis() - startTimestamp);

            startManagedReader();
        } catch (Exception ex) {
            closeReason = "ManagedConnection Exception: " + ex.getMessage();
        } finally {
            if (remoteAppDesc != null) {

                removeConn();

                LOG.warn("Connection shutdown", closeReason);
                if (getMember() != null) {
                    getMember().onManagedConClose(closeReason);
                }
            } else {
                //if this is null, this is not established connection attempt. Do nothing here
                //this is attempt to connecto to prim member probably

            }
        }
    }

    private void startManagedReader() throws IOException {
        while (running) {
            try {
                if (socket.isConnected()) {
                    Object request = ois.readObject();
                    if (request instanceof JcDistMsg) {
                        JcDistMsg msg = (JcDistMsg) request;
                        if (member == null) {
                            LOG.trace("Member not set yet for Managed connection {}", this);
                        }

                        try {
                            switch (msg.getType()) {
                                case PING:
                                    JcCoreService.getInstance().onPingMsg(msg);
                                    break;
                                case SUBSCRIBE:
                                    JcCoreService.getInstance().onSubscRequestMsg(member, msg);
                                    break;
                                case SUBSCRIBE_RESP:
                                    member.onSubscResponseMsg(msg);
                                    break;
                                case PUBLISH_FILTER:
                                    member.onFilterPublishMsg(msg);
                                    break;
                                case CREATE_IO:
                                    JcConnectionTypeEnum type = (JcConnectionTypeEnum) msg.getData();
                                    JcClientIOConnection.createNewConnection(this, type.getOppositeIo(), (con) -> {
                                        member.addConnection(con);
                                    });
                                    break;
                                case LEAVE:
                                    if (msg.getData() != null) {
                                        closeReason = msg.getData().toString();
                                    } else {
                                        closeReason = "Received leave message from socket";
                                    }
                                    mustClose = true;
                                    break;
                                case SUBSCRIBE_STATE_REQ:
                                    member.onSubscribeStateReq(msg);
                                    LOG.warn("SUBSCRIBE_STATE_REQ: [{}]", remoteAppDesc);
                                    break;
                                case SUBSCRIBE_STATE_RESP:
                                    member.onSubscribeStateResp(msg);
                                    LOG.warn("SUBSCRIBE_STATE_RESP: [{}]", remoteAppDesc);
                                    break;
                                default:
                                    LOG.error("Receive unknown UDP msg type: [{}]", msg.getType());
                            }
                        } catch (Exception e) {
                            LOG.error(null, e);
                        }
                    } else {
                        LOG.warn("Invalid msg type received in managed connection found: {} expected JcDistMsg", request.getClass());
                    }

                }
//            } catch (IOException ex) {
//                LOG.warn(connId + " Destroying JcClientConnection because: " + ex.getMessage(), ex);
//
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

    protected void validate() {
        if (mustClose && isRunning()) {
            if (closeReason == null) {
                closeReason = "Managed connection has been mark for removal";

                JcDistMsg msg = new JcDistMsg(JcDistMsgType.LEAVE);
                msg.setSrcDesc(core.selfDesc);
                msg.setData(closeReason);
                try {
                    writeAndFlushToOOS(msg);
                } catch (IOException ex) {
                    Logger.getLogger(JcClientManagedConnection.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            super.destroy(closeReason);
            //this will force the run method to execute finally block
        }
    }

    private boolean isHigherPriority() {
        return JcCoreService.getSelfDesc().getInstanceId().compareTo(remoteAppDesc.getInstanceId()) > 0;
    }

    private void removeConn() {
        if (remoteAppDesc == null) {
            return;
        }
        String remInstanceId = remoteAppDesc.getInstanceId();
        synchronized (remInstMngConMap) {
            List<JcClientManagedConnection> list = remInstMngConMap.get(remInstanceId);
            list.remove(this);
        }
    }

    protected static JcMember getMemberById(String remInstanceId) {
        synchronized (remInstMngConMap) {
            List<JcClientManagedConnection> list = remInstMngConMap.get(remInstanceId);
            if (list != null) {
                if (!list.isEmpty()) {
                    return list.get(0).getMember();
                }
            }
        }
        return null;
    }

    private boolean validateConnAndAdd() {
        String remInstanceId = remoteAppDesc.getInstanceId();
        synchronized (remInstMngConMap) {
            List<JcClientManagedConnection> list = remInstMngConMap.get(remInstanceId);
            if (list == null) {
                list = new ArrayList<>();
                remInstMngConMap.put(remInstanceId, list);
                list.add(this);
                return true;
            }

            for (JcClientManagedConnection mngCon : list) {
                if (mngCon.server == server) {
                    LOG.info("Droping Concurent connection detected. " + this);
                    return false;
                }
                if ((isHigherPriority() && server) || (!isHigherPriority() && !server)) {
                    LOG.info("Droping Concurent connection detected. " + this);
                    return false;
                }
                mngCon.mustClose = true;
                mngCon.closeReason = "Concurent connecteion established " + this;
                try {
                    mngCon.socket.close();
                } catch (Exception ex) {
                    LOG.error(null, ex);
                }
            }
            if (!list.isEmpty()) {
                LOG.info("Multiple connection in list. ");
            }
            list.add(this);
        }
        return true;
    }

    private void startNewConnection() throws Exception {
        SocketAddress socketAddress = new InetSocketAddress(ipAddress, ipPort);
        Socket soc = new Socket();

        soc.setReuseAddress(true);

        soc.connect(socketAddress, 2000);
        this.setSocket(soc);

        JcHandhsakeFrame handshakeReqPubKey = new JcHandhsakeFrame(JcCoreService.getSelfDesc());
        handshakeReqPubKey.setRequestedConnType(JcConnectionTypeEnum.MANAGED);
        handshakeReqPubKey.setFrameType(JcHandhsakeFrame.TYPE_REQ_PUBKEY);
        writeAndFlushToOOS(handshakeReqPubKey);

        //
        //wait for public key response with timout
        JcHandhsakeFrame handshakeRespPubKey = getHandshakeFromSocket(3);
        if (handshakeRespPubKey == null) {
            throw new JcRuntimeException("Timeout for expected request public key");
        }
        if (handshakeRespPubKey.getFrameType() != JcHandhsakeFrame.TYPE_RESP_PUBKEY) {
            throw new JcRuntimeException("Invalid handshake frame type expected: [" + JcHandhsakeFrame.TYPE_RESP_PUBKEY + "] found [" + handshakeRespPubKey.getFrameType() + "]  ConType: " + isServer());
        }
        this.remoteAppDesc = handshakeRespPubKey.getRemoteAppDesc();

        //if no password provided use rem member instanceId as secret
        if (password == null) {
            password = remoteAppDesc.getInstanceId();
        }
        LOG.trace("Sending new Authentication request password: [" + password + "]");

        JcHandhsakeFrame handshakeReqAuth = new JcHandhsakeFrame(JcCoreService.getSelfDesc());
        handshakeReqAuth.setFrameType(JcHandhsakeFrame.TYPE_REQ_AUTH);
        //encript the secret
        byte[] doFinal = encriptData(password.getBytes());
        handshakeReqAuth.setData(doFinal);
        writeAndFlushToOOS(handshakeReqAuth);

        //
        //wait for Authentication response  with timout
        JcHandhsakeFrame handshakeRespAuthResp = getHandshakeFromSocket(3);
        if (handshakeRespPubKey == null) {
            throw new JcRuntimeException("Timeout for expected request public key");
        }
        if (handshakeRespAuthResp.getFrameType() != JcHandhsakeFrame.TYPE_RESP_AUTH_SUCCESS) {
            if (handshakeRespAuthResp.getFrameType() != JcHandhsakeFrame.TYPE_RESP_AUTH_FAIL) {
                throw new JcRuntimeException("Authenticatoin fail member: " + remoteAppDesc + "] found [" + ipAddress + ":" + ipPort + "]");
            }
            throw new JcRuntimeException("Invalid handshake frame type expected: [" + JcHandhsakeFrame.TYPE_RESP_AUTH_SUCCESS + "] found [" + handshakeRespAuthResp.getFrameType() + "] ConType: " + isServer());
        }

        //this should containe the connectionId 
        //Success
        this.remoteConnectionId = (String) handshakeRespAuthResp.getData();

        if (validateConnAndAdd()) {
            JcHandhsakeFrame handshakeUpdateConId = new JcHandhsakeFrame(JcCoreService.getSelfDesc());
            handshakeUpdateConId.setFrameType(JcHandhsakeFrame.TYPE_RESP_UPDATE_CONID);
            handshakeUpdateConId.setData(this.getConnId());
            writeAndFlushToOOS(handshakeUpdateConId);
        } else {
            JcHandhsakeFrame handshakeUpdateConId = new JcHandhsakeFrame(JcCoreService.getSelfDesc());
            handshakeUpdateConId.setFrameType(JcHandhsakeFrame.TYPE_ERR_CONCURR);
            handshakeUpdateConId.setData(this.getConnId());
            writeAndFlushToOOS(handshakeUpdateConId);
            throw new JcRuntimeException("Concurrent managed connection[{}] deopped  ConType: " + (isServer() ? "S" : "C"));
        }
    }

    private void onIncomingHandshake() throws Exception {
        if (this.incominHandshake == null) {
            throw new JcRuntimeException("Invalid incoming handshake");
        }
        if (incominHandshake.getFrameType() != JcHandhsakeFrame.TYPE_REQ_PUBKEY) {
            throw new JcRuntimeException("Invalid incoming handshake frame type expected: [" + JcHandhsakeFrame.TYPE_REQ_PUBKEY + "] found [" + incominHandshake.getFrameType() + "]");
        }

        remoteAppDesc = incominHandshake.getRemoteAppDesc();

        LOG.trace("Received Incoming MangedCon PubKey Request. Sending responses");
        //
        //Send resp with public key 
        JcHandhsakeFrame handshakeResponse = new JcHandhsakeFrame(JcCoreService.getSelfDesc());
        handshakeResponse.setFrameType(JcHandhsakeFrame.TYPE_RESP_PUBKEY);
        writeAndFlushToOOS(handshakeResponse);

        //wait for authentication request here
        JcHandhsakeFrame handshakeRequestAuth = getHandshakeFromSocket(3);

        if (handshakeRequestAuth == null) {
            throw new JcRuntimeException("Timeout for expected request authentication");
        }
        if (handshakeRequestAuth.getFrameType() != JcHandhsakeFrame.TYPE_REQ_AUTH) {
            throw new JcRuntimeException("Invalid handshake frame type expected: [" + JcHandhsakeFrame.TYPE_REQ_AUTH + "] found [" + handshakeRequestAuth.getFrameType() + "] ConType: " + isServer());
        }
        boolean succes = authenticate((byte[]) handshakeRequestAuth.getData());

        if (!succes) {
            throw new JcRuntimeException("Authentication fail");
        }

        if (validateConnAndAdd()) {
            handshakeResponse = new JcHandhsakeFrame(JcCoreService.getSelfDesc());
            handshakeResponse.setFrameType(JcHandhsakeFrame.TYPE_RESP_AUTH_SUCCESS);
            handshakeResponse.setData(this.getConnId());
            writeAndFlushToOOS(handshakeResponse);
        } else {
            JcHandhsakeFrame handshakeUpdateConId = new JcHandhsakeFrame(JcCoreService.getSelfDesc());
            handshakeUpdateConId.setFrameType(JcHandhsakeFrame.TYPE_ERR_CONCURR);
            handshakeUpdateConId.setData(this.getConnId());
            writeAndFlushToOOS(handshakeUpdateConId);
            throw new JcRuntimeException("Concurrent managed connection[{}] deopped  ConType: " + (isServer() ? "S" : "C"));
        }

        //wait for receiving client conneciton ID
        JcHandhsakeFrame handshakClientConId = getHandshakeFromSocket(3);
        if (handshakClientConId == null) {
            throw new JcRuntimeException("Timeout for expected request authentication");
        }
        if (handshakClientConId.getFrameType() != JcHandhsakeFrame.TYPE_RESP_UPDATE_CONID) {

            if (handshakClientConId.getFrameType() == JcHandhsakeFrame.TYPE_ERR_CONCURR) {
                throw new JcRuntimeException("Concurent connection err Closing:" + this);
            }
            throw new JcRuntimeException("Invalid handshake frame type expected: [" + JcHandhsakeFrame.TYPE_RESP_UPDATE_CONID + "] found [" + handshakClientConId.getFrameType() + "] ConType: " + isServer());
        }
        this.remoteConnectionId = (String) handshakClientConId.getData();

        LOG.trace("MangedCon established from {}", getRemoteAppDesc());
    }

    private byte[] encriptData(byte data[]) throws GeneralSecurityException {
        if (useEncryption) {
            KeyFactory keyFactory;

            keyFactory = KeyFactory.getInstance("RSA");
            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(remoteAppDesc.getPublicKey());
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            Cipher encryptCipher = Cipher.getInstance("RSA");
            encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);

            byte[] doFinal = encryptCipher.doFinal(data);
            return doFinal;
        } else {
            return data;
        }
    }

    private boolean authenticate(byte encriptedMsg[]) {

        byte[] secret;
        if (useEncryption) {
            secret = core.decryptData(encriptedMsg);
        } else {
            secret = encriptedMsg;
        }
        String secStr = new String(secret);
        if (secStr == null) {
            throw new JcRuntimeException("Auth fail! invalid password null!");
        }
        String jcPass = core.getSecret();
        if (jcPass == null) {
            jcPass = core.selfDesc.getInstanceId();
        }

        boolean equals = secStr.equals(jcPass);
        if (!equals) {
            LOG.trace("Invalid Authentication secret [" + secStr + "]");
        }
        return equals;
    }

    public String getRemoteConnectionId() {
        return remoteConnectionId;
    }

    @Override
    public boolean isServer() {
        return server;
    }

    public boolean mustClose() {

        return mustClose;
    }

    protected void incrementIoClientErr() {
        ioClientFailCounter++;
    }

    protected void resetIoClientErr() {
        ioClientFailCounter++;
    }

    public int getIoClientFailCounter() {
        return ioClientFailCounter;
    }

}
