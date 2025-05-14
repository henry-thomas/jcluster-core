/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import org.jcluster.core.bean.JcAppDescriptor;
import org.jcluster.core.bean.JcHandhsakeFrame;
import org.jcluster.core.bean.SerializedConnectionBean;
import org.slf4j.LoggerFactory;

/**
 *
 * @autor Henry Thomas
 */
public class JcServerEndpoint implements Runnable {

    private static final ch.qos.logback.classic.Logger LOG = (Logger) LoggerFactory.getLogger(JcServerEndpoint.class);

    private boolean running;
    ServerSocket server;

    private final ThreadFactory threadFactory;
    private final List<Integer> tcpListenPorts;

    public JcServerEndpoint(ThreadFactory threadFactory, List<Integer> tcpListenPorts) {
        this.threadFactory = threadFactory;
        this.tcpListenPorts = tcpListenPorts;
//        LOG.setLevel(Level.ALL);
    }

    @Override
    public void run() {
        JcAppDescriptor selfDesc = JcCoreService.getInstance().getSelfDesc();
        try {

            server = new ServerSocket();
            server.setReuseAddress(true);

            selfDesc.setIpPort(0);
            IOException lastEx = null;

            for (Integer port : tcpListenPorts) {
                try {
                    InetSocketAddress address = new InetSocketAddress(port);
                    server.setReuseAddress(true);
                    server.bind(address);
                    selfDesc.setIpPort(port);
                    LOG.info("TCP Server init successfully on port: {}", port);
                    break;
                } catch (IOException ex) {
                    LOG.warn("TCP Server FAILED to connect on port: {}", port);
                    lastEx = ex;
                }
            }

            Thread.currentThread().setName("JC_TCP_Server@" + selfDesc.getIpPort());
            if (!server.isBound() && lastEx != null) {
                throw lastEx;
            }

            selfDesc.setIsolated(false);

            running = true;
            while (running) {
                Socket client = server.accept();
                client.setKeepAlive(true);
                client.setTcpNoDelay(true);
                //this should not block this thread, should start on its own thread for handshaking
                onIncomingConnection(client);
            }

            server.close();
        } catch (IOException ex) {
            if (running) {//if this is not runing is shutdown, no need to print IOException
                LOG.warn(null, ex);
                running = false;
            }
        } finally {
            try {
                selfDesc.setIpPort(0);
                selfDesc.setIsolated(true);
                LOG.warn("Closing TCP JcServerEndpoint");
                server.close();
            } catch (IOException ex) {
                LOG.warn(null, ex);
            }
        }
    }

    private void onIncomingConnection(Socket cl) {
        Thread t = threadFactory.newThread(() -> {
            try {
                SerializedConnectionBean scb = new SerializedConnectionBean(cl);

                JcHandhsakeFrame handShakeReq = JcClientConnection.getHandshakeFromSocket(3, scb.getOis());

                if (null == handShakeReq.getRequestedConnType()) {
                    LOG.warn("handShakeReq with invalid connTyp=null");
                } else {
                    switch (handShakeReq.getRequestedConnType()) {
                        case MANAGED:   // IO Managed
                            JcClientManagedConnection.createFormIncomingConnection(scb, handShakeReq);
                            break;
                        case INBOUND:// IO Connection
                        case OUTBOUND:// IO Connection
                            JcClientIOConnection.createFromIncomingConnection(scb, handShakeReq);
                            break;
                        default:
                            LOG.warn("handShakeReq with invalid connTyp=" + handShakeReq.getRequestedConnType());
                    }
                }

            } catch (Exception ex) {
                LOG.warn(null, ex);
                try {
                    cl.close();
                } catch (IOException ex1) {
                    java.util.logging.Logger.getLogger(JcServerEndpoint.class.getName()).log(java.util.logging.Level.SEVERE, null, ex1);
                }
            }

        });
        t.setName("JC-ServerConProc");

        t.start();
    }

    public void destroy() {
        try {
            running = false;
            server.close();
            LOG.warn("JcServerEndpoint destroyed");
        } catch (IOException ex) {
            LOG.error(null, ex);
        }
    }

    public boolean isRunning() {
        return running;
    }

}
