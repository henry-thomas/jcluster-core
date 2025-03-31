/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.monitor;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.jcluster.core.JcConnectionTypeEnum;

/**
 *
 * @autor Henry Thomas
 */
public class JcConnMetrics implements Serializable {

    private static final long serialVersionUID = 4063085972825278316L;

    private JcConnectionTypeEnum connType;
    private int txCount = 0;
    private int rxCount = 0;
    private int errCount = 0;
    private int timeoutCount = 0;
    private int recreateCount = 0;
    private long lastConnAttempt = 0;
    private final Map<String, MethodExecMetric> methodExecMap = new HashMap<>();

    public JcConnMetrics() {
    }

    public void clear() {
        txCount = 0;
        rxCount = 0;
        errCount = 0;
        timeoutCount = 0;
        recreateCount = 0;
        lastConnAttempt = 0;
        methodExecMap.clear();
    }

    public JcConnMetrics sumMetrics(Collection<JcConnMetrics> list) {

        txCount = list.stream().mapToInt(m -> m.txCount).sum();
        rxCount = list.stream().mapToInt(m -> m.rxCount).sum();
        errCount = list.stream().mapToInt(m -> m.errCount).sum();
        timeoutCount = list.stream().mapToInt(m -> m.timeoutCount).sum();

        list.stream()
                .map(JcConnMetrics::getMethodExecMap)
                .flatMap(map -> map.entrySet().stream())
                .forEach(entry -> methodExecMap.merge(entry.getKey(), entry.getValue(), MethodExecMetric::addMetric));

        return this;
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
