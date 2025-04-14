/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import org.jcluster.core.bean.JcConnectionListener;
import org.jcluster.core.bean.JcHandhsakeFrame;
import org.jcluster.core.bean.SerializedConnectionBean;
import org.jcluster.core.exception.JcRuntimeException;

/*
            IO Con
 Client                   Server
    |                       |
    |   ----REQ Desc ---->  |
    |  Attached MngConId    |
    |                       |
    |                       |
    |  <--Resp SUCC -----   |
            COMPLETE 
    ~                       ~
    |                       |
    |                       |
    |  <--Resp FAIL -----   |
    |                       |
    |                       |
            FAIL

 */
/**
 *
 * @author platar86
 */
public class JcClientIOConnection extends JcClientConnection {

    private final boolean server;
    private JcClientManagedConnection mngCon = null;
    private JcHandhsakeFrame incominHandshake = null;

    private JcClientIOConnection(SerializedConnectionBean scb, JcHandhsakeFrame handShakeReq) throws Exception {
        super(handShakeReq.getRequestedConnType());
        this.setSocket(scb);

        this.incominHandshake = handShakeReq;
        this.server = true;
        this.startSelf();
    }

    private JcClientIOConnection(JcClientManagedConnection mngCon, JcConnectionTypeEnum type) throws Exception {
        super(type);
        this.mngCon = mngCon;

        this.server = false;
        this.startSelf();
    }

    public static JcClientIOConnection createFromIncomingConnection(SerializedConnectionBean scb, JcHandhsakeFrame handShakeReq) throws Exception {
        return createFromIncomingConnection(scb, handShakeReq, null);
    }

    public static JcClientIOConnection createFromIncomingConnection(SerializedConnectionBean scb, JcHandhsakeFrame handShakeReq, JcConnectionListener onConnectCb) throws Exception {
        JcClientIOConnection jcIO = new JcClientIOConnection(scb, handShakeReq);
        jcIO.onConnectListener = onConnectCb;
        return jcIO;
    }

    public static JcClientIOConnection createNewConnection(JcClientManagedConnection mngCon, JcConnectionTypeEnum type) throws Exception {
        return createNewConnection(mngCon, type, null);
    }

    public static JcClientIOConnection createNewConnection(JcClientManagedConnection mngCon, JcConnectionTypeEnum type, JcConnectionListener onConnectCb) throws Exception {
        JcClientIOConnection jcIO = new JcClientIOConnection(mngCon, type);
        jcIO.onConnectListener = onConnectCb;
        return jcIO;
    }

    @Override
    public void run() {
        boolean handshakeComplete = false;
        try {
            if (server) {
                onIncomingHandshake();
            } else {
                startNewConnection();
                mngCon.resetIoClientErr();          //described in exception
            }
            handshakeComplete = true;
            remoteAppDesc = mngCon.remoteAppDesc;
            setMember(mngCon.getMember());
            getMember().addConnection(this);

            //connection start reader here and will not exit untill exception
            super.startConnectionProccessor();

        } catch (Exception ex) {
            if (!server && !handshakeComplete) {
                //there is probably connection issue, in this case after few 
                //fails we can ask mngConnection to create one reversed IO conneciton 
                //by incrementing counter in mng connection
                mngCon.incrementIoClientErr();
            }
            closeReason = "IOConnection Exception: " + ex.getMessage();
        }

        LOG.warn("Connection shutdown Reason: [{}] Con: {}", closeReason, this);

        if (getMember() != null) {
            getMember().removeConnection(this, "IOConnection Exception: " + closeReason);
        }
    }

    private void onIncomingHandshake() throws Exception {
        if (this.incominHandshake == null) {
            throw new JcRuntimeException("Invalid incoming handshake");
        }
        if (incominHandshake.getFrameType() != JcHandhsakeFrame.TYPE_REQ_IO_JOIN) {
            throw new JcRuntimeException("Invalid incoming handshake frame type expected: [" + JcHandhsakeFrame.TYPE_REQ_IO_JOIN + "] found [" + incominHandshake.getFrameType() + "]ConType: " + isServer());
        }

        String mngConId = (String) incominHandshake.getData();
        JcMember memberByMngConId = JcCoreService.getInstance().getMemberByMngConId(mngConId);

        if (memberByMngConId == null) {
            JcHandhsakeFrame handshakeResponse = new JcHandhsakeFrame(JcCoreService.getSelfDesc());
            handshakeResponse.setFrameType(JcHandhsakeFrame.TYPE_RESP_IO_JOIN_FAIL);
            handshakeResponse.setData("Cna not find managed connection with ID: [" + mngConId + "]");
            writeAndFlushToOOS(handshakeResponse);
        }

        mngCon = memberByMngConId.getManagedConnection();
        //
        //Send resp with public key 
        JcHandhsakeFrame handshakeResponse = new JcHandhsakeFrame(JcCoreService.getSelfDesc());
        handshakeResponse.setFrameType(JcHandhsakeFrame.TYPE_RESP_IO_JOIN_SUCC);
        writeAndFlushToOOS(handshakeResponse);
    }

    private void startNewConnection() throws Exception {
        SocketAddress socketAddress = new InetSocketAddress(mngCon.remoteAppDesc.getIpAddress(), mngCon.remoteAppDesc.getIpPort());
        Socket soc = new Socket();

        soc.setReuseAddress(true);
        soc.connect(socketAddress, 2000);
        this.setSocket(soc);

        JcHandhsakeFrame joinReq = new JcHandhsakeFrame(JcCoreService.getSelfDesc());
        joinReq.setFrameType(JcHandhsakeFrame.TYPE_REQ_IO_JOIN);

//        JcConnectionTypeEnum type = mngCon.remoteAppDesc.isIsolated() ? JcConnectionTypeEnum.OUTBOUND : JcConnectionTypeEnum.INBOUND;
        joinReq.setRequestedConnType(this.getType() == JcConnectionTypeEnum.OUTBOUND ? JcConnectionTypeEnum.INBOUND : JcConnectionTypeEnum.OUTBOUND);
        joinReq.setData(mngCon.getRemoteConnectionId());
        writeAndFlushToOOS(joinReq);

        //wait for receiving client conneciton ID
        JcHandhsakeFrame resp = getHandshakeFromSocket(3);
        if (resp == null) {
            throw new JcRuntimeException("Timeout for expected request authentication");
        }
        if (resp.getFrameType() != JcHandhsakeFrame.TYPE_RESP_IO_JOIN_SUCC) {
            if (resp.getFrameType() == JcHandhsakeFrame.TYPE_RESP_IO_JOIN_FAIL) {
                throw new JcRuntimeException("Connection IO join response with Fail. Response: " + resp.getData());
            }
            throw new JcRuntimeException("Invalid handshake frame type expected: [" + JcHandhsakeFrame.TYPE_RESP_IO_JOIN_SUCC + "] found [" + resp.getFrameType() + "] ConType: " + isServer());
        }

    }

    @Override
    public boolean isServer() {
        return server;
    }

    @Override
    public String toString() {
        return "IOCon{[" + getConnId() + "]-" + (server ? 'S' : 'C') + "- RemDesc:[" + getRemoteAppDesc() + " MngCon:[" + mngCon + "]} ";
    }

}
