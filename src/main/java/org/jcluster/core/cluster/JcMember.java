/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.cluster;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import org.jcluster.core.bean.JcAppDescriptor;
import org.jcluster.core.messages.JcDistMsg;
import ch.qos.logback.classic.Logger;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.LoggerFactory;

/**
 *
 * @author henry
 */
public class JcMember {

    private static final Logger LOG = (Logger) LoggerFactory.getLogger(JcMember.class);

    private JcAppDescriptor desc;
    private DatagramSocket socket;
    private long lastSeen;
    private String id;
    private final Set<String> subscribtionSet = new HashSet<>();

    public JcMember(JcAppDescriptor desc) {
        this.desc = desc;
        if (desc != null) {
            id = desc.getIpStrPortStr();
        }
    }

    public void close() {
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception e) {
            }
        }
    }

    private static void sendMessage(JcDistMsg msg, DatagramSocket socket, String ip, int port) throws IOException {
        if (msg.hasTTLExpire()) {
            return;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = new ObjectOutputStream(bos);
        out.writeObject(msg);
        byte[] data = bos.toByteArray();

        DatagramPacket p = new DatagramPacket(data, data.length, InetAddress.getByName(ip), port);
        socket.send(p);
    }

    public static void sendMessage(int port, String ip, JcDistMsg msg) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            sendMessage(msg, socket, ip, port);
        }
    }

    public void sendMessage(JcDistMsg msg) throws IOException {
        if (socket == null) {
            socket = new DatagramSocket();
        }
        sendMessage(msg, socket, desc.getIpAddress(), desc.getIpPort());

    }

    public JcAppDescriptor getDesc() {
        return desc;
    }

    public void setDesc(JcAppDescriptor desc) {
        this.desc = desc;
        if (desc != null) {
            id = desc.getIpStrPortStr();
        }
    }

    public boolean isLastSeenExpired() {

        return System.currentTimeMillis() - lastSeen > 30_000;
    }

    public void updateLastSeen() {
        lastSeen = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public Set<String> getSubscribtionSet() {
        return subscribtionSet;
    }

}
