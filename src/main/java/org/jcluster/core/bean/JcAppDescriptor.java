/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.bean;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.jcluster.core.config.JcAppConfig;

/**
 *
 * @autor Henry Thomas
 *
 * Created by JcManager for each instance
 *
 */
public class JcAppDescriptor implements Serializable {
    //add whatever we need to represent our instances

    private final boolean isolated;
    private final String appName;
    private final Set<String> topicList = new HashSet<>();
    private final String instanceId;
    private String ipAddress;
    private int ipPortListenUDP;//this is list
    
    private int ipPortListenTCP;//this is list


    public JcAppDescriptor() {
        this.ipPortListenUDP = JcAppConfig.getINSTANCE().getPort();
        this.ipAddress = JcAppConfig.getINSTANCE().getHostName();

        this.appName = JcAppConfig.getINSTANCE().getAppName();

        this.instanceId = RandomStringUtils.random(16, true, true);
        this.isolated = JcAppConfig.getINSTANCE().isIsolated();
    }


    public boolean isIsolated() {
        return isolated;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getIpPortListenUDP() {
        return ipPortListenUDP;
    }

    public String getAppName() {
        return appName;
    }

    public Set<String> getTopicList() {
        return topicList;
    }

//    public boolean isMonitor() {
//        return monitor;
//    }
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + Objects.hashCode(this.instanceId);
        hash = 47 * hash + Objects.hashCode(this.ipAddress);
        hash = 47 * hash + this.ipPortListenUDP;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final JcAppDescriptor other = (JcAppDescriptor) obj;
        if (this.ipPortListenUDP != other.ipPortListenUDP) {
            return false;
        }
        if (!Objects.equals(this.instanceId, other.instanceId)) {
            return false;
        }

        return Objects.equals(this.ipAddress, other.ipAddress);
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setIpPortListenUDP(int ipPortListenUDP) {
        this.ipPortListenUDP = ipPortListenUDP;
    }

    @Override
    public String toString() {
        return "JcAppDescriptor{" + "appName=" + appName + ", instanceId=" + instanceId + ", address=" + ipAddress + ":" + ipPortListenUDP + '}';
    }

    public String getIpStrPortStr() {
        return ipAddress + ":" + ipPortListenUDP;
    }

    public int getIpPortListenTCP() {
        return ipPortListenTCP;
    }

    public void setIpPortListenTCP(int ipPortListenTCP) {
        this.ipPortListenTCP = ipPortListenTCP;
    }

}
