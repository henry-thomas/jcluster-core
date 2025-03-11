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
import org.jcluster.core.config.JcAppConfig;
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

    public JcMember(JcAppDescriptor desc) {
        this.desc = desc;
    }

    public static void sendMessage(int port, String ip, JcDistMsg msg) throws IOException {
        if (msg.hasTTLExpire()) {
            return;
        }
        DatagramSocket socket = new DatagramSocket();
        socket.setReuseAddress(true);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = new ObjectOutputStream(bos);
        out.writeObject(msg);
        byte[] data = bos.toByteArray();

        DatagramPacket p = new DatagramPacket(data, data.length, InetAddress.getByName(ip), port);
        socket.send(p);
        socket.close();
    }

    public void sendMessage(JcDistMsg msg) throws IOException {
        if (msg.hasTTLExpire()) {
            return;
        }
        if (socket == null) {
            socket = new DatagramSocket();
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = new ObjectOutputStream(bos);
        out.writeObject(msg);
        byte[] data = bos.toByteArray();

        DatagramPacket p = new DatagramPacket(data, data.length, InetAddress.getByName(desc.getIpAddress()), desc.getIpPort());
        socket.send(p);
    }

    public JcAppDescriptor getDesc() {
        return desc;
    }

    public void setDesc(JcAppDescriptor desc) {
        this.desc = desc;
    }

    public boolean isLastSeenExpired() {

        return System.currentTimeMillis() - lastSeen > 30_000;
    }

    public void updateLastSeen() {
        lastSeen = System.currentTimeMillis();
    }

}
