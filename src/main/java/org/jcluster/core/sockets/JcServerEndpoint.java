/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.sockets;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcluster.core.bean.JcAppInstanceData;
import org.jcluster.core.cluster.ClusterManager;
import org.jcluster.core.cluster.JcFactory;

/**
 *
 * @author henry
 */
public class JcServerEndpoint implements Runnable {

    private final ClusterManager manager = JcFactory.getManager();
    private final Map<String, JcClientConnection> connMap = new HashMap<>();
    private boolean running;
    ServerSocket server;

    @Override
    public void run() {
        try {
            server = new ServerSocket();
            server.setReuseAddress(true);

            InetSocketAddress address = new InetSocketAddress(manager.getThisDescriptor().getIpPort());
            server.bind(address);
            running = true;
            while (running) {

                Socket sock = server.accept();
                String connId = JcFactory.getManager().getThisDescriptor().getAppName() + "-" + sock.getInetAddress().getHostAddress() + ":" + sock.getLocalPort() + "-INBOUND";

                JcClientConnection conn = connMap.get(connId);
                //remove old connections, recreate them.
                if (conn != null) {
                    conn.destroy();
                    connMap.remove(connId);
                }

                JcClientConnection jcClientConnection = new JcClientConnection(sock);

                connMap.put(connId, jcClientConnection);

                JcAppInstanceData.getInstance().addInboundConnection(jcClientConnection);

                Thread cThread = new Thread(jcClientConnection);
                cThread.start();
            }

            server.close();
        } catch (IOException ex) {
            running = false;
//            Logger.getLogger(JcServerEndpoint.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void destroy() {
        try {
            for (Map.Entry<String, JcClientConnection> entry : connMap.entrySet()) {
                JcClientConnection conn = entry.getValue();
                conn.destroy();
            }
            JcAppInstanceData.getInstance().getInboundConnections().clear();
            connMap.clear();
            running = false;
            server.close();
        } catch (IOException ex) {
            Logger.getLogger(JcServerEndpoint.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
