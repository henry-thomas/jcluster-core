/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import org.jcluster.core.exception.sockets.JcSocketConnectException;
import org.jcluster.core.messages.JcMessage;
import org.jcluster.core.messages.JcMsgResponse;
import org.jcluster.core.monitor.JcMemberMetrics;
import org.jcluster.core.monitor.JcMetrics;
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
        try {

            server = new ServerSocket();
            server.setReuseAddress(true);

            JcAppDescriptor selfDesc = JcCoreService.getInstance().getSelfDesc();
            Thread.currentThread().setName("JC_TCP_Server@" + selfDesc.getIpPortListenUDP());

            IOException lastEx = null;
            for (Integer port : tcpListenPorts) {
                try {
                    InetSocketAddress address = new InetSocketAddress(selfDesc.getIpAddress(), port);
                    server.bind(address);
                    selfDesc.setIpPortListenTCP(port);
                    LOG.info("TCP Server init successfully on port: {}", port);
                    break;
                } catch (IOException ex) {
                    LOG.warn("TCP Server FAILED to connect on port: {}", port);
                    lastEx = ex;
                }
            }
            if (!server.isBound() && lastEx != null) {
                throw lastEx;
            }

            running = true;
            while (running) {

                Socket sock = server.accept();
                try {
                    JcHandhsakeFrame handshakeFrame = doHandshake(sock);
                    LOG.info("New Connection Hanshaking Complete: {}", handshakeFrame);

                    JcMetrics metrics = JcCoreService.getInstance().getAllMetrics();
                    String memId = handshakeFrame.getRemoteAppDesc().getIpStrPortStr();
                    JcMemberMetrics met = metrics.getMemMetricsMap().get(handshakeFrame.getRemoteAppDesc().getIpStrPortStr());
                    if (met == null) {
                        met = new JcMemberMetrics();
                        metrics.getMemMetricsMap().put(memId, met);
                    }

                    JcClientConnection jcClientConnection = new JcClientConnection(sock, handshakeFrame, met);
                    threadFactory.newThread(jcClientConnection).start();

                    LOG.info("JcInstanceConnection connected.  {}", jcClientConnection);

                    JcAppDescriptor remDesc = handshakeFrame.getRemoteAppDesc();
                    JcMember member = JcCoreService.getInstance().getMember(remDesc.getIpStrPortStr());
                    if (member == null) {
                        LOG.warn("New Connection from invalid member: {}", member);
                        continue;
                    }

                    JcRemoteInstanceConnectionBean ric = member.getConector();
                    ric.addConnection(jcClientConnection);

                } catch (Exception e) {
                    LOG.error(null, e);
                    sock.close();
                }

            }

            server.close();
        } catch (IOException ex) {
            running = false;
            LOG.warn(null, ex);
        } finally {
            try {
                server.close();
            } catch (IOException ex) {
                LOG.warn(null, ex);
            }
        }
    }

    private JcHandhsakeFrame doHandshake(Socket socket) {

        FutureTask<JcHandhsakeFrame> futureHanshake = new FutureTask<>(() -> {
            try {
                LOG.info("New Connection Accepted Start Hanshaking");
                JcMessage handshakeRequest = new JcMessage("handshake", new Object[]{JcCoreService.getInstance().getSelfDesc()});

                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

                oos.writeObject(handshakeRequest);
                JcMsgResponse handshakeResponse = (JcMsgResponse) ois.readObject();
                if (handshakeResponse.getData() instanceof JcHandhsakeFrame) {
                    return (JcHandhsakeFrame) handshakeResponse.getData();
                } else {
                    LOG.warn("Unknown Message Type on Handshake: {}", handshakeResponse.getData().getClass().getName());
                }

            } catch (IOException | ClassNotFoundException ex) {
                LOG.error(null, ex);
            }
            return null;

        });
        JcCoreService.getInstance().getExecutorService().execute(futureHanshake);
        try {
            JcHandhsakeFrame hf = futureHanshake.get(5, TimeUnit.SECONDS);
            if (hf != null) {
                return hf;
            }
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            LOG.error(null, ex);
        }

        throw new JcSocketConnectException("Handshake Timeout!");
    }

    public void destroy() {
        try {
            running = false;
            server.close();
            LOG.trace("JcServerEndpoint destroyed");
        } catch (IOException ex) {
            LOG.error(null, ex);
        }
    }

    public boolean isRunning() {
        return running;
    }

}
