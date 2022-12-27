/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import static java.lang.System.currentTimeMillis;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.concurrent.ManagedThreadFactory;
import org.jcluster.core.bean.JcHandhsakeFrame;
import org.jcluster.core.exception.sockets.JcSocketConnectException;
import org.jcluster.core.messages.JcMessage;
import org.jcluster.core.messages.JcMsgResponse;

/**
 *
 * @autor Henry Thomas
 */
public class JcServerEndpoint implements Runnable {

    private static final Logger LOG = Logger.getLogger(JcServerEndpoint.class.getName());

    private final JcManager manager = JcFactory.getManager();
    private boolean running;
    ServerSocket server;

    private final ManagedThreadFactory threadFactory;

    public JcServerEndpoint(ManagedThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket();
            server.setReuseAddress(true);

            InetSocketAddress address = new InetSocketAddress(manager.getInstanceAppDesc().getIpPort());
            server.bind(address);
            running = true;
            while (running) {

                Socket sock = server.accept();
                try {
                    LOG.info("New Connection Accepted Start Hanshaking");
                    JcHandhsakeFrame handshakeFrame = doHandshake(sock);
                    LOG.log(Level.INFO, "New Connection Hanshaking Complete: {0}", handshakeFrame);

                    JcClientConnection jcClientConnection = new JcClientConnection(sock, handshakeFrame);
                    threadFactory.newThread(jcClientConnection).start();

                    LOG.log(Level.INFO, "JcInstanceConnection connected.  {0}", jcClientConnection);

                    JcRemoteInstanceConnectionBean ric = manager.getRemoteInstance(handshakeFrame.getRemoteAppDesc().getInstanceId());
                    //In case where this instance does not have calls to the socket.accept() calling instance,
                    //there will be no Remote instance conneciton sincse they are filtered by required app name. 
                    //In this case we have to create one,
                    //as long as that instance is available in HZ it will not be destroyed
                    if (ric == null) {
                        manager.addRemoteInstanceConnection(handshakeFrame.getRemoteAppDesc());
                    }
                    ric.addConnection(jcClientConnection);

                } catch (Exception e) {
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

        FutureTask<JcHandhsakeFrame> futureHanshake = new FutureTask<>(() -> {
            try {
                JcMessage handshakeRequest = new JcMessage("handshake", new Object[]{JcFactory.getManager().getInstanceAppDesc()});

                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

                oos.writeObject(handshakeRequest);
                JcMsgResponse handshakeResponse = (JcMsgResponse) ois.readObject();
                if (handshakeResponse.getData() instanceof JcHandhsakeFrame) {
                    return (JcHandhsakeFrame) handshakeResponse.getData();
                } else {
                    LOG.log(Level.WARNING, "Unknown Message Type on Handshake: {0}", handshakeResponse.getData().getClass().getName());
                }

            } catch (IOException | ClassNotFoundException ex) {
            }
            return null;

        });
        JcManager.getInstance().getExecutorService().execute(futureHanshake);
        try {
            JcHandhsakeFrame hf = futureHanshake.get(5, TimeUnit.SECONDS);
            if (hf != null) {
                return hf;
            }
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            Logger.getLogger(JcServerEndpoint.class.getName()).log(Level.SEVERE, null, ex);
        }

        throw new JcSocketConnectException("Handshake Timeout!");
    }

    public void destroy() {
        try {
            running = false;
            server.close();
        } catch (IOException ex) {
            Logger.getLogger(JcServerEndpoint.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
