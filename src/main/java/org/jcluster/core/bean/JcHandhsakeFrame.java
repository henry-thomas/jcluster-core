/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.bean;

import java.io.Serializable;
import org.jcluster.core.sockets.ConnectionType;

/**
 *
 * @author henry
 */
public class JcHandhsakeFrame implements Serializable {

    private boolean accepted;
    private ConnectionType requestedConnType = null;
    private final JcAppDescriptor remoteAppDesc;

    public JcHandhsakeFrame(JcAppDescriptor remoteAppDesc) {
        this.remoteAppDesc = remoteAppDesc;

    }

    public JcAppDescriptor getRemoteAppDesc() {
        return remoteAppDesc;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public ConnectionType getRequestedConnType() {
        return requestedConnType;
    }

    public void setRequestedConnType(ConnectionType requestedConnType) {
        this.requestedConnType = requestedConnType;
    }

    @Override
    public String toString() {
        return "JcHandhsakeFrame{" + "accepted=" + accepted + ", requestedConnType=" + requestedConnType + ", remoteAppDesc=" + remoteAppDesc + '}';
    }

}
