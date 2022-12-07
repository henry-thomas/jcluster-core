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

/**
 *
 * @author henry 
 * 
 * Created by JcManager for each instance
 * 
 */
public class JcAppDescriptor implements Serializable {
    //add whatever we need to represent our instances

    private String appName = "APP_INSTANCE";
    private String instanceId;
    private String ipAddress;
    private int ipPort;
    private final Map<String, HashSet<Object>> filterMap = new HashMap<>();

    public JcAppDescriptor() {
        this.instanceId = RandomStringUtils.random(16, true, true);
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getIpPort() {
        return ipPort;
    }

    public void setIpPort(int ipPort) {
        this.ipPort = ipPort;
    }

    public Map<String, HashSet<Object>> getFilterMap() {
        return filterMap;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

}
