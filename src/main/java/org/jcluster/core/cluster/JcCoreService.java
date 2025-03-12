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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.jcluster.core.bean.FilterDescBean;

/**
 *
 * @autor Henry Thomas
 */
public final class JcCoreService {

    private static final Logger LOG = (Logger) LoggerFactory.getLogger(JcCoreService.class);

    public static final int UDP_LISTEN_PORT_DEFAULT = 4445;

    private final Map<String, FilterDescBean> selfFilterMap = new ConcurrentHashMap<>();

    private final Map<String, JcMember> memberMap = new ConcurrentHashMap<>();
    private final Map<String, JcMember> primaryMemberMap = new HashMap<>();

    private final JcAppDescriptor selfDesc = new JcAppDescriptor();
    private long lastPrimaryMemUpdate = 0l;

    private static JcCoreService INSTANCE = new JcCoreService();
    private boolean running = false;
    private DatagramSocket socket;

    byte[] buf = new byte[65535 - 28];

    private final Map<String, JcDistMsg> requestMsgMap = new HashMap<>();
    private final Map<String, Set<String>> subscTopicFilterMap = new HashMap<>();
    private final Map<String, Set<String>> subscAppFilterMap = new HashMap<>();

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

                String memID = msg.getSrcIpAddr() + ":" + msg.getSrc().getIpPort();
                LOG.info("Received JcDistMsg: {}  SRC:{}  Size:{}b", msg.getType(), memID, packet.getData().length);
                processRecMsg(msg, memID);
            } else {
                LOG.warn("Received JcDistMsg  from invalid type: ", ob.getClass().getName());
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
        onMemberAdd(mem);
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

    private void onSubscMsg(JcMember mem, JcDistMsg msg) {
        if (msg.getData() instanceof String) {
            String filter = (String) msg.getData();

        }
    }

    private void processRecMsg(JcDistMsg msg, String memID) {

        JcMember mem = memberMap.get(memID);

        switch (msg.getType()) {
            case JOIN:
                if (false) {
                    //authenticate first
                }
                mem = onMemberJoinMsg(msg, memID);
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
                onMemberJoinMsg(msg, memID);
                break;
            case PING:
                onPingMsg(msg);
                break;
            case SUBSCRIBE:
                if (mem != null) {
                    onSubscMsg(mem, msg);
                }
                break;

            default:
                throw new AssertionError();
        }
    }

    private void onMemberAdd(JcMember mem) {

    }

    private void onMemberRemove(JcMember mem) {
        mem.close();
        //remove member if has been subscribe to something
        selfFilterMap.entrySet().forEach(entry -> {
            entry.getValue().removeSubscirber(mem.getId());
        });
    }

    private void updateMember(JcMember mem) {
        Set<String> toSubscribe = new HashSet<>();

        //verify subscription is correct for app filters
        for (Map.Entry<String, Set<String>> entry : subscAppFilterMap.entrySet()) {
            String appName = entry.getKey();
            Set<String> reqSubsFiltSet = entry.getValue();

            //check if same appName
            if (Objects.equals(appName, mem.getDesc().getAppName())) {
                toSubscribe.addAll(reqSubsFiltSet.stream()
                        .filter((t) -> !mem.getSubscribtionSet().contains(t))
                        .collect(Collectors.toSet()));
            }
        }

        //verify subscription is correct for topic filters
        for (Map.Entry<String, Set<String>> entry : subscTopicFilterMap.entrySet()) {
            String topic = entry.getKey();
            Set<String> reqSubsFiltSet = entry.getValue();

            //check if same appName
            if (mem.getDesc().getTopicList().contains(topic)) {
                toSubscribe.addAll(reqSubsFiltSet.stream()
                        .filter((t) -> !mem.getSubscribtionSet().contains(t))
                        .collect(Collectors.toSet()));
            }
        }

        //subscribe if needed
        for (String filterName : toSubscribe) {

            JcDistMsg jcDistMsg = new JcDistMsg(JcDistMsgType.SUBSCRIBE);
            jcDistMsg.setSrc(selfDesc);
            jcDistMsg.setData(filterName);
            try {
                LOG.info("Sending subscription request filter:[{}] to {}",
                        filterName, mem.getId());

                mem.sendMessage(jcDistMsg);
                requestMsgMap.put(jcDistMsg.getMsgId(), jcDistMsg);
            } catch (IOException ex) {
                LOG.warn(null, ex);
            }
        }

    }

    private void checkMemberState() {
        LOG.info("Check member state");

        for (Map.Entry<String, JcMember> entry : primaryMemberMap.entrySet()) {
            String memId = entry.getKey();
            JcMember mem = entry.getValue();
            if (entry.getValue() == null) {

//            memberMap.entrySet().stream().fi
                if (!memberMap.containsKey(memId)) {
                    try {
                        sendReqJoin(memId);
                    } catch (IOException ex) {
                        LOG.error(null, ex);
                    } catch (Exception ex) {
                        LOG.error(null, ex);
                        //TODO remove from memberMap here
                    }
                } else if (mem.isLastSeenExpired()) {
                    JcDistMsg jcDistMsg = new JcDistMsg(JcDistMsgType.JOIN);
                    jcDistMsg.setSrc(selfDesc);
                    try {
                        mem.sendMessage(jcDistMsg);
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

            String memId = entry.getKey();
            JcMember mem = entry.getValue();

            strMembLog += memId + "\n\t";

            if (mem.isLastSeenExpired()) {
                LOG.warn("Jc Member:" + memId + " TIMEOUT!");
                memberMap.remove(memId);
                if (primaryMemberMap.containsKey(memId)) {
                    primaryMemberMap.put(memId, null);
                }

                onMemberRemove(mem);
                mem.close();
            } else {
                try {
                    mem.sendMessage(ping);

                } catch (IOException ex) {
                    LOG.warn(null, ex);
                }
                try {
                    updateMember(mem);
                } catch (Exception ex) {
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

    public void removeSelfFilterValue(String filterName, Object value) {
        FilterDescBean fd = selfFilterMap.get(filterName);
        if (fd == null) {
            fd = new FilterDescBean(filterName);
            selfFilterMap.put(filterName, fd);
        }
        fd.removeFilterValue(value);

        //notify all subscribers for this value
        //TODO
    }

    public void addSelfFilterValue(String filterName, Object value) {
        FilterDescBean fd = selfFilterMap.get(filterName);
        if (fd == null) {
            fd = new FilterDescBean(filterName);
            selfFilterMap.put(filterName, fd);
        }
        //add value
        fd.addFilterValue(value);

        //notify all subscribers for this value
        //TODO
    }

    public boolean subscribeToTopicFilter(String topic, String filterName) {
        if (topic == null) {
            return false;
        }
        Set<String> topicSet = subscTopicFilterMap.get(topic);
        if (topicSet == null) {
            topicSet = new HashSet<>();
            subscTopicFilterMap.put(topic, topicSet);
        }

        //force immediate subscription
        lastPrimaryMemUpdate = 0l;

        return topicSet.add(filterName);
    }

    public boolean subscribeToAppFilter(String appName, String filterName) {
        if (appName == null) {
            return false;
        }

        Set<String> topicSet = subscAppFilterMap.get(appName);
        if (topicSet == null) {
            topicSet = new HashSet<>();
            subscAppFilterMap.put(appName, topicSet);
        }

        //force immediate subscription
        lastPrimaryMemUpdate = 0l;

        return topicSet.add(filterName);
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
