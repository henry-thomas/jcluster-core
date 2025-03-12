/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.cluster;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.concurrent.ManagedThreadFactory;
import org.jcluster.core.bean.JcAppDescriptor;
import org.jcluster.core.messages.JcDistMsg;
import org.jcluster.core.messages.JcDistMsgType;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @autor Henry Thomas
 */
public final class JcCoreService {

    private static final Logger LOG = (Logger) LoggerFactory.getLogger(JcCoreService.class);

    public static final int UDP_LISTEN_PORT_DEFAULT = 4445;

//    private String appId;
    private final Map<String, JcMember> memberMap = new ConcurrentHashMap<>();
    private final Map<String, JcMember> primaryMemberMap = new HashMap<>();
    private final JcAppDescriptor selfDesc = new JcAppDescriptor();
    private long lastPrimaryMemUpdate;

    private static JcCoreService INSTANCE = new JcCoreService();
    private boolean running = false;
    private DatagramSocket socket;

    private Map<String, JcDistMsg> requestMsgMap = new HashMap<>();

    private JcCoreService() {
        LOG.setLevel(ch.qos.logback.classic.Level.ALL);
    }

    public final void start() throws Exception {
        start(null);
    }

    public final void start(Map<String, Object> config) throws Exception {
        if (config == null) {
            config = new HashMap<>();
        }
        if (!running) {
            Thread jcManagerThread;
            ManagedThreadFactory threadFactory = (ManagedThreadFactory) config.get("threadFactory");
            if (threadFactory == null) {
                jcManagerThread = new Thread(this::mainLoop);
            } else {
                jcManagerThread = threadFactory.newThread(this::mainLoop);
            }

            initUdpServer(config);
            initPrimaryMembers(config);
            running = true;
            jcManagerThread.setName("JcCore@" + selfDesc.getIpAddress() + ":" + selfDesc.getIpPort());
            jcManagerThread.start();
        }
    }

    private void initPrimaryMembers(Map<String, Object> config) {
        List<String> memList = (List<String>) config.get("primaryMembers");
        if (memList != null) {
            for (String ipPortStr : memList) {
                primaryMemberMap.put(ipPortStr, null);
            }
        }
    }

    private void initUdpServer(Map<String, Object> config) throws Exception {

        List<Integer> portList = (List<Integer>) config.get("udpListenPort");
        if (portList == null) {
            portList = new ArrayList<>();
            portList.add(UDP_LISTEN_PORT_DEFAULT);
        }

        int timeout = 2000;
        for (Integer port : portList) {
            try {
                socket = new DatagramSocket(port);
                socket.setSoTimeout(timeout);
                selfDesc.setIpPort(port);
                LOG.info("UDP Server init successfully on port: {}", port);
                return;
            } catch (SocketException ex) {
                LOG.error(null, ex);
            }
        }
        throw new Exception("Could not init UDP server from list: " + portList.toString());
    }

    byte[] buf = new byte[10_000];

    private boolean checkForMsg() {
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
            socket.receive(packet);

            ByteArrayInputStream bis = new ByteArrayInputStream(packet.getData());
            ObjectInput in = new ObjectInputStream(bis);
            Object ob = in.readObject();

            if (ob instanceof JcDistMsg) {
                JcDistMsg msg = (JcDistMsg) ob;
                msg.setSrcIpAddr(packet.getAddress().getHostAddress());
                processRecMsg(msg);
            }
            return true;
        } catch (IOException | ClassNotFoundException ex) {
            if (ex instanceof SocketTimeoutException) {

            } else {
                LOG.error(null, ex);
            }
        }
        return false;

    }

    private JcMember onMemberJoinMsg(JcDistMsg msg, String ipStrPortStr) {

        JcMember mem = memberMap.get(ipStrPortStr);
        if (mem == null) {
            mem = new JcMember(msg.getSrc());
            memberMap.put(ipStrPortStr, mem);
        }
        if (primaryMemberMap.containsKey(ipStrPortStr)) {
            primaryMemberMap.put(ipStrPortStr, mem);
        }
        mem.updateLastSeen();
        return mem;

    }

    private void onPingMsg(JcDistMsg msg) {
        String srcId = msg.getSrc().getIpStrPortStr();
        JcMember member = memberMap.get(srcId);
        if (member == null) {
            try {
                sendReqJoin(srcId);
                LOG.info("Discovered member: [" + srcId + "] from ping msg");
            } catch (Exception ex) {
                LOG.info(null, ex);
            }
        } else {
            member.updateLastSeen();
        }
        try {
            List<String> ipStrList = (List<String>) msg.getData();

            if (!ipStrList.contains(selfDesc.getIpStrPortStr())) {
                ipStrList.add(selfDesc.getIpStrPortStr());
            }

            int ttl = msg.getTtl();
            for (Map.Entry<String, JcMember> entry : memberMap.entrySet()) {
                String memId = entry.getKey();
                JcMember mem = entry.getValue();

                //forward to ones we don't have in our list
                if (!ipStrList.contains(memId)) {

                    if (!memId.equals(selfDesc.getIpStrPortStr())) {
                        mem.sendMessage(msg);
                        msg.setTtl(ttl);
                    } else {
                        LOG.warn("Received ping from self.");
                    }
                }
            }

        } catch (Exception e) {
            LOG.info(null, e);
        }
        LOG.trace("Receive ping message from: " + srcId);
    }

    private JcDistMsg generatePingMsg() {
        JcDistMsg msg = new JcDistMsg(JcDistMsgType.PING);
        List<String> memberIpStrList = new ArrayList<>();
        Set<String> keySet = memberMap.keySet();
        for (String ipStr : keySet) {
            memberIpStrList.add(ipStr);
        }
        memberIpStrList.add(selfDesc.getIpStrPortStr());
        msg.setData(memberIpStrList);
        msg.setSrc(selfDesc);
        return msg;
    }

    private void processRecMsg(JcDistMsg msg) {

        String ipStrPortStr = msg.getSrcIpAddr() + ":" + msg.getSrc().getIpPort();
        LOG.info("Received JcDistMsg: " + msg + "   SRC:" + ipStrPortStr);
        JcMember mem = memberMap.get(ipStrPortStr);

        switch (msg.getType()) {
            case JOIN:
                if (false) {
                    //authenticate first
                }
                mem = onMemberJoinMsg(msg, ipStrPortStr);
                //send response
                JcDistMsg resp = JcDistMsg.generateJoinResponse(msg, selfDesc);
                try {
                    mem.sendMessage(resp);
                } catch (IOException ex) {
                    LOG.error(null, ex);
                }
                break;

            case JOIN_RESP:
                JcDistMsg req = requestMsgMap.remove(msg.getMsgId());
                if (req == null) {
                    LOG.trace("Receive JOIN Response without request! " + msg);
                    break;
                }
                LOG.trace("Receive join response");
                onMemberJoinMsg(msg, ipStrPortStr);
                break;
            case PING:
                onPingMsg(msg);
                break;

            default:
                throw new AssertionError();
        }
    }

    private void checkMemberState() {
        LOG.info("Check member state");

        for (Map.Entry<String, JcMember> entry : primaryMemberMap.entrySet()) {
            String ipPortStr = entry.getKey();
            JcMember member = entry.getValue();
            if (entry.getValue() == null) {

//            memberMap.entrySet().stream().fi
                if (!memberMap.containsKey(ipPortStr)) {
                    try {
                        sendReqJoin(ipPortStr);
                    } catch (IOException ex) {
                        LOG.error(null, ex);
                    } catch (Exception ex) {
                        LOG.error(null, ex);
                        //TODO remove from memberMap here
                    }
                } else if (member.isLastSeenExpired()) {
                    JcDistMsg jcDistMsg = new JcDistMsg(JcDistMsgType.JOIN);
                    jcDistMsg.setSrc(selfDesc);
                    try {
                        member.sendMessage(jcDistMsg);
                    } catch (IOException ex) {
                        LOG.error(null, ex);
                    }
                }
            }
        }

        JcDistMsg ping = generatePingMsg();
        String strMembLog = "Cluster known members: \n\t";
        for (Iterator<Map.Entry<String, JcMember>> iterator = memberMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, JcMember> entry = iterator.next();

            String ipStrPortStr = entry.getKey();
            JcMember mem = entry.getValue();

            strMembLog += ipStrPortStr + "\n\t";

            if (mem.isLastSeenExpired()) {
                LOG.warn("Jc Member:" + ipStrPortStr + " TIMEOUT!");
                memberMap.remove(ipStrPortStr);
                if (primaryMemberMap.containsKey(ipStrPortStr)) {
                    primaryMemberMap.put(ipStrPortStr, null);
                }
            } else {
                try {
                    mem.sendMessage(ping);
                } catch (IOException ex) {
                    LOG.warn(null, ex);
                }
            }
        }
        LOG.debug(strMembLog);
    }

    private void sendReqJoin(String ipStrPortStr) throws IOException, Exception {
        String[] arr = ipStrPortStr.split(":");
        if (arr.length != 2) {
            throw new Exception("IP String invalid");
        }

        Integer port = Integer.valueOf(arr[1]);
        String ipAddr = arr[0];

        JcDistMsg jcDistMsg = new JcDistMsg(JcDistMsgType.JOIN);
        jcDistMsg.setSrc(selfDesc);
        requestMsgMap.put(jcDistMsg.getMsgId(), jcDistMsg);
        JcMember.sendMessage(port, ipAddr, jcDistMsg);
    }

    private void mainLoop() {
        while (running) {
            try {
                checkForMsg();
                if (System.currentTimeMillis() - lastPrimaryMemUpdate > 5_000) {
                    lastPrimaryMemUpdate = System.currentTimeMillis();
                    checkMemberState();
                }

            } catch (Exception e) {
                LOG.error(null, e);
            }
        }
    }

    public static Map<String, JcMember> getMemberMap() {
        return INSTANCE.memberMap;
    }

    public synchronized static JcCoreService getInstance() {

        if (INSTANCE == null) {
            INSTANCE = new JcCoreService();
        }

        return INSTANCE;
    }

    public static void main(String[] args) throws SocketException {
        Map<String, Object> argMap = new HashMap();
        List<String> primaryMemberList = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String[] arr = args[i].split("=");
            if (arr.length == 2) {
                if (arr[0].equals("udpListenPort")) {
                    Integer valueOf = Integer.valueOf(arr[1]);
                    ArrayList<Integer> arrayList = new ArrayList<>();
                    arrayList.add(valueOf);
                    argMap.put(arr[0], arrayList);
                } else if (arr[0].equals("primaryMember")) {
                    primaryMemberList.add(arr[1]);
                }
            }
        }
        argMap.put("primaryMembers", primaryMemberList);
        try {
            INSTANCE.start(argMap);
        } catch (Exception ex) {
            LOG.error(null, ex);
        }

    }

}
