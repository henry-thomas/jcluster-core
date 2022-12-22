/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.sockets;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import static java.lang.System.currentTimeMillis;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcluster.core.bean.JcHandhsakeFrame;
import org.jcluster.core.bean.JcAppInstanceData;
import org.jcluster.core.cluster.ClusterManager;
import org.jcluster.core.cluster.JcFactory;
import org.jcluster.core.exception.sockets.JcSocketConnectException;
import org.jcluster.core.messages.JcMessage;
import org.jcluster.core.messages.JcMsgResponse;

/**
 *
 * @author henry
 */
public class JcServerEndpoint implements Runnable {

    private static final Logger LOG = Logger.getLogger(JcServerEndpoint.class.getName());

    private final ClusterManager manager = JcFactory.getManager();
    private final Map<String, JcClientConnection> connMap = new HashMap<>();
    private boolean running;
    ServerSocket server;

    @Override
    public void run() {
        try {
            server = new ServerSocket();
            server.setReuseAddress(true);

            InetSocketAddress address = new InetSocketAddress(manager.getAppDescriptor().getIpPort());
            server.bind(address);
            running = true;
            while (running) {

                Socket sock = server.accept();
                try {
                    LOG.info("New Connection Accepted Start Hanshaking");
                    JcHandhsakeFrame handshakeFrame = doHandshake(sock);
                    LOG.log(Level.INFO, "New Connection Hanshaking Complete: {0}", handshakeFrame);

                    JcClientConnection jcClientConnection = new JcClientConnection(sock, handshakeFrame);
                    manager.getThreadFactory().newThread(jcClientConnection).start();

                    LOG.log(Level.INFO, "New ConnectionThread started type: {0}", jcClientConnection.getConnType());
                    if (jcClientConnection.getConnType() == ConnectionType.OUTBOUND) {
                        JcAppInstanceData.getInstance().addOutboundConnection(jcClientConnection);
                    } else {
                        JcAppInstanceData.getInstance().addInboundConnection(jcClientConnection);
                    }
                } catch (JcSocketConnectException e) {
                    Logger.getLogger(JcServerEndpoint.class.getSimpleName()).log(Level.SEVERE, null, e);
                    sock.close();
                }

            }

            server.close();
        } catch (IOException ex) {
            running = false;
//            Logger.getLogger(JcServerEndpoint.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private JcHandhsakeFrame doHandshake(Socket socket) {
        try {
            JcMessage handshakeRequest = new JcMessage("handshake", new Object[]{JcFactory.getManager().getAppDescriptor()});

            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            oos.writeObject(handshakeRequest);
            long startTime = currentTimeMillis();
            while (currentTimeMillis() - startTime < 2000) {
                try {
                    JcMsgResponse handshakeResponse = (JcMsgResponse) ois.readObject();
                    if (handshakeResponse.getData() instanceof JcHandhsakeFrame) {
                        return (JcHandhsakeFrame) handshakeResponse.getData();
                    } else {
                        LOG.warning("Unknown Message Type on Handshake: " + handshakeResponse.getData().getClass().getName());
                    }
                } catch (ClassNotFoundException ex) {

                }
            }

        } catch (IOException ex) {
            throw new JcSocketConnectException(ex.getMessage());
        }
        throw new JcSocketConnectException("Handshake Timeout!");
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
