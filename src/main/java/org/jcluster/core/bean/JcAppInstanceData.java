/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jcluster.core.sockets.JcClientConnection;

/**
 *
 * @author henry
 */
public class JcAppInstanceData implements Serializable {

    private final String serialVersionUID = "-1455291844074901991";

    private JcAppDescriptor desc;
    private final Map<String, JcClientConnection> ouboundConnections = new ConcurrentHashMap<>();
    private final Map<String, JcClientConnection> inboundConnections = new ConcurrentHashMap<>();
    private static JcAppInstanceData INSTANCE;
    private int totalReconnects = 0;

    public static JcAppInstanceData getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JcAppInstanceData();
        }
        return INSTANCE;
    }

    private JcAppInstanceData() {
//        this.desc = desc;
    }

    public List<JcConnectionMetrics> getInboundMetrics() {
        List<JcConnectionMetrics> metrics = new ArrayList<>();
        for (Map.Entry<String, JcClientConnection> entry : inboundConnections.entrySet()) {
            JcClientConnection conn = entry.getValue();

            metrics.add(conn.getMetrics());
        }
        return metrics;
    }

    public List<JcConnectionMetrics> getOutboundMetrics() {
        List<JcConnectionMetrics> metrics = new ArrayList<>();
        for (Map.Entry<String, JcClientConnection> entry : ouboundConnections.entrySet()) {
            JcClientConnection conn = entry.getValue();

            metrics.add(conn.getMetrics());
        }
        return metrics;
    }

    public List<JcConnectionMetrics> getAllMetrics() {
        List<JcConnectionMetrics> metrics = getInboundMetrics();
        metrics.addAll(getOutboundMetrics());
        return metrics;
    }

    public int getTotalOutboundConnections() {
        return ouboundConnections.size();
    }

    public int getTotalInboundConnections() {
        return inboundConnections.size();
    }

    public void incrReconnectCount() {
        ++totalReconnects;
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

    public int getTotalReconnects() {
        return totalReconnects;
    }

}
