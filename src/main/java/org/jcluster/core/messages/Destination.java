/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.messages;

import java.io.Serializable;

/**
 *
 * @author henry
 */
public class Destination implements Serializable{

    private String serverId;
    private String serverAddress;
    private String mdbPort;

    public Destination(String serverId, String serverAddress, String mdbPort) {
        this.serverId = serverId;
        this.serverAddress = serverAddress;
        this.mdbPort = mdbPort;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public String getMdbPort() {
        return mdbPort;
    }

    public void setMdbPort(String mdbPort) {
        this.mdbPort = mdbPort;
    }

}
