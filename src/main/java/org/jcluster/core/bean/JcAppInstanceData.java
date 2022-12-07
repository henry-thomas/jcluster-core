/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.bean;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.jcluster.core.sockets.JcClientConnection;

/**
 *
 * @author henry
 */
public class JcAppInstanceData implements Serializable {

    private final String serialVersionUID = "-1455291844074901991";

    private JcAppDescriptor desc;
    private final Map<String, JcClientConnection> ouboundConnections = new HashMap<>();
    private final Map<String, JcClientConnection> inboundConnections = new HashMap<>();
    private static JcAppInstanceData INSTANCE;

    public static JcAppInstanceData getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JcAppInstanceData();
        }
        return INSTANCE;
    }

    private JcAppInstanceData() {
//        this.desc = desc;
    }

    public void addOutboundConnection(JcClientConnection conn) {

        ouboundConnections.put(conn.getConnId(), conn);
    }

    public void addInboundConnection(JcClientConnection conn) {

        inboundConnections.put(conn.getConnId(), conn);
    }

    public JcAppDescriptor getDesc() {
        return desc;
    }

    public Map<String, JcClientConnection> getOuboundConnections() {
        return ouboundConnections;
    }

    public Map<String, JcClientConnection> getInboundConnections() {
        return inboundConnections;
    }

}
