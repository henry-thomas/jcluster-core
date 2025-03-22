/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.monitor;

import java.io.Serializable;

/**
 *
 * @author henry
 */
public class MethodExecMetric implements Serializable {

    private static final long serialVersionUID = 8436518005629785705L;

    private int execCount;
    private long totalExecTime;
    private long lastExecTime;

    public MethodExecMetric() {
    }

    public MethodExecMetric addMetric(MethodExecMetric met) {
        this.execCount += met.execCount;
        this.totalExecTime += met.totalExecTime;
        return this;
    }

    public int getExecCount() {
        return execCount;
    }

    public void setExecCount(int execCount) {
        this.execCount = execCount;
    }

    public void setLastExecTime(long lastExecDuration) {
        this.lastExecTime = lastExecDuration;
        this.totalExecTime += lastExecDuration;
        this.execCount += 1;
    }

    public long getTotalExecTime() {
        return totalExecTime;
    }

}
