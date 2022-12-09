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
public class ConnectionParam implements Serializable{

    private final String connId;
    private final boolean accepted;

    public ConnectionParam(String connId, boolean accepted) {
        this.connId = connId;
        this.accepted = accepted;
    }

    public String getConnId() {
        return connId;
    }

    public boolean isAccepted() {
        return accepted;
    }

}
