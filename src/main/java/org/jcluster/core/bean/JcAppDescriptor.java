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

/**
 *
 * @autor Henry Thomas
 *
 * Created by JcManager for each instance
 *
 */
public class JcAppDescriptor implements Serializable {
    //add whatever we need to represent our instances

    private String appName = "unknown";
    private final Set<String> topicList = new HashSet<>();
    private final String instanceId;
    private String ipAddress;
    private int ipPort;
    boolean isolated = false;
    private byte[] publicKey;
//    private int ipPortListenUDP;//this is list

    public JcAppDescriptor() {
        this.instanceId = RandomStringUtils.random(16, true, true);
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

//    public int getIpPortListenUDP() {
//        return ipPortListenUDP;
//    }
    public int getIpPort() {
        return ipPort;
    }

    public void setIpPort(int ipPort) {
        this.ipPort = ipPort;
    }

    public String getAppName() {
        return appName;
    }

    public Set<String> getTopicList() {
        return topicList;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Objects.hashCode(this.instanceId);
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
        return Objects.equals(this.instanceId, other.instanceId);
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public String toString() {
        return "JcAppDescriptor{" + "appName=" + appName + ", topicList=" + topicList + ", instanceId=" + instanceId + ", ipAddress=" + ipAddress + ", ipPortListenTCP=" + ipPort + '}';
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

}
