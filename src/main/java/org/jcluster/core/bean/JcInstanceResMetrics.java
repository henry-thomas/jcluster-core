/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.bean;

import java.io.Serializable;

/**
 *
 * @author henry
 */
public class JcInstanceResMetrics implements Serializable{

    private Double cpuUsage = 0d;
    private Double peakCpuUsage = 0d;
    private long memUsage = 0l;
    private long netRxTraffic = 0l;
    private long netTxTraffic = 0l;
    private long peakNetRxTraffic = 0l;
    private long peakNetTxTraffic = 0l;

    public Double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(Double cpuUsage) {
        if (cpuUsage > peakCpuUsage) {
            peakCpuUsage = cpuUsage;
        }
        this.cpuUsage = cpuUsage;
    }

    public Double getPeakCpuUsage() {
        return peakCpuUsage;
    }

    public void setPeakCpuUsage(Double peakCpuUsage) {
        this.peakCpuUsage = peakCpuUsage;
    }

    public long getMemUsage() {
        return memUsage;
    }

    public void setMemUsage(long memUsage) {
        this.memUsage = memUsage;
    }

    public long getNetRxTraffic() {
        return netRxTraffic;
    }

    public void setNetRxTraffic(long netRxTraffic) {
        if (netRxTraffic > peakNetRxTraffic) {
            peakNetRxTraffic = netRxTraffic;
        }
        this.netRxTraffic = netRxTraffic;
    }

    public long getPeakNetRxTraffic() {
        return peakNetRxTraffic;
    }

    public void setPeakNetRxTraffic(long peakNetRxTraffic) {
        this.peakNetRxTraffic = peakNetRxTraffic;
    }

    public long getPeakNetTxTraffic() {
        return peakNetTxTraffic;
    }

    public void setPeakNetTxTraffic(long peakNetTxTraffic) {
        this.peakNetTxTraffic = peakNetTxTraffic;
    }

    public long getNetTxTraffic() {
        return netTxTraffic;
    }

    public void setNetTxTraffic(long netTxTraffic) {
        if (netTxTraffic > peakNetTxTraffic) {
            peakNetTxTraffic = netTxTraffic;
        }

        this.netTxTraffic = netTxTraffic;
    }

}
