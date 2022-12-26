/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.bean;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
    private final String instanceId;
    private final String ipAddress;
    private final int ipPort;
    //use this to detect dead instances somehow left unclean in the map
    private long lastAlive;
    private final Map<String, HashSet<Object>> filterMap = new HashMap<>();

    public JcAppDescriptor() {
        this.ipPort = JcAppConfig.getINSTANCE().getPort();
        this.ipAddress = JcAppConfig.getINSTANCE().getHostName();
        this.appName = JcAppConfig.getINSTANCE().getAppName();

        this.instanceId = RandomStringUtils.random(16, true, true);
        this.isolated = JcAppConfig.getINSTANCE().isIsolated();
    }

    public void updateTimestamp() {
        lastAlive = System.currentTimeMillis();
    }

    public long getLastAlive() {
        return lastAlive;
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

    public int getIpPort() {
        return ipPort;
    }

    public Map<String, HashSet<Object>> getFilterMap() {
        return filterMap;
    }

    public String getAppName() {
        return appName;
    }

    @Override
    public String toString() {
        return "JcAppDescriptor{" + "appName=" + appName + ", instanceId=" + instanceId + ", address=" + ipAddress + ":" + ipPort + '}';
    }

}
