/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

import ch.qos.logback.classic.Logger;
import javax.enterprise.concurrent.ManagedThreadFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
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
import org.slf4j.LoggerFactory;

/**
 *
 * @autor Henry Thomas
 */
public class JcServerEndpoint implements Runnable {

    private static final ch.qos.logback.classic.Logger LOG = (Logger) LoggerFactory.getLogger(JcCoreService.class);

    private boolean running;
    ServerSocket server;

    private final ThreadFactory threadFactory;

    public JcServerEndpoint(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket();
            server.setReuseAddress(true);

            InetSocketAddress address = new InetSocketAddress(JcCoreService.getInstance().getSelfDesc().getIpPortListenUDP());
            server.bind(address);
            running = true;
            while (running) {

                Socket sock = server.accept();
                try {
                    LOG.info("New Connection Accepted Start Hanshaking");
                    JcHandhsakeFrame handshakeFrame = doHandshake(sock);
                    LOG.info("New Connection Hanshaking Complete: {}", handshakeFrame);

                    JcClientConnection jcClientConnection = new JcClientConnection(sock, handshakeFrame);
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
//            Logger.getLogger(JcServerEndpoint.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private JcHandhsakeFrame doHandshake(Socket socket) {

        FutureTask<JcHandhsakeFrame> futureHanshake = new FutureTask<>(() -> {
            try {
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
//                LOG.log(Level.SEVERE, "Could not handshake", ex);
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
        } catch (IOException ex) {
            LOG.error(null, ex);
        }
    }

    public boolean isRunning() {
        return running;
    }

}
