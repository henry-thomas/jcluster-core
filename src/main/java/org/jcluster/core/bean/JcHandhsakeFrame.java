/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.bean;

import java.io.Serializable;
import org.jcluster.core.JcConnectionTypeEnum;

/**
 *
 * @autor Henry Thomas
 */
public class JcHandhsakeFrame implements Serializable {

    public final static int TYPE_UNKNOWN = 0;
    public final static int TYPE_REQ_PUBKEY = 1;
    public final static int TYPE_REQ_AUTH = 2;
    public final static int TYPE_RESP_AUTH_SUCCESS = 20;
    public final static int TYPE_RESP_AUTH_FAIL = 21;

    public final static int TYPE_RESP_UPDATE_CONID = 3;
    public final static int TYPE_REQ_IO_JOIN = 6;//join io connectection referenced to managed connection
    public final static int TYPE_RESP_IO_JOIN_SUCC = 60;//join io connectection referenced to managed connection
    public final static int TYPE_RESP_IO_JOIN_FAIL = 61;//join io connectection referenced to managed connection
    public final static int TYPE_RESP_PUBKEY = 10;

    private static final long serialVersionUID = -711425395787330558L;

    private JcConnectionTypeEnum requestedConnType = null;
    private JcAppDescriptor remoteAppDesc;
    private Object data = null;
    private int frameType = 0;

    public JcHandhsakeFrame() {

    }

    public JcHandhsakeFrame(JcAppDescriptor remoteAppDesc) {
        this.remoteAppDesc = remoteAppDesc;
    }

    public int getFrameType() {
        return frameType;
    }

    public void setFrameType(int frameType) {
        this.frameType = frameType;
    }

    public JcAppDescriptor getRemoteAppDesc() {
        return remoteAppDesc;
    }

    public JcConnectionTypeEnum getRequestedConnType() {
        return requestedConnType;
    }

    public void setRequestedConnType(JcConnectionTypeEnum requestedConnType) {
        this.requestedConnType = requestedConnType;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "JcHandhsakeFrame{requestedConnType=" + requestedConnType + ", remoteAppDesc=" + remoteAppDesc + '}';
    }

}
