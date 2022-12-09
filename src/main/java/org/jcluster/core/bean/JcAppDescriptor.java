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

    private final String appName;
    private final String instanceId;
    private final String ipAddress;
    private final int ipPort;
    private final Map<String, HashSet<Object>> filterMap = new HashMap<>();

    public JcAppDescriptor(String ipAddress, int ipPort, String appName) {
        this.instanceId = RandomStringUtils.random(16, true, true);
        this.ipAddress = ipAddress;
        this.ipPort = ipPort;
        this.appName = appName;
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

}
