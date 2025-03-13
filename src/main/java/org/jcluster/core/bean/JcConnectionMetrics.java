/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.bean;

import java.io.Serializable;
import org.jcluster.core.JcConnectionTypeEnum;
import org.jcluster.core.JcFactory;

/**
 *
 * @autor Henry Thomas
 */
public class JcConnectionMetrics implements Serializable {

    private final String homeServerName = null;
    private final String remoteServerName = null;
    private final String connId;
    private final String instanceId;
    private final String ipAddress;
    private final String appName;
    private final JcConnectionTypeEnum connType;
    private int txCount = 0;
    private int rxCount = 0;
    private int errCount = 0;
    private int timeoutCount = 0;
    private int reqRespMapSize = 0;
    private long lastConnAttempt = 0;

    public JcConnectionMetrics(JcAppDescriptor desc, JcConnectionTypeEnum connType, String connId) {
//        this.remoteServerName = desc.getServerName();
//        this.homeServerName = JcFactory.getManager().getInstanceAppDesc().getServerName();
        this.connId = connId;
        this.connType = connType;
        this.ipAddress = desc.getIpAddress() + ":" + desc.getIpPortListenUDP();
        this.appName = desc.getAppName();
        this.instanceId = desc.getInstanceId();
    }

    public String getConnId() {
        return connId;
    }

    public JcConnectionTypeEnum getConnType() {
        return connType;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public long getLastConnAttempt() {
        return lastConnAttempt;
    }

    public void updateLastConnAttempt() {
        this.lastConnAttempt = System.currentTimeMillis();
    }

    public void incTxCount() {
        ++txCount;
    }

    public void incRxCount() {
        ++rxCount;
    }

    public void incErrCount() {
        ++errCount;
    }

    public void incTimeoutCount() {
        ++timeoutCount;
    }

    public int getTxCount() {
        return txCount;
    }

    public int getRxCount() {
        return rxCount;
    }

    public int getErrCount() {
        return errCount;
    }

    public int getTimeoutCount() {
        return timeoutCount;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getAppName() {
        return appName;
    }

    public int getReqRespMapSize() {
        return reqRespMapSize;
    }

    public void setReqRespMapSize(int reqRespMapSize) {
        this.reqRespMapSize = reqRespMapSize;
    }

    public String getHomeServerName() {
        return homeServerName;
    }

    public String getRemoteServerName() {
        return remoteServerName;
    }

}
