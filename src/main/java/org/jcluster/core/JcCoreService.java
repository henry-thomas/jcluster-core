/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

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
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.enterprise.concurrent.ManagedExecutorService;
import org.jcluster.core.bean.FilterDescBean;
import org.jcluster.core.bean.JcMemFilterMetric;
import org.jcluster.core.bean.MemSimpleDesc;
import org.jcluster.core.exception.JcRuntimeException;
import org.jcluster.core.exception.cluster.JcInstanceNotFoundException;
import org.jcluster.core.messages.PublishMsg;
import org.jcluster.core.monitor.AppMetricMonitorInterface;
import org.jcluster.core.monitor.AppMetricsMonitor;
import org.jcluster.core.monitor.JcMetrics;
import org.jcluster.core.test.JcTestImpl;

/**
 *
 * @autor Henry Thomas
 */
public final class JcCoreService {

    private static final Logger LOG = (Logger) LoggerFactory.getLogger(JcCoreService.class);

    public static final int LISTEN_PORT_DEFAULT = 4445;

    private boolean enterprise = false;

    private int outBoundMinConnection;

    //to keep track on own filters set
    //key is filterName
    //value is FilterDescBean with set of values inside
    private final Map<String, FilterDescBean> selfFilterValueMap = new ConcurrentHashMap<>();

    private final Map<String, JcMember> memberMap = new ConcurrentHashMap<>();

    private final Map<MemSimpleDesc, String> primaryMemberMap = new HashMap<>();

    private JcMetrics metrics;

    protected final JcAppDescriptor selfDesc = new JcAppDescriptor();
    private long lastPrimaryMemUpdate = 0l;
    private long lastMemSubscriptionTimestamp = 0l;

    private static JcCoreService INSTANCE = new JcCoreService();
    private boolean running = false;

//    private ServerSocket serverSocket;
    public static final int UDP_FRAME_MAX_SIZE = 65535 - 28;
    public static final int UDP_FRAME_FRAGMENTATION_SIZE = UDP_FRAME_MAX_SIZE - 2048;

    private final byte[] buf = new byte[UDP_FRAME_MAX_SIZE];  //UDP frame 4byte, IP frame 16byte, 

    private volatile static int threadFactoryCounter = 100;

    //to keep track on response meessages
    protected final Map<String, JcDistMsg> requestMsgMap = new HashMap<>();

    //to keep track on where we need to subscribe, value is a list of filter names
    //key is topicName
    private final Map<String, Set<String>> subscTopicFilterMap = new HashMap<>();
    //to keep track on where we need to subscribe, value is a list of filter names
    //the key can be used for showing what app we need to connect to
    //key is appName
    private final Map<String, Set<String>> subscAppFilterMap = new HashMap<>();

    private final Set<MemSimpleDesc> toConnectList = new HashSet<>();

    private ExecutorService executorService = null;
    private ThreadFactory threadFactory = null;

    private JcServerEndpoint serverEndpoint;
    private Thread jcCoreMonitor = null;
    private static final Object CONTROL_LOCK = new Object();

    private String secret = null;

    //Buffer for all received messages
    private final Set<MemberEventListener> memberEventList = new HashSet<>();

    private KeyPair keyPair;

    private JcCoreService() {
        LOG.setLevel(ch.qos.logback.classic.Level.ALL);

        KeyPairGenerator generator;
        KeyPair p = null;
        try {
            generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            p = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            LOG.error(null, ex);
        }
        this.keyPair = p;
    }

    protected byte[] decryptData(byte encriptedData[]) {
        Cipher decryptCipher;
        try {
            decryptCipher = Cipher.getInstance("RSA");
            decryptCipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] doFinal = decryptCipher.doFinal(encriptedData);
            return doFinal;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            LOG.error(null, ex);
        }
        return null;
    }

    public final JcMetrics getAllMetrics() {
        return metrics;
    }

    public final void stop() throws Exception {
        if (running) {
            synchronized (CONTROL_LOCK) {

                LOG.error("JCLUSTER -- Shutdown... APPNAME: [{0}] InstanceID: [{1}]", new Object[]{selfDesc.getAppName(), selfDesc.getInstanceId()});
                running = false;
                memberMap.forEach((t, member) -> {
                    JcDistMsg msg = new JcDistMsg(JcDistMsgType.LEAVE);
                    msg.setSrcDesc(selfDesc);
                    msg.setData("Shutdown");
                    member.sendManagedMessage(msg);

                    onMemberRemove(member, "JC CORE Shutdown ");

                });

                if (serverEndpoint != null) {
                    serverEndpoint.destroy();
                }
                if (jcCoreMonitor != null) {
                    LOG.info("JCLUSTER Stoping monitor ...");
                    while (jcCoreMonitor.isAlive()) { //this thread has multiple wait in the code, make sure is not allive before start new instance of JC
                        jcCoreMonitor.interrupt();
                        Thread.sleep(200);
                    }
                    LOG.info("JCLUSTER Stoping monitor complete.");
                }
                LOG.info("============== JcCoreService Shutting down JCluster ==============");
            }
        } else {
            LOG.info("JCLUSTER -- Invalid Shutdown... APPNAME: [{}] InstanceID: [{}]", selfDesc.getAppName(), selfDesc.getInstanceId());
        }

    }

    public final void startWithArgs(String[] args) throws Exception {
//        appName=myPower24-lws selfIpAddress=192.168.100.15 udpListenPort=8381 tcpListenPort=2205 primaryMembers=192.168.100.15:8381
        start(JcManager.getDefaultConfig(args, false));
    }

    public final void start() throws Exception {
        start(null);
    }

    public final void start(Map<String, Object> config) throws Exception {
        if (config == null) {
            config = JcManager.getDefaultConfig(enterprise);
        }
        synchronized (CONTROL_LOCK) {

            if (!running) {
                if (config.containsKey("appName")) {
                    selfDesc.setAppName((String) config.get("appName"));
                }
                Object topics = config.get("topics");
                if (topics != null) {
                    selfDesc.getTopicList().addAll((Collection<String>) topics);
                }
                if (config.containsKey("selfIpAddress")) {
                    
                    selfDesc.setIpAddress((String) config.get("selfIpAddress"));
                    
                } else {
                    throw new JcRuntimeException("Missing property for [selfIpAddress]");
                }

                if (config.containsKey("secret")) {
                    this.secret = (String) config.get("secret");
                } else {
//                    this.secret = selfDesc.getInstanceId();
                }

                if (config.containsKey("secKeyPair")) {
                    this.keyPair = (KeyPair) config.get("KeyPair");
                }
                selfDesc.setPublicKey(this.keyPair.getPublic().getEncoded());

                LOG.info("JCLUSTER -- Startup... APPNAME: [{}] InstanceID: [{}]\n\tTopics: {}", selfDesc.getAppName(), selfDesc.getInstanceId(), selfDesc.getTopicList());

                ManagedExecutorService mes = (ManagedExecutorService) config.get("executorService");
                if (mes != null) {
                    executorService = mes;
                } else {
                    executorService = new ThreadPoolExecutor(10, 100, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
                }

                ManagedThreadFactory tf = (ManagedThreadFactory) config.get("threadFactory");
                if (tf == null) {
                    threadFactory = Executors.defaultThreadFactory();
                } else {
                    threadFactory = tf;
                }

                running = true;

                jcCoreMonitor = threadFactory.newThread(this::startTcpConnectionMonitor);
                jcCoreMonitor.setName("JC_Monitor");
                jcCoreMonitor.start();

                //TCP server 
                serverEndpoint = new JcServerEndpoint(threadFactory, (List<Integer>) config.get("tcpListenPort"));
                Thread serverThread = threadFactory.newThread(serverEndpoint);
                serverThread.setName("JC_TCP_SERVER");
                serverThread.start();

                metrics = new JcMetrics(selfDesc);

                JcManager.registerLocalClassImplementation(AppMetricsMonitor.class);
                JcManager.registerLocalClassImplementation(JcTestImpl.class);

                initPrimaryMembers(config);
            }
        }
    }

    public String getSecret() {
        return secret;
    }

    private MemSimpleDesc getPrimaryMemberByDesc(JcAppDescriptor jad) {
        return primaryMemberMap.keySet().stream()
                .filter((t) -> t.sameAs(jad))
                .findFirst()
                .orElse(null);
    }

    private void initPrimaryMembers(Map<String, Object> config) {
        List<String> memList = (List<String>) config.get("primaryMembers");
        if (memList != null) {
            for (String str : memList) {
                primaryMemberMap.put(MemSimpleDesc.createFromString(str), null);
            }
        }
    }

    protected JcMember getMember(String memId) {
        return memberMap.get(memId);
    }

    protected JcMember getMemberByMngConId(String mngConId) {
        return memberMap.values()
                .stream()
                .filter((t) -> t.getManagedConnection().getConnId().equals(mngConId))
                .findFirst()
                .orElse(null);
    }

    protected JcMember getMemberByPMD(MemSimpleDesc pmd) {
        return memberMap.values()
                .stream()
                .filter((t) -> pmd.sameAs(t.getDesc()))
                .findFirst()
                .orElse(null);
    }

    protected void onPingMsg(JcDistMsg msg) {
        JcAppDescriptor srcDesc = msg.getSrcDesc();
//        String srcId = srcDesc.getInstanceId();
        JcMember mem = memberMap.get(srcDesc.getInstanceId());
        if (mem == null) {
            try {
                JcAppDescriptor jad = msg.getSrcDesc();
                if (!jad.isIsolated()) {
                    synchronized (toConnectList) {
                        toConnectList.add(new MemSimpleDesc(jad.getIpAddress(), jad.getIpPort()));
                    }
//                    JcClientManagedConnection.createNew(jad.getIpAddress(), jad.getIpPort(), this::onNewManagedConnection);
                }
            } catch (Exception ex) {
                LOG.info(null, ex);
            }
        } else {
            mem.updateLastSeen();
//            LOG.warn("Receive ping message from: " + srcDesc.getInstanceId() + " App: " + mem.getDesc().getAppName());

        }
        try {
            List<String> ipStrList = (List<String>) msg.getData();

            if (!ipStrList.contains(selfDesc.getInstanceId())) {
                ipStrList.add(selfDesc.getInstanceId());
            }

            int ttl = msg.getTtl();
            for (Map.Entry<String, JcMember> entry : memberMap.entrySet()) {
                String memId = entry.getKey();
                JcMember m = entry.getValue();

                //forward to ones we don't have in our list
                if (!ipStrList.contains(memId)) {
                    if (!memId.equals(selfDesc.getInstanceId())) {
                        m.sendManagedMessage(msg);
                        msg.setTtl(ttl);
                    } else {
                        LOG.warn("Received ping from self.");
                    }
                }
            }

        } catch (Exception e) {
            LOG.info(null, e);
        }
    }

    private JcDistMsg generatePingMsg() {
        JcDistMsg msg = new JcDistMsg(JcDistMsgType.PING);
        List<String> memberIpStrList = new ArrayList<>();
        Set<String> keySet = memberMap.keySet();
        for (String ipStr : keySet) {
            memberIpStrList.add(ipStr);
        }
        memberIpStrList.add(selfDesc.getInstanceId());
        msg.setData(memberIpStrList);
        msg.setSrcDesc(selfDesc);
        return msg;

    }

    protected void onSubscRequestMsg(JcMember mem, JcDistMsg msg) {
        if (msg.getData() instanceof String) {
            String filterName = (String) msg.getData();
            FilterDescBean fd = selfFilterValueMap.get(filterName);
            if (fd == null) {
                fd = new FilterDescBean(filterName);
                selfFilterValueMap.put(filterName, fd);
            }

            JcDistMsg resp = new JcDistMsg(JcDistMsgType.SUBSCRIBE_RESP, msg.getMsgId(), msg.getTtl());
            resp.setSrcDesc(selfDesc);

            LOG.info("Receive subscription request from: {} filter: {}", mem.getId(), filterName);

            PublishMsg pm = new PublishMsg();
            pm.setOperationType(PublishMsg.OPER_TYPE_ADDBULK);
            pm.setFilterName(filterName);
            pm.setTransCount(fd.getTransCount());
            pm.getValueSet().addAll(fd.getValueSet());
            resp.setData(pm);

            mem.sendManagedMessage(resp);
            mem.getSubscribtionSet().add(filterName);
        }
    }

    protected void onMemberAdd(JcMember newMember) {
        String instanceId = newMember.getDesc().getInstanceId();
        JcMember oldMem = memberMap.get(instanceId);
        if (oldMem != null) {
            LOG.trace("Creating manged conneciton[{}] from same time concurently!", oldMem.getId());
        }
        memberMap.put(instanceId, newMember);

        MemSimpleDesc primaryMemberByDesc = getPrimaryMemberByDesc(newMember.getDesc());
        if (primaryMemberByDesc != null) {
            primaryMemberMap.put(primaryMemberByDesc, instanceId);
        }

        //Add from remote since we have him already
//        mem.close();
        RemMembFilter filter = newMember.getOrCreateFilterTarget(AppMetricMonitorInterface.JC_INSTANCE_FILTER);
        filter.addFilterValue(newMember.getDesc().getInstanceId());

        MemberEvent ev = new MemberEvent(newMember, MemberEvent.TYPE_ADD);
        synchronized (memberEventList) {
            memberEventList.forEach((e) -> {
                try {
                    e.onEvent(ev);
                } catch (Exception ex) {
                    LOG.error(null, ex);
                }
            });
        }
    }

    protected void onMemberRemove(JcMember mem, String reason) {
        memberMap.remove(mem.getId());
//        mem.close(reason);
        //remove member if has been subscribe to something
        selfFilterValueMap.entrySet().forEach(entry -> {
            entry.getValue().removeSubscirber(mem.getId());
        });

        RemMembFilter filter = mem.getOrCreateFilterTarget(AppMetricMonitorInterface.JC_INSTANCE_FILTER);
        filter.removeFilterValue(mem.getDesc().getInstanceId());

        MemberEvent ev = new MemberEvent(mem, MemberEvent.TYPE_REMOVE);
        synchronized (memberEventList) {
            memberEventList.forEach((e) -> {
                try {
                    e.onEvent(ev);
                } catch (Exception ex) {
                    LOG.error(null, ex);
                }
            });
        }

        JcMember replacementMember = JcClientManagedConnection.getMemberById(mem.getId());
        if (replacementMember != null) {
            onMemberAdd(replacementMember);
        }
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
            jcDistMsg.setSrcDesc(selfDesc);
            jcDistMsg.setData(filterName);
            LOG.info("Sending subscription request filter:[{}] to {}",
                    filterName, mem.getId());

            mem.sendManagedMessage(jcDistMsg);
            requestMsgMap.put(jcDistMsg.getMsgId(), jcDistMsg);
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

    private void checkPrimaryMemState() {

        for (Map.Entry<MemSimpleDesc, String> entry : primaryMemberMap.entrySet()) {
            MemSimpleDesc pmd = entry.getKey();
            String memId = entry.getValue();

            if (memId == null) {
                JcMember memberByPMD = getMemberByPMD(pmd);
                if (memberByPMD != null) {
                    primaryMemberMap.replace(pmd, memberByPMD.getId());
                } else if (!pmd.sameAs(selfDesc)) {
                    try {
                        synchronized (toConnectList) {
                            toConnectList.add(new MemSimpleDesc(pmd.getIp(), pmd.getPort()));
                        }

                    } catch (Exception ex) {
                        java.util.logging.Logger.getLogger(JcCoreService.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else if (!memberMap.containsKey(memId)) {
                primaryMemberMap.replace(pmd, null);
            }
        }

        synchronized (toConnectList) {
            for (MemSimpleDesc jad : toConnectList) {
                try {
                    JcClientManagedConnection.createNew(jad.getIp(), jad.getPort(), jad.getSecret());
                } catch (Exception ex) {
                    LOG.error(null, ex);
                }
            }
            toConnectList.clear();
        }
    }

    private void checkMemberState() {
//        LOG.trace("Check member state");

        JcDistMsg ping = generatePingMsg();
//        String strMembLog = "Cluster known members: \n\t";
        for (Iterator<Map.Entry<String, JcMember>> iterator = memberMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, JcMember> entry = iterator.next();

            JcMember mem = entry.getValue();

            mem.sendPing(ping);
// 
            if (mem.isOnDemandConnection() || subscAppFilterMap.containsKey(mem.getDesc().getAppName())
                    || !Collections.disjoint(mem.getDesc().getTopicList(), subscTopicFilterMap.keySet())) {
                mem.setOutboundRequired();
            }
            mem.validate();
        }
    }

    private void startTcpConnectionMonitor() {

        while (running) {
            long beginCycle = System.currentTimeMillis();
            //LOG.info("JCluster Health_Checker cycle");

            try {
                if (System.currentTimeMillis() - lastPrimaryMemUpdate > 5000) {
                    lastPrimaryMemUpdate = System.currentTimeMillis();
                    checkPrimaryMemState();

                    checkMemberState();
                }

                if (System.currentTimeMillis() - lastMemSubscriptionTimestamp > 5000) {
                    lastMemSubscriptionTimestamp = System.currentTimeMillis();
                    checkMemberSubscribtion();
                }

            } catch (Exception e) {
                LOG.error(null, e);
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
        msg.setSrcDesc(selfDesc);
        msg.setData(pm);
        subscribers.forEach((t) -> {
            t.sendManagedMessage(msg);
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

    protected List<JcMemFilterMetric> getMemFilterValuesCount(String app, String fName) {
        return memberMap.values()
                .stream()
                .filter(m -> m.getDesc().getAppName().equals(app))
                .map(m -> new JcMemFilterMetric(m, fName))
                .collect(Collectors.toList());
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

    protected List<JcMember> getMemConByApp(String app) {
        List<JcMember> riList = memberMap.values().stream()
                .filter((mem) -> Objects.equals(mem.getDesc().getAppName(), app))
                //                .filter((mem) -> mem.isOutboundAvailable())
                .collect(Collectors.toList());
        return riList;
    }

    protected List<JcMember> getMemConByTopic(String topic) {
        List<JcMember> riList = memberMap.values().stream()
                //                .filter((mem) -> mem.isOutboundAvailable())
                .filter((mem) -> mem.getDesc().getTopicList().contains(topic))
                .collect(Collectors.toList());

        return riList;

    }

    protected JcMember getMemConByFilter(Map<String, Object> fMap) {
        JcMember foundMem = memberMap.values().stream()
                .filter((mem) -> mem.containsFilter(fMap))
                .findFirst().orElseThrow(() -> new JcRuntimeException("No available instance found"));

        if (foundMem == null) {
            return null;
        }

        if (!foundMem.isOutboundAvailable()) {
            foundMem.setOnDemandConnection(true);
        }

        return foundMem;
    }

    protected List<JcMember> getMemConListByTopicAndFilter(String topic, Map<String, Object> fMap) {
        return memberMap.values().stream()
                .filter((mem) -> mem.getDesc().getTopicList().contains(topic))
                .filter((mem) -> mem.containsFilter(fMap))
                .collect(Collectors.toList());
    }

    protected JcMember getMemConByTopicAndFilter(String topic, Map<String, Object> fMap) {
        JcMember foundMem = memberMap.values().stream()
                .filter((mem) -> mem.getDesc().getTopicList().contains(topic))
                .filter((mem) -> mem.containsFilter(fMap))
                .findFirst().orElse(null);

        if (foundMem == null) {
            return null;
        }

        if (!foundMem.isOutboundAvailable()) {
            foundMem.setOnDemandConnection(true);
        }

        return foundMem;
    }

    protected JcMember getMemConByTopicSingle(String topic) {
        JcMember foundMem = memberMap.values().stream()
                .filter((mem) -> mem.getDesc().getTopicList().contains(topic))
                .findFirst().orElseThrow(() -> new JcRuntimeException("No available instance found"));

        if (foundMem == null) {
            return null;
        }

        if (!foundMem.isOutboundAvailable()) {
            foundMem.setOnDemandConnection(true);
        }
        return foundMem;
    }

    protected JcMember getMemConByAppAndFilter(String app, Map<String, Object> fMap) {
        JcMember foundMem = memberMap.values().stream()
                .filter((mem)
                        -> Objects.equals(mem.getDesc().getAppName(), app)
                )
                .filter((mem)
                        -> mem.containsFilter(fMap)
                )
                .findFirst().orElseThrow(() -> new JcRuntimeException("No available instance found"));

        if (foundMem == null) {
            return null;
        }

        if (!foundMem.isOutboundAvailable()) {
            foundMem.setOnDemandConnection(true);
        }

        return foundMem;
    }

    protected JcMember getMemConByAppSingle(String app) {
        JcMember foundMem = memberMap.values().stream()
                .filter((mem) -> Objects.equals(mem.getDesc().getAppName(), app))
                .findFirst().orElseThrow(() -> new JcInstanceNotFoundException("No available instance found for app: [" + app + "]"));

        if (foundMem == null) {
            return null;
        }
        if (!foundMem.isOutboundAvailable()) {
            foundMem.setOnDemandConnection(true);
        }
        return foundMem;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    public static JcAppDescriptor getSelfDesc() {
        return INSTANCE.selfDesc;
    }

//    public JcAppDescriptor getSelfDesc() {
//        return selfDesc;
//    }
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
        try {
            synchronized (memberEventList) {
                memberEventList.add(listener);
            }
        } catch (Exception e) {
            LOG.error(null, e);
        }
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

    protected void getTcpServerStat() {
        serverEndpoint.server.isBound();
    }

}
