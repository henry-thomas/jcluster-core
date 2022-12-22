/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.bean;

/**
 *
 * @author henry
 */
public class JcConnectionMetrics {

    private String instanceId;
    private final String ipAddress;
    private final String appName;
    private int txCount = 0;
    private int rxCount = 0;
    private int errCount = 0;
    private int timeoutCount = 0;
    private int reqRespMapSize = 0;
    private long lastConnAttempt = 0;

    public JcConnectionMetrics(String appName, String instanceId, String ipAddress) {
        this.ipAddress = ipAddress;
        this.appName = appName;
        this.instanceId = instanceId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void changeInstanceId(String instanceId) {
        this.instanceId = instanceId;
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

}
