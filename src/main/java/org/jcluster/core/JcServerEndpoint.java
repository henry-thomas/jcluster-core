/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jcluster.core.bean.JcAppDescriptor;
import org.jcluster.core.bean.JcHandhsakeFrame;
import org.jcluster.core.bean.SerializedConnectionBean;
import org.jcluster.core.messages.JcMsgResponse;
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
        LOG.setLevel(Level.ALL);
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
                LOG.warn("Closing TCP JcServerEndpoint");
                server.close();
            } catch (IOException ex) {
                LOG.warn(null, ex);
            }
        }
    }

//    protected final JcHandhsakeFrame getHandshakeFromSocket(int timeoutSec, ObjectInputStream ois) throws ExecutionException, TimeoutException, InterruptedException {
//        FutureTask<JcHandhsakeFrame> futureHanshake = new FutureTask<>(() -> {
//            try {
//                JcMsgResponse handshakeResponse = (JcMsgResponse) ois.readObject();
//
//                if (handshakeResponse.getData() instanceof JcHandhsakeFrame) {
//                    return (JcHandhsakeFrame) handshakeResponse.getData();
//                } else {
//                    LOG.warn("Unknown Message Type on Handshake: {}", handshakeResponse.getData().getClass().getName());
//                }
//
//            } catch (IOException | ClassNotFoundException ex) {
//                LOG.error(null, ex);
//                throw ex;
//            }
//            return null;
//        });
//
//        JcCoreService.getInstance().getExecutorService().execute(futureHanshake);
//        JcHandhsakeFrame hf = futureHanshake.get(timeoutSec, TimeUnit.SECONDS);
//        if (hf != null) {
//            return hf;
//        }
//        return null;
//    }
    private void onIncomingConnection(Socket cl) {
        Thread t = threadFactory.newThread(() -> {
            try {
                SerializedConnectionBean scb = new SerializedConnectionBean(cl);

                JcHandhsakeFrame handShakeReq = JcClientConnection.getHandshakeFromSocket(3, scb.getOis());

                if (handShakeReq.getRequestedConnType() == JcConnectionTypeEnum.MANAGED) {
                    JcClientManagedConnection.createFormIncomingConnection(scb, handShakeReq, JcCoreService.getInstance()::onNewManagedConnection);
                } else {
                    JcClientIOConnection.createFormIncomingConnection(scb, handShakeReq);
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
