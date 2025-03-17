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

    private static final long serialVersionUID = -711425395787330558L;

    private JcConnectionTypeEnum requestedConnType = null;
    private final JcAppDescriptor remoteAppDesc;

    public JcHandhsakeFrame(JcAppDescriptor remoteAppDesc) {
        this.remoteAppDesc = remoteAppDesc;

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

    @Override
    public String toString() {
        return "JcHandhsakeFrame{requestedConnType=" + requestedConnType + ", remoteAppDesc=" + remoteAppDesc + '}';
    }

}
