/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.bean;

import java.io.Serializable;
import org.jcluster.core.JcMember;

/**
 *
 * @author henry
 */
public class JcMemFilterMetric implements Serializable {

    private static final long serialVersionUID = 1198844759084294828L;

    private final long size;
    private final long lastUpdate = -1;
    private final String appName;
    private final String ipAddress;
    private final int udpPort;
    private final int tcpPort;
    private final String memId;
    private final String instanceId;

    public JcMemFilterMetric(JcMember mem, String fName) {
        this.size = mem.filterSetSize(fName);
        this.appName = mem.getDesc().getAppName();
        this.ipAddress = mem.getDesc().getIpAddress();
        this.udpPort = mem.getDesc().getIpPortListenUDP();
        this.tcpPort = mem.getDesc().getIpPortListenTCP();
        this.memId = mem.getId();
        this.instanceId = mem.getDesc().getInstanceId();
    }

    public long getSize() {
        return size;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public String getAppName() {
        return appName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getUdpPort() {
        return udpPort;
    }

    public String getMemId() {
        return memId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public String toString() {
        return "JcMemFilterMetric{" + "size=" + size + ", lastUpdate=" + lastUpdate + ", appName=" + appName + ", ipAddress=" + ipAddress + ", udpPort=" + udpPort + ", tcpPort=" + tcpPort + ", memId=" + memId + ", instanceId=" + instanceId + '}';
    }

}
