/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypower24.jcclustertest;

/**
 *
 * @author henry
 */
public class JcConfig {

    private String primaryMemberIp;
    private int primaryPort;

    private String appName;
    private String ip;
    private int port;
    private boolean isolated;
    private boolean primary;

    public String getPrimaryMemberIp() {
        return primaryMemberIp;
    }

    public void setPrimaryMemberIp(String primaryMemberIp) {
        this.primaryMemberIp = primaryMemberIp;
    }

    public int getPrimaryPort() {
        return primaryPort;
    }

    public void setPrimaryPort(int primaryPort) {
        this.primaryPort = primaryPort;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isIsolated() {
        return isolated;
    }

    public void setIsolated(boolean isolated) {
        this.isolated = isolated;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

}
