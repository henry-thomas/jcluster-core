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
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.naming.NamingException;
import org.jcluster.core.bean.FilterDescBean;
//import org.jcluster.core.config.JcAppConfig;
import org.jcluster.core.exception.JcRuntimeException;
import org.jcluster.core.exception.cluster.JcInstanceNotFoundException;
import static org.jcluster.core.messages.JcDistMsgType.JOIN;
import static org.jcluster.core.messages.JcDistMsgType.JOIN_RESP;
import static org.jcluster.core.messages.JcDistMsgType.PING;
import static org.jcluster.core.messages.JcDistMsgType.SUBSCRIBE;
//import org.jcluster.core.messages.JcMessage;
//import org.jcluster.core.messages.JcMsgFragment;
import static org.jcluster.core.messages.JcMsgFragment.FRAGMENT_DATA_MAX_SIZE;
import org.jcluster.core.messages.PublishMsg;
import org.jcluster.core.monitor.AppMetricMonitorInterface;
import org.jcluster.core.monitor.AppMetricsMonitor;
import org.jcluster.core.monitor.JcMetrics;

/**
 *
 * @autor Henry Thomas
 */
public final class JcCoreService {

    private static final Logger LOG = (Logger) LoggerFactory.getLogger(JcCoreService.class);

    public static final int UDP_LISTEN_PORT_DEFAULT = 4445;

    private boolean enterprise = false;

    private int outBoundMinConnection;

    //to keep track on own filters set
    //key is filterName
    //value is FilterDescBean with set of values inside
    private final Map<String, FilterDescBean> selfFilterValueMap = new ConcurrentHashMap<>();

    private final Map<String, JcMember> memberMap = new ConcurrentHashMap<>();

    private final Map<String, JcMember> primaryMemberMap = new HashMap<>();

    private JcMetrics metrics;

    protected final JcAppDescriptor selfDesc = new JcAppDescriptor();
    private long lastPrimaryMemUpdate = 0l;
    private long lastMemSubscriptionTimestamp = 0l;

    private static JcCoreService INSTANCE = new JcCoreService();
    private boolean running = false;
    private DatagramSocket socketUdpRx;

    public static final int UDP_FRAME_MAX_SIZE = 65535 - 28;
    public static final int UDP_FRAME_FRAGMENTATION_SIZE = UDP_FRAME_MAX_SIZE - 2048;

    private final byte[] buf = new byte[UDP_FRAME_MAX_SIZE];  //UDP frame 4byte, IP frame 16byte, 

    //to keep track on response meessages
    protected final Map<String, JcDistMsg> requestMsgMap = new HashMap<>();

    //to keep track on where we need to subscribe, value is a list of filter names
    //key is topicName
    private final Map<String, Set<String>> subscTopicFilterMap = new HashMap<>();
    //to keep track on where we need to subscribe, value is a list of filter names
    //the key can be used for showing what app we need to connect to
    //key is appName
    private final Map<String, Set<String>> subscAppFilterMap = new HashMap<>();

    private ExecutorService executorService = null;
    private ThreadFactory threadFactory = null;

    private JcServerEndpoint serverEndpoint;

    //Buffer for all received messages
    protected final BlockingQueue<JcDistMsg> receiveBufferQueue = new ArrayBlockingQueue<>(4096);

    private final List<MemberEventListener> memberEventList = new ArrayList<>();

    private JcCoreService() {
        LOG.setLevel(ch.qos.logback.classic.Level.ALL);
    }

    public final JcMetrics getMetrics() {
        return metrics;
    }

    public final void stop() throws Exception {
        if (running) {
            LOG.info("JCLUSTER -- Shutdown... APPNAME: [{}] InstanceID: [{}]", selfDesc.getAppName(), selfDesc.getInstanceId());
            running = false;
            memberMap.forEach((t, member) -> {
                try {
                    JcDistMsg msg = new JcDistMsg(JcDistMsgType.LEAVE);
                    msg.setSrc(selfDesc);
                    member.sendMessage(msg);

                    onMemberRemove(member);

                } catch (IOException ex) {
                    java.util.logging.Logger.getLogger(JcCoreService.class.getName()).log(Level.SEVERE, null, ex);
                }
            });

            if (socketUdpRx != null) {
                socketUdpRx.close();
            }
        } else {
            LOG.info("JCLUSTER -- Invalid Shutdown... APPNAME: [{}] InstanceID: [{}]", selfDesc.getAppName(), selfDesc.getInstanceId());

        }
    }

    public final void start() throws Exception {
        start(null);
    }

    public final void start(Map<String, Object> config) throws Exception {
        if (config == null) {
            config = JcManager.getDefaultConfig(enterprise);
        }

        if (!running) {
            if (config.containsKey("appName")) {
                selfDesc.setAppName((String) config.get("appName"));
            }
//            if (config.containsKey("tcpListenPort")) {
//                selfDesc.setIpPortListenTCP((int) config.get("tcpListenPort"));
//            }
            if (config.containsKey("selfIpAddress")) {
                selfDesc.setIpAddress((String) config.get("selfIpAddress"));
            } else {
                throw new JcRuntimeException("Missing property for [selfIpAddress]");
            }

            LOG.info("JCLUSTER -- Startup... APPNAME: [{}] InstanceID: [{}]", selfDesc.getAppName(), selfDesc.getInstanceId());

            ManagedExecutorService mes = (ManagedExecutorService) config.get("executorService");
            if (mes != null) {
                executorService = mes;
            } else {
                executorService = new ThreadPoolExecutor(5, 50, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
            }

            ManagedThreadFactory tf = (ManagedThreadFactory) config.get("threadFactory");
            if (tf == null) {
                threadFactory = Executors.defaultThreadFactory();
            } else {
                threadFactory = tf;
            }

            running = true;

            initUdpServer(config);
            initPrimaryMembers(config);

            //start UDP receive thread
            Thread jcManagerThread = threadFactory.newThread(this::mainLoopUdpRx);
            jcManagerThread.setName("JcCore-UDP_RX@" + selfDesc.getIpAddress() + ":" + selfDesc.getIpPortListenUDP());
            jcManagerThread.start();

            //start Main receive thread handler
            Thread jcManagerMonitorThread = threadFactory.newThread(this::mainLoopMonitor);
            jcManagerMonitorThread.setName("JcCore-ProcUdp_RX@" + selfDesc.getIpAddress() + ":" + selfDesc.getIpPortListenUDP());
            jcManagerMonitorThread.start();

            Thread jcRemConThread = threadFactory.newThread(this::startTcpConnectionMonitor);
            jcRemConThread.start();
            //TCP server 
//            serverEndpoint = new JcServerEndpoint(threadFactory, (List<Integer>) config.get("tcpListenPort"));
            serverEndpoint = new JcServerEndpoint(threadFactory);
            Thread serverThread = threadFactory.newThread(serverEndpoint);

            serverThread.start();

            metrics = new JcMetrics(selfDesc);
            JcManager.getInstance().registerLocalClassImplementation(AppMetricsMonitor.class);

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

//        int timeout = 2000;
        SocketException lastEx = null;
        for (Integer port : portList) {
            try {
                socketUdpRx = new DatagramSocket(port);
                socketUdpRx.setReceiveBufferSize(FRAGMENT_DATA_MAX_SIZE * 500);
//                socketUdpRx.setSoTimeout(timeout);
                selfDesc.setIpPortListenUDP(port);
                LOG.info("UDP Server init successfully on port: {}", port);
                return;
            } catch (SocketException ex) {
                lastEx = ex;
            }
        }
        if (lastEx != null) {
            throw lastEx;
        }
        throw new Exception("Could not init UDP server from list: " + portList.toString());
    }

    protected JcMember getMember(String memId) {
        return memberMap.get(memId);
    }

    private JcMember onMemberJoinMsg(JcDistMsg msg, String memId) {
        JcMember mem = memberMap.get(memId);
        if (mem == null) {
            mem = new JcMember(msg.getSrc(), this);
            memberMap.put(memId, mem);
        }
        if (primaryMemberMap.containsKey(memId)) {
            primaryMemberMap.put(memId, mem);
        }
        mem.updateLastSeen();
        onMemberAdd(mem);
        return mem;

    }

    private void onPingMsg(JcDistMsg msg) {
        String srcId = msg.getSrc().getIpStrPortStr();
        JcMember mem = memberMap.get(srcId);
        if (mem == null) {
            try {
                sendReqJoin(srcId);
                LOG.info("Discovered member: [" + srcId + "] from ping msg");
            } catch (Exception ex) {
                LOG.info(null, ex);
            }
        } else {
            if (msg.getSrc().getInstanceId().equals(mem.getDesc().getInstanceId())) {
                mem.updateLastSeen();
            } else {
                LOG.warn("Jc Member:[{}]  InstanceID:[{}] incorect id!", mem.getDesc().getIpStrPortStr(), mem.getDesc().getInstanceId());
                memberMap.remove(srcId);
                onMemberRemove(mem);

                try {
                    sendReqJoin(srcId);
                } catch (Exception ex) {
                }
                LOG.info("Try to reconnect to new member: [" + srcId + "] from ping invalid InstanceId msg");
            }

        }
        try {
            List<String> ipStrList = (List<String>) msg.getData();

            if (!ipStrList.contains(selfDesc.getIpStrPortStr())) {
                ipStrList.add(selfDesc.getIpStrPortStr());
            }

            int ttl = msg.getTtl();
            for (Map.Entry<String, JcMember> entry : memberMap.entrySet()) {
                String memId = entry.getKey();
                JcMember m = entry.getValue();

                //forward to ones we don't have in our list
                if (!ipStrList.contains(memId)) {

                    if (!memId.equals(selfDesc.getIpStrPortStr())) {
                        m.sendMessage(msg);
                        msg.setTtl(ttl);
                    } else {
                        LOG.warn("Received ping from self.");
                    }
                }
            }

        } catch (Exception e) {
            LOG.info(null, e);
        }
//        LOG.trace("Receive ping message from: " + srcId);
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
            FilterDescBean fd = selfFilterValueMap.get(filterName);
            if (fd == null) {
                fd = new FilterDescBean(filterName);
                selfFilterValueMap.put(filterName, fd);
            }

            JcDistMsg resp = new JcDistMsg(JcDistMsgType.SUBSCRIBE_RESP, msg.getMsgId(), msg.getTtl());
            resp.setSrc(selfDesc);

            LOG.info("Receive subscription request from: {} filter: {}", mem.getId(), filterName);

            PublishMsg pm = new PublishMsg();
            pm.setOperationType(PublishMsg.OPER_TYPE_ADDBULK);
            pm.setFilterName(filterName);
            pm.setTransCount(fd.getTransCount());
            pm.getValueSet().addAll(fd.getValueSet());
            resp.setData(pm);

            try {
                mem.sendMessage(resp);
                mem.getSubscribtionSet().add(filterName);
            } catch (IOException ex) {
                LOG.warn(null, ex);
            }
        }
    }

    private void onMemberAdd(JcMember mem) {
        //Add from remote since we have him already
        RemMembFilter filter = mem.getOrCreateFilterTarget(AppMetricMonitorInterface.JC_INSTANCE_FILTER);
        filter.addFilterValue(mem.getDesc().getInstanceId());

        MemberEvent ev = new MemberEvent(mem, MemberEvent.TYPE_ADD);
        memberEventList.forEach((e) -> {
            try {
                e.onEvent(ev);
            } catch (Exception ex) {
                LOG.error(null, ex);
            }
        });
    }

    private void onMemberRemove(JcMember mem) {
        mem.close();
        //remove member if has been subscribe to something
        selfFilterValueMap.entrySet().forEach(entry -> {
            entry.getValue().removeSubscirber(mem.getId());
        });

        //Remove from remote since we have him already
        RemMembFilter filter = mem.getOrCreateFilterTarget(AppMetricMonitorInterface.JC_INSTANCE_FILTER);
        filter.removeFilterValue(mem.getDesc().getInstanceId());

        MemberEvent ev = new MemberEvent(mem, MemberEvent.TYPE_REMOVE);
        memberEventList.forEach((e) -> {
            try {
                e.onEvent(ev);
            } catch (Exception ex) {
                LOG.error(null, ex);
            }
        });

    }

    private void updateMemberSubscription(JcMember mem) {
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

        if (toSubscribe.isEmpty()) {
            mem.verifyFilterIntegrity();
        }

    }

    private void checkMemberSubscribtion() {
        for (Map.Entry<String, JcMember> entry : memberMap.entrySet()) {

            String memId = entry.getKey();
            JcMember mem = entry.getValue();

            try {
                updateMemberSubscription(mem);
            } catch (Exception ex) {
                LOG.warn(null, ex);
            }
        }
    }

    private void checkMemberState() {
//        LOG.trace("Check member state");

        for (Map.Entry<String, JcMember> entry : primaryMemberMap.entrySet()) {
            String memId = entry.getKey();
            JcMember mem = entry.getValue();
            if (mem == null) {

//            memberMap.entrySet().stream().fi
                if (!memberMap.containsKey(memId) && !memId.equals(selfDesc.getIpStrPortStr())) {
                    try {
                        sendReqJoin(memId);
                    } catch (IOException ex) {
                        LOG.error(null, ex);
                    } catch (Exception ex) {
                        LOG.error(null, ex);
                        //TODO remove from memberMap here
                    }
                } else if (mem != null && mem.isLastSeenExpired()) {
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
//        String strMembLog = "Cluster known members: \n\t";
        for (Iterator<Map.Entry<String, JcMember>> iterator = memberMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, JcMember> entry = iterator.next();

            String memId = entry.getKey();
            JcMember mem = entry.getValue();

//            strMembLog += memId + "\n\t";
            if (mem.isLastSeenExpired()) {
                LOG.warn("Jc Member:" + memId + " TIMEOUT!");
                memberMap.remove(memId);
                if (primaryMemberMap.containsKey(memId)) {
                    primaryMemberMap.put(memId, null);
                }
                onMemberRemove(mem);
            } else {
                try {
                    mem.sendMessage(ping);
                } catch (IOException ex) {
                    LOG.warn(null, ex);
                }

                mem.verifyRxFrag();
                metrics.updateMemMetrics(mem);

            }
        }
//        LOG.debug(strMembLog);
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
        sendMessage(jcDistMsg, ipAddr, port);

    }

    private void sendMessage(JcDistMsg msg, String ip, int port) throws IOException {
        if (msg.hasTTLExpire()) {
            return;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = new ObjectOutputStream(bos);
        out.writeObject(msg);

        byte[] data = bos.toByteArray();

        DatagramPacket p = new DatagramPacket(data, data.length, InetAddress.getByName(ip), port);
        socketUdpRx.send(p);
    }

    
    private void processRecMsg(JcDistMsg msg, String memId) {

        JcMember mem = memberMap.get(memId);
        switch (msg.getType()) {
            case JOIN:
                if (false) {
                    //authenticate first
                }
                mem = onMemberJoinMsg(msg, memId);
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
                onMemberJoinMsg(msg, memId);
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
            case FRG_DATA:
                if (mem != null) {
                    mem.onFrgMsgReceived(msg);
                } else {
                    System.out.println("");
                }
                break;
            case FRG_ACK:
                if (mem != null) {
                    mem.onFrgMsgAck(msg);
                }
                break;
            case FRG_RESEND:
                if (mem != null) {
                    mem.onFrgMsgResend(msg);
                }
                break;

            default:
                LOG.error("Receive unknown UDP msg type: [{}]", msg.getType());
//                throw new AssertionError();
        }
    }

    protected void onFragmentedDataRx(byte data[]) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            ObjectInput in = new ObjectInputStream(bis);
            Object ob = in.readObject();

            if (ob instanceof JcDistMsg) {
                JcDistMsg msg = (JcDistMsg) ob;
                String memId = msg.getSrc().getIpAddress() + ":" + msg.getSrc().getIpPortListenUDP();
                processRecMsg(msg, memId);
            } else {
                LOG.warn("Received JcDistMsg  from invalid type: ", ob.getClass().getName());
            }
        } catch (IOException | ClassNotFoundException ex) {
            LOG.error(null, ex);
        }
    }

    private void mainLoopUdpRx() {
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                try {
                    socketUdpRx.receive(packet);

                    ByteArrayInputStream bis = new ByteArrayInputStream(packet.getData());
                    ObjectInput in = new ObjectInputStream(bis);
                    Object ob = in.readObject();

                    if (ob instanceof JcDistMsg) {
                        JcDistMsg msg = (JcDistMsg) ob;
                        msg.setSrcIpAddr(packet.getAddress().getHostAddress());

                        boolean status = receiveBufferQueue.add(msg);
                        if (!status) {
                            LOG.warn("UDP receive buffer full!");
                        }

                    } else {
                        LOG.warn("Received JcDistMsg  from invalid type: ", ob.getClass().getName());
                    }

                } catch (IOException | ClassNotFoundException ex) {
                    if (ex instanceof SocketTimeoutException) {

                    } else {
                        LOG.error(null, ex);
                    }
                }

            } catch (Exception e) {
                LOG.error(null, e);
            }
        }
    }

    private void mainLoopMonitor() {
        while (running) {
            try {
                JcDistMsg msg = receiveBufferQueue.poll(1000, TimeUnit.MILLISECONDS);
                if (msg != null) {
                    String memId = msg.getSrcIpAddr() + ":" + msg.getSrc().getIpPortListenUDP();
                    processRecMsg(msg, memId);
                }

                if (System.currentTimeMillis() - lastPrimaryMemUpdate > 500) {
                    lastPrimaryMemUpdate = System.currentTimeMillis();
                    checkMemberState();
                }

                if (System.currentTimeMillis() - lastMemSubscriptionTimestamp > 5000) {
                    lastMemSubscriptionTimestamp = System.currentTimeMillis();
                    checkMemberSubscribtion();
                }

            } catch (Exception e) {
                LOG.error(null, e);
            }
        }
    }

    public static Map<String, JcMember> getMemberMap() {
        return INSTANCE.memberMap;
    }

    public static JcCoreService getInstance() {

        if (INSTANCE == null) {
            INSTANCE = new JcCoreService();
        }

        return INSTANCE;
    }

    public void removeSelfFilterValue(String filterName, Object value) {
        FilterDescBean fd = selfFilterValueMap.get(filterName);
        if (fd == null) {
            fd = new FilterDescBean(filterName);
            selfFilterValueMap.put(filterName, fd);
        }
        fd.removeFilterValue(value);

        //notify all subscribers for this value
        List<JcMember> subscribers = memberMap.values().stream()
                .filter((t) -> t.isSubscribed(filterName))
                .collect(Collectors.toList());

        PublishMsg pm = new PublishMsg();
        pm.setFilterName(filterName);
        pm.setValue(value);
        pm.setOperationType(PublishMsg.OPER_TYPE_REMOVE);
        pm.setTransCount(fd.getTransCount());

        publisFilterChange(subscribers, pm);
    }

    public void addSelfFilterValue(String filterName, Object value) {
        FilterDescBean fd = selfFilterValueMap.get(filterName);
        if (fd == null) {
            fd = new FilterDescBean(filterName);
            selfFilterValueMap.put(filterName, fd);
        }
        //add value
        fd.addFilterValue(value);

        //notify all subscribers for this value
        List<JcMember> subscribers = memberMap.values().stream()
                .filter((t) -> t.isSubscribed(filterName))
                .collect(Collectors.toList());

        PublishMsg pm = new PublishMsg();
        pm.setFilterName(filterName);
        pm.setValue(value);
        pm.setOperationType(PublishMsg.OPER_TYPE_ADD);
        pm.setTransCount(fd.getTransCount());

        publisFilterChange(subscribers, pm);
    }

    private void publisFilterChange(List<JcMember> subscribers, PublishMsg pm) {
        JcDistMsg msg = new JcDistMsg(JcDistMsgType.PUBLISH_FILTER);
        msg.setSrc(selfDesc);
        msg.setData(pm);
        subscribers.forEach((t) -> {
            try {
                t.sendMessage(msg);
            } catch (IOException ex) {
                LOG.warn(null, ex);
            }
        });
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
        if (filterName != null) {
            topicSet.add(filterName);
        }
        return true;
    }

//   
    private void tcpConnectionMonitorLoop() {

        for (Map.Entry<String, JcMember> entry : memberMap.entrySet()) {
            String memId = entry.getKey();
            JcMember mem = entry.getValue();
            JcRemoteInstanceConnectionBean ri = mem.getConector();

            //validate all instances have no stuck connections
            ri.validateTimeoutsAllConn();

            //validate all instances have correct amount of outbound connections
            //we have to check the app name because there is another apps that makes only inboudn connections
            if (ri.isOnDemandConnection() || subscAppFilterMap.containsKey(mem.getDesc().getAppName())
                    || !Collections.disjoint(mem.getDesc().getTopicList(), subscTopicFilterMap.keySet())) {
                ri.validateOutboundConnectionCount(5);
            }

        }
    }

    private void startTcpConnectionMonitor() {
        LOG.info("JCLUSTER Health Checker Startup...");
        Thread.currentThread().setName("JC_TCP_CON_Health");
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

    protected int getFilterValuesCount(String app, String fName) {
        int totalFilterValueSize = memberMap.values().stream()
                .filter((mem) -> Objects.equals(mem.getDesc().getAppName(), app))
                .mapToInt((value) -> value.filterSetSize(fName))
                .sum();

        return totalFilterValueSize;
    }

    protected boolean containsFilterValue(String app, String fName, Object fValue) {
        return memberMap.values().stream()
                .anyMatch((mem) -> {
                    return Objects.equals(mem.getDesc().getAppName(), app) && mem.containsFilter(fName, fValue);
                });
    }

    protected List<JcRemoteInstanceConnectionBean> getMemConByApp(String app) {
        List<JcRemoteInstanceConnectionBean> riList = memberMap.values().stream()
                .filter((mem) -> Objects.equals(mem.getDesc().getAppName(), app))
                //                .filter((mem) -> mem.getConector().isOutboundAvailable())
                .map((mem) -> mem.getConector())
                .collect(Collectors.toList());

        return riList;
    }

    protected List<JcRemoteInstanceConnectionBean> getMemConByTopic(String topic) {
        List<JcRemoteInstanceConnectionBean> riList = memberMap.values().stream()
                //                .filter((mem) -> mem.getConector().isOutboundAvailable())
                .filter((mem) -> mem.getDesc().getTopicList().contains(topic))
                .map((mem) -> mem.getConector())
                .collect(Collectors.toList());

        return riList;

    }

    protected JcRemoteInstanceConnectionBean getMemConByFilter(Map<String, Object> fMap) {
        JcMember foundMem = memberMap.values().stream()
                .filter((mem) -> mem.containsFilter(fMap))
                .findFirst().orElseThrow(() -> new JcRuntimeException("No available instance found"));

        if (foundMem == null) {
            return null;
        }

        if (!foundMem.getConector().isOutboundAvailable()) {
            foundMem.getConector().setOnDemandConnection(true);
        }

        return foundMem.getConector();

    }

    protected JcRemoteInstanceConnectionBean getMemConByTopicAndFilter(String topic, Map<String, Object> fMap) {
        JcMember foundMem = memberMap.values().stream()
                .filter((mem) -> mem.getDesc().getTopicList().contains(topic))
                .filter((mem) -> mem.containsFilter(fMap))
                .findFirst().orElseThrow(() -> new JcRuntimeException("No available instance found"));

        if (foundMem == null) {
            return null;
        }

        if (!foundMem.getConector().isOutboundAvailable()) {
            foundMem.getConector().setOnDemandConnection(true);
        }

        return foundMem.getConector();

    }

    protected JcRemoteInstanceConnectionBean getMemConByTopicSingle(String topic) {
        JcMember foundMem = memberMap.values().stream()
                .filter((mem) -> mem.getDesc().getTopicList().contains(topic))
                .findFirst().orElseThrow(() -> new JcRuntimeException("No available instance found"));

        if (foundMem == null) {
            return null;
        }

        if (!foundMem.getConector().isOutboundAvailable()) {
            foundMem.getConector().setOnDemandConnection(true);
        }
        return foundMem.getConector();

    }

    protected JcRemoteInstanceConnectionBean getMemConByAppAndFilter(String app, Map<String, Object> fMap) {
        JcMember foundMem = memberMap.values().stream()
                .filter((mem)
                        -> Objects.equals(mem.getDesc().getAppName(), app)
                )
                .filter((mem) -> mem.containsFilter(fMap))
                .findFirst().orElseThrow(() -> new JcRuntimeException("No available instance found"));

        if (foundMem == null) {
            return null;
        }

        if (!foundMem.getConector().isOutboundAvailable()) {
            foundMem.getConector().setOnDemandConnection(true);
        }

        return foundMem.getConector();

    }

    protected JcRemoteInstanceConnectionBean getMemConByAppSingle(String app) {
        JcMember foundMem = memberMap.values().stream()
                .filter((mem) -> Objects.equals(mem.getDesc().getAppName(), app))
                .findFirst().orElseThrow(() -> new JcInstanceNotFoundException("No available instance found for app: [" + app + "]"));

        if (foundMem == null) {
            return null;
        }
        if (!foundMem.getConector().isOutboundAvailable()) {
            foundMem.getConector().setOnDemandConnection(true);
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

    public int getOutBoundMinConnection() {
        return outBoundMinConnection;
    }

    public boolean isEnterprise() {
        return enterprise;
    }

    public void setEnterprise(boolean enterprise) {
        this.enterprise = enterprise;
    }

    public void addMemberEventListener(MemberEventListener listener) {

        memberEventList.add(listener);
    }

    public Collection<FilterDescBean> getSelfFilterValues() {
        return selfFilterValueMap.values();
    }

    public Set<Object> getFilterValues(String fName) {
        FilterDescBean fd = selfFilterValueMap.get(fName);
        if (fd == null) {
            throw new JcRuntimeException("Filter does not exist: " + fName);
        }
        return fd.getValueSet();
    }

}
