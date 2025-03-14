/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jcluster.core.bean.RemMembFilter;
import org.jcluster.core.messages.PublishMsg;
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

    private final JcRemoteInstanceConnectionBean conector = new JcRemoteInstanceConnectionBean();

    //this is for verification and keep track if we are subscribe or not to a filter 
    //at this specific member
    //value is filterName
    private final Set<String> subscribtionSet = new HashSet<>();

    private final Map<String, RemMembFilter> filterMap = new HashMap<>();

    public JcMember(JcAppDescriptor desc) {
        this.desc = desc;
        if (desc != null) {
            conector.setDesc(desc);
            id = desc.getIpStrPortStr();
        }
    }

    public JcRemoteInstanceConnectionBean getConector() {
        return conector;
    }

    public RemMembFilter getOrCreateFilterTarget(String filterName) {
        if (filterName == null) {
            throw new RuntimeException("Invalid filter name NULL");
        }
        RemMembFilter remFilter = filterMap.get(filterName);
        if (remFilter == null) {
            remFilter = new RemMembFilter();
            filterMap.put(filterName, remFilter);
        }
        return remFilter;
    }

    protected void onFilterPublishMsg(JcDistMsg msg) {
        if (msg == null || !(msg.getData() instanceof PublishMsg)) {
            LOG.warn("Receive invalid Publish filter : {}", msg);
            return;
        }
        PublishMsg pm = (PublishMsg) msg.getData();

        String filterName = pm.getFilterName();
        if (filterName == null) {
            LOG.warn("Receive invalid Publish filter  filterName is NULL");
            return;
        }

        RemMembFilter rmf = getOrCreateFilterTarget(filterName);
        rmf.onFilterPublishMsg(pm);
    }

    protected void onSubscResponseMsg(JcDistMsg msg) {
        if (msg == null || !(msg.getData() instanceof PublishMsg)) {
            LOG.warn("Receive invalid Subscription response: {}", msg);
            return;
        }
        PublishMsg pm = (PublishMsg) msg.getData();
        String filterName = pm.getFilterName();
        if (filterName == null) {
            LOG.warn("Receive invalid Subscription response filterName is NULL");
            return;
        }

        //mark that this is already subscribed to
        getSubscribtionSet().add(filterName);

        RemMembFilter rmf = getOrCreateFilterTarget(filterName);
        rmf.onFilterPublishMsg(pm);

        rmf.onFilterPublishMsg(pm);

    }

    public void close() {
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception e) {
            }
        }
        conector.destroy();
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
        sendMessage(msg, socket, desc.getIpAddress(), desc.getIpPortListenUDP());

    }

    public JcAppDescriptor getDesc() {
        return desc;
    }

    public void setDesc(JcAppDescriptor desc) {
        this.desc = desc;
        if (desc != null) {
            conector.setDesc(desc);
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

    public boolean containsFilter(Map<String, Object> fMap) {
        for (Map.Entry<String, Object> entry : fMap.entrySet()) {
            String fName = entry.getKey();
            Object fValue = entry.getValue();

            RemMembFilter rf = this.filterMap.get(fName);
            if (rf == null) {
                return false;
            }
            if (!rf.containsFilterValue(fValue)) {
                return false;
            }
        }

        return true;
    }

    public long getLastSeen() {
        return lastSeen;
    }

}
