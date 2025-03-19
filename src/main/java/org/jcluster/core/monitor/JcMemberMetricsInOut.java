/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.monitor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.jcluster.core.JcConnectionTypeEnum;
import org.jcluster.core.bean.JcAppDescriptor;

/**
 *
 * @autor Henry Thomas
 */
public class JcMemberMetricsInOut implements Serializable {

    private JcConnectionTypeEnum connType;
    private int txCount = 0;
    private int rxCount = 0;
    private int errCount = 0;
    private int timeoutCount = 0;
    private int reqRespMapSize = 0;
    private int recreateCount = 0;
    private long lastConnAttempt = 0;
    private final Map<String, MethodExecMetric> methodExecMap = new HashMap<>();

    public void addMetrics(JcMemberMetricsInOut metrics) {
        txCount += metrics.txCount;
        rxCount += metrics.rxCount;
        errCount += metrics.errCount;
        timeoutCount += metrics.timeoutCount;
        reqRespMapSize += metrics.reqRespMapSize;

        for (Map.Entry<String, MethodExecMetric> entry : metrics.getMethodExecMap().entrySet()) {
            String methodName = entry.getKey();
            MethodExecMetric execMetric = entry.getValue();

            MethodExecMetric m = methodExecMap.get(methodName);
            if (m == null) {
                methodExecMap.put(methodName, execMetric);
                continue;
            }
            m.addMetric(execMetric);
        }
    }

    public JcConnectionTypeEnum getConnType() {
        return connType;
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

    public int getReqRespMapSize() {
        return reqRespMapSize;
    }

    public void setReqRespMapSize(int reqRespMapSize) {
        this.reqRespMapSize = reqRespMapSize;
    }

    public int getRecreateCount() {
        return recreateCount;
    }

    public void setRecreateCount(int recreateCount) {
        this.recreateCount = recreateCount;
    }

    public Map<String, MethodExecMetric> getMethodExecMap() {
        return methodExecMap;
    }

}
