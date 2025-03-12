/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
//    private final boolean monitor;
    private final String appName;
    private final Set<String> topicList = new HashSet<>();
//    private final List<String> outboundAppNameList = new ArrayList<>();
    private final String instanceId;
//    private final String serverName;
    private String ipAddress;
    private int ipPort;
    private int outBoundMinConnection;
    //use this to detect dead instances somehow left unclean in the map
//    private long lastAlive;
//    private final Map<String, HashSet<Object>> filterMap = new HashMap<>();

    public JcAppDescriptor() {
        this.ipPort = JcAppConfig.getINSTANCE().getPort();
        this.ipAddress = JcAppConfig.getINSTANCE().getHostName();

        this.appName = JcAppConfig.getINSTANCE().getAppName();

        this.instanceId = RandomStringUtils.random(16, true, true);
//        this.serverName = JcAppConfig.getINSTANCE().readProp("JC_SERVER_NAME", instanceId);
        this.isolated = JcAppConfig.getINSTANCE().isIsolated();
//        this.topicList.addAll(JcAppConfig.getINSTANCE().getTopicList());

    }

    public int getOutBoundMinConnection() {
        return outBoundMinConnection;
    }

    public void setOutBoundMinConnection(int outBoundMinConnection) {
        this.outBoundMinConnection = outBoundMinConnection;
    }

//    public List<String> getOutboundAppNameList() {
//        return outboundAppNameList;
//    }
//    public String getServerName() {
//        return serverName;
//    }
//    public void updateTimestamp() {
//        lastAlive = System.currentTimeMillis();
//    }
//
//    public long getLastAlive() {
//        return lastAlive;
//    }
    public boolean isIsolated() {
        return isolated;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getIpPort() {
        return ipPort;
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
        hash = 47 * hash + this.ipPort;
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
        if (this.ipPort != other.ipPort) {
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

    public void setIpPort(int ipPort) {
        this.ipPort = ipPort;
    }

    @Override
    public String toString() {
        return "JcAppDescriptor{" + "appName=" + appName + ", instanceId=" + instanceId + ", address=" + ipAddress + ":" + ipPort + '}';
    }

    public String getIpStrPortStr() {
        return ipAddress + ":" + ipPort;
    }

}
