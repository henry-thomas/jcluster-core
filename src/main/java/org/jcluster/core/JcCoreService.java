/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.enterprise.concurrent.ManagedExecutorService;
import org.jcluster.core.bean.FilterDescBean;
import org.jcluster.core.config.JcAppConfig;
import static org.jcluster.core.messages.JcDistMsgType.JOIN;
import static org.jcluster.core.messages.JcDistMsgType.JOIN_RESP;
import static org.jcluster.core.messages.JcDistMsgType.PING;
import static org.jcluster.core.messages.JcDistMsgType.SUBSCRIBE;
import org.jcluster.core.messages.PublishMsg;

/**
 *
 * @autor Henry Thomas
 */
public final class JcCoreService {

    private static final Logger LOG = (Logger) LoggerFactory.getLogger(JcCoreService.class);

    public static final int UDP_LISTEN_PORT_DEFAULT = 4445;

    //to keep track on own filters set
    //key is filterName
    //value is FilterDescBean with set of values inside
    private final Map<String, FilterDescBean> selfFilterMap = new ConcurrentHashMap<>();

    private final Map<String, JcMember> memberMap = new ConcurrentHashMap<>();

    private final Map<String, JcMember> primaryMemberMap = new HashMap<>();

    private final JcAppDescriptor selfDesc = new JcAppDescriptor();
    private long lastPrimaryMemUpdate = 0l;

    private static JcCoreService INSTANCE = new JcCoreService();
    private boolean running = false;
    private DatagramSocket socket;

    private byte[] buf = new byte[65535 - 28];

    //to keep track on response meessages
    private final Map<String, JcDistMsg> requestMsgMap = new HashMap<>();

    //to keep track on where we need to subscribe, value is a list of filter names
    //key is topicName
    private final Map<String, Set<String>> subscTopicFilterMap = new HashMap<>();
    //to keep track on where we need to subscribe, value is a list of filter names
    //the key can be used for showing what app we need to connect to
    //key is appName
    private final Map<String, Set<String>> subscAppFilterMap = new HashMap<>();

    private ExecutorService executorService = null;
    private ThreadFactory threadFactory = null;

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
            Thread jcRemConThread;

            ManagedExecutorService mes = (ManagedExecutorService) config.get("executorService");
            if (mes != null) {
                executorService = mes;
            } else {
                executorService = new ThreadPoolExecutor(5, 50, 0L, TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<Runnable>());
            }

            ManagedThreadFactory tf = (ManagedThreadFactory) config.get("threadFactory");
            if (tf == null) {
                threadFactory = Executors.defaultThreadFactory();
            } else {
                threadFactory = tf;
            }

            jcManagerThread = threadFactory.newThread(this::mainLoop);
            jcRemConThread = threadFactory.newThread(this::startTcpConnectionMonitor);

            initUdpServer(config);
            initPrimaryMembers(config);
            running = true;
            jcManagerThread.setName("JcCore@" + selfDesc.getIpAddress() + ":" + selfDesc.getIpPort());
            jcManagerThread.start();
            jcRemConThread.start();
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

    private void onSubscRequestMsg(JcMember mem, JcDistMsg msg) {
        if (msg.getData() instanceof String) {
            String filterName = (String) msg.getData();
            FilterDescBean fd = selfFilterMap.get(filterName);

            JcDistMsg resp = new JcDistMsg(JcDistMsgType.SUBSCRIBE_RESP, msg.getMsgId(), msg.getTtl());
            resp.setSrc(selfDesc);

            if (fd == null) {
                //send response that you can help the member because not having filter liket hwi
                LOG.warn("Receive subscription INVALID request from: {} filter: {}",
                        mem.getId(), filterName);

                PublishMsg pm = new PublishMsg();
                pm.setOperationType(PublishMsg.OPER_TYPE_INVALID_FNAME);
                resp.setData(pm);

            } else {
                LOG.info("Receive subscription request from: {} filter: {}",
                        mem.getId(), filterName);

                PublishMsg pm = new PublishMsg();
                pm.setOperationType(PublishMsg.OPER_TYPE_ADDBULK);
                pm.setTransCount(fd.getTransCount());
                pm.getValueSet().addAll(fd.getValueSet());

            }

            try {
                mem.sendMessage(msg);
            } catch (IOException ex) {
                LOG.warn(null, ex);
            }
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
                    onSubscRequestMsg(mem, msg);
                }
                break;
            case SUBSCRIBE_RESP:
                if (mem != null) {
                    mem.onSubscResponseMsg(msg);
                }
                break;
            case PUBLISH_FILTER:
                if (mem != null) {
                    mem.onFilterPublishMsg(msg);
                }
                break;

            default:
                throw new AssertionError();
        }
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

    private void tcpConnectionMonitorLoop() {

        for (Map.Entry<String, JcMember> entry : memberMap.entrySet()) {
            String memId = entry.getKey();
            JcMember mem = entry.getValue();
            JcRemoteInstanceConnectionBean ri = mem.getConector();

            //validate all instances have no stuck connections
            ri.validateTimeoutsAllConn();

            //validate all instances have correct amount of outbound connections
            //we have to check the app name because there is another apps that makes only inboudn connections
            if (subscAppFilterMap.containsKey(mem.getDesc().getAppName())
                    || !Collections.disjoint(mem.getDesc().getTopicList(), subscAppFilterMap.keySet())) {
                ri.validateOutboundConnectionCount(JcAppConfig.getINSTANCE().getMinConnections());
            }

            //Create INBOUND incase this instance is isolated
//        if (selfDesc.isIsolated()) {
//            for (Map.Entry<String, JcRemoteInstanceConnectionBean> entry : remoteInstanceMap.entrySet()) {
//                JcRemoteInstanceConnectionBean ri = entry.getValue();
//
//                //we have to check the app name because there is another apps that makes only inboudn connections
//                if (ri.getRemoteAppDesc().getOutboundAppNameList().contains(selfDesc.getAppName())
//                        || !Collections.disjoint(ri.getRemoteAppDesc().getTopicList(), topicList)) {
//
//                    ri.validateInboundConnectionCount(ri.getRemoteAppDesc().getOutBoundMinConnection());
//                }
//
//            }
//        }
        }
    }

    private void startTcpConnectionMonitor() {
        LOG.info("JCLUSTER Health Checker Startup...");
        Thread.currentThread().setName("J-CLUSTER_RenCon_Health");
        while (running) {
            long beginCycle = System.currentTimeMillis();
            //LOG.info("JCluster Health_Checker cycle");

            try {
                tcpConnectionMonitorLoop();
            } catch (Exception e) {
                LOG.warn(null, e);
            }

            if (System.currentTimeMillis() - beginCycle < 500) {
                try {
                    synchronized (this) {
                        this.wait(500 - (System.currentTimeMillis() - beginCycle));
                    }
                } catch (InterruptedException ex) {
                }
            }
        }

        LOG.info("Shutting down {}", Thread.currentThread().getName());
    }

    protected List<JcRemoteInstanceConnectionBean> getMemConByApp(String app) {
        List<JcRemoteInstanceConnectionBean> riList = memberMap.values().stream()
                .filter((mem) -> Objects.equals(mem.getDesc().getAppName(), app))
                .filter((mem) -> mem.getConector().isOutboundAvailable())
                .map((mem) -> mem.getConector())
                .collect(Collectors.toList());

        return riList;
    }

    protected List<JcRemoteInstanceConnectionBean> getMemConByTopic(String topic) {
        List<JcRemoteInstanceConnectionBean> riList = memberMap.values().stream()
                .filter((mem) -> mem.getConector().isOutboundAvailable())
                .filter((mem) -> mem.getDesc().getTopicList().contains(topic))
                .map((mem) -> mem.getConector())
                .collect(Collectors.toList());

        return riList;

    }

    protected JcRemoteInstanceConnectionBean getMemConByTopicAndFilter(String topic, Map<String, Object> fMap) {
        JcMember foundMem = memberMap.values().stream()
                .filter((mem) -> mem.getConector().isOutboundAvailable())
                .filter((mem) -> mem.getDesc().getTopicList().contains(topic))
                .filter((mem) -> mem.containsFilter(fMap))
                .findFirst().get();

        if (foundMem == null) {
            return null;
        }

        return foundMem.getConector();

    }

    protected JcRemoteInstanceConnectionBean getMemConByTopicSingle(String topic) {
        JcMember foundMem = memberMap.values().stream()
                .filter((mem) -> mem.getConector().isOutboundAvailable())
                .filter((mem) -> mem.getDesc().getTopicList().contains(topic))
                .findFirst().get();

        if (foundMem == null) {
            return null;
        }

        return foundMem.getConector();

    }

    protected JcRemoteInstanceConnectionBean getMemConByAppAndFilter(String app, Map<String, Object> fMap) {
        JcMember foundMem = memberMap.values().stream()
                .filter((mem) -> mem.getConector().isOutboundAvailable())
                .filter((mem) -> Objects.equals(mem.getDesc().getAppName(), app))
                .filter((mem) -> mem.containsFilter(fMap))
                .findFirst().get();

        if (foundMem == null) {
            return null;
        }

        return foundMem.getConector();

    }

    protected JcRemoteInstanceConnectionBean getMemConByAppSingle(String app) {
        JcMember foundMem = memberMap.values().stream()
                .filter((mem) -> mem.getConector().isOutboundAvailable())
                .filter((mem) -> Objects.equals(mem.getDesc().getAppName(), app))
                .findFirst().get();

        if (foundMem == null) {
            return null;
        }

        return foundMem.getConector();

    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    public JcAppDescriptor getSelfDesc() {
        return selfDesc;
    }

}
