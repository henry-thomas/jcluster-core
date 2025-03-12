/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedThreadFactory;
import static java.lang.System.currentTimeMillis;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import org.jcluster.core.bean.JcAppDescriptor;
import org.jcluster.core.bean.JcMemberEvent;
import org.jcluster.core.bean.JcMemerEventTypeEnum;
import org.jcluster.core.cluster.JcCoreService;
import org.jcluster.core.exception.cluster.JcClusterNotFoundException;
import org.jcluster.core.cluster.JcMember;
import org.jcluster.core.config.JcAppConfig;
import org.jcluster.core.exception.JcRuntimeException;
import org.jcluster.core.exception.cluster.JcIOException;
import org.jcluster.core.proxy.JcProxyMethod;

/**
 *
 * @autor Henry Thomas
 *
 * Keeps record of all connected apps.
 *
 * Also has the logic for sending to the correct app.
 *
 * Also checks connections to apps in different clusters.
 *
 */
public class JcManager {

    private static final Logger LOG = Logger.getLogger(JcManager.class.getName());

    protected final Queue<JcMemberEvent> memberEventQueue = new ArrayDeque<>(512);
//    private final JcInstance thisAppInstance = new JcInstance(); //representst this app instance, configured at bootstrap
    private final JcAppDescriptor selfDesc = new JcAppDescriptor();

    //this maps hold object for each remote instance
    private final Map<String, JcRemoteInstanceConnectionBean> remoteInstanceMap = new ConcurrentHashMap<>(); //
//    private final JcMap<String, List<JcConnectionMetrics>> jcMetricsMap; //

    //List that contains the instanceId's of remote instances.
    private static final JcManager INSTANCE = new JcManager();
    private static boolean running = false;
    private JcServerEndpoint serverEndpoint;
    private final ManagedExecutorService executorService;
    private final ManagedThreadFactory threadFactory;

    private long lastPingTimestamp = 0l;
//    private final Set<String> appNameList;
//    private final Set<String> topicList = new ;
//    private final InstanceResMonitorBean monitorBean;
//    private final List<String> onlineInstanceIdList = new ArrayList<>();

    private JcManager() {
//        hzController = DiscoveryService.getInstance();
//        jcMetricsMap = hzController.getMap("jcMetricsMap");
        ManagedExecutorService exs = null;
        ManagedThreadFactory th = null;

        try {
            exs = (ManagedExecutorService) ServiceLookup.getService("concurrent/__defaultManagedExecutorService");
            th = (ManagedThreadFactory) ServiceLookup.getService("concurrent/__defaultManagedThreadFactory");
        } catch (NamingException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        executorService = exs;
        threadFactory = th;

//        appNameList = JcBootstrap.appNameList;
//        selfDesc.getOutboundAppNameList().addAll(appNameList); //TODO
//        topicList = JcBootstrap.topicNameList;
    }

    protected static JcManager getInstance() {
        return INSTANCE;
    }

    public void addFilter(String filterName, Object value) {
        JcCoreService.getInstance().addSelfFilterValue(filterName, value);
//        Map<String, HashSet<Object>> filterMap = selfDesc.getFilterMap();
//
//        HashSet<Object> filterSet = filterMap.get(filterName);
//        if (filterSet == null) {
//            filterSet = new HashSet<>();
//            filterMap.put(filterName, filterSet);
//        }
//
//        filterSet.add(value);

//        updateThisHzDescriptor();
//        LOG.log(Level.INFO, "Added filter: [{0}] with value: [{1}]", new Object[]{filterName, String.valueOf(value)});
    }

    public void removeFilter(String filterName, Object value) {
        JcCoreService.getInstance().addSelfFilterValue(filterName, value);
//        HashSet<Object> filterSet = selfDesc.getFilterMap().get(filterName);
//        if (filterSet == null) {
//            LOG.log(Level.INFO, "Filter does not exist: [{0}] with value: [{1}]", new Object[]{filterName, String.valueOf(value)});
//            return;
//        } else {
//            filterSet.remove(value);
//        }

//        updateThisHzDescriptor();
//        LOG.log(Level.INFO, "Removed filter: [{0}] with value: [{1}]", new Object[]{filterName, String.valueOf(value)});
    }

    private void proccessNewEvent(JcMemberEvent ev) {
        JcAppDescriptor remDesc = ev.getAppDescriptor();
        if (ev.getEventType() == JcMemerEventTypeEnum.MEMBER_ADD) {
            //precess member of appNames that are required only

            //TODO
//            if (appNameList.contains(ev.getAppDescriptor().getAppName()) && !ev.getAppDescriptor().isIsolated()) {
//                if (!remoteInstanceMap.containsKey(remDesc.getInstanceId())) {
//                    JcRemoteInstanceConnectionBean ric = new JcRemoteInstanceConnectionBean(remDesc);
//                    remoteInstanceMap.put(ev.getAppDescriptor().getInstanceId(), ric);
//                }
//            }
        } else if (ev.getEventType() == JcMemerEventTypeEnum.MEMBER_REMOVE) {
            JcRemoteInstanceConnectionBean remove = remoteInstanceMap.remove(ev.getAppDescriptor().getInstanceId());
            if (remove != null) {
                remove.destroy();
            }
        }
    }

    private void jcMainThreadExecution() {
        LOG.info("JCLUSTER Health Checker Startup...");
        Thread.currentThread().setName("J-CLUSTER_Health_Checker_Thread");
        while (running) {
            long beginCycle = System.currentTimeMillis();
            //LOG.info("JCluster Health_Checker cycle");

            try {
                //execute event in current thread to avoid synchronization in the map
                synchronized (memberEventQueue) {
                    JcMemberEvent ev;
                    while ((ev = memberEventQueue.poll()) != null) {
                        proccessNewEvent(ev);
                    }
                }
            } catch (Exception e) {
            }

            try {
                mainLoop();
            } catch (Exception e) {
                LOG.log(Level.INFO, "JCluster exception in main loop", e);
            }
            if (System.currentTimeMillis() - beginCycle < 500) {
                try {
                    synchronized (JcManager.this) {
                        JcManager.this.wait(500 - (System.currentTimeMillis() - beginCycle));
                    }
                } catch (InterruptedException ex) {
                }
            }
        }
        LOG.info("Shutting down J-CLUSTER_Health_Checker_Thread");
    }

    protected synchronized JcManager startManager() {
        if (!running) {
            LOG.log(Level.INFO, "JCLUSTER --- STARTUP. AppName: {0} InstanceId: {1}", new Object[]{selfDesc.getAppName(), selfDesc.getInstanceId()});
            serverEndpoint = new JcServerEndpoint(threadFactory);
            Thread serverThread = threadFactory.newThread(serverEndpoint);
            serverThread.setName("JcServerEndpoint");
            serverThread.start();

            //adding this app to the shared appMap, 
            //which is visible to all other apps in the Hazelcast Cluster
//            updateThisHzDescriptor();
            running = true;

            //Adding a filter to this instance with its own id
//            addFilter(JC_INSTANCE_FILTER, instanceDesc.getServerName());
        }
        return INSTANCE;
    }

    public void onNewMemberEvent(JcMemberEvent event) {
        //do not connect to yourself 
        if (event.getAppDescriptor() == null || event.getAppDescriptor().getInstanceId().equals(selfDesc.getInstanceId())) {
            return;
        }
        synchronized (memberEventQueue) {
            memberEventQueue.add(event);
        }
        synchronized (this) {
            this.notifyAll();
        }
    }

    protected JcRemoteInstanceConnectionBean addRemoteInstanceConnection(JcAppDescriptor desc) {
        return null;
        //Here!
//TODO
//        boolean outboundEnabled = appNameList.contains(desc.getAppName()) || !Collections.disjoint(topicList, desc.getTopicList());

//        JcRemoteInstanceConnectionBean jcRemoteInstanceConnectionBean = new JcRemoteInstanceConnectionBean(desc, outboundEnabled);
//
//        JcRemoteInstanceConnectionBean ric = remoteInstanceMap.put(desc.getInstanceId(), jcRemoteInstanceConnectionBean);
//
//        if (ric != null) {
//            ric.destroy();
//        }
//
//        return jcRemoteInstanceConnectionBean;
    }

    private void mainLoop() {

        //ping interval
        if (currentTimeMillis() - lastPingTimestamp >= 1500) {
            lastPingTimestamp = currentTimeMillis();
            for (Map.Entry<String, JcRemoteInstanceConnectionBean> entry : remoteInstanceMap.entrySet()) {
                entry.getValue().pingAllOutbound();
            }
        }

        //validate all instances have no stuck connections
        for (Map.Entry<String, JcRemoteInstanceConnectionBean> entry : remoteInstanceMap.entrySet()) {
            entry.getValue().validateTimeoutsAllConn();
        }

        //validate all instances have correct amount of outbound connections
        for (Map.Entry<String, JcRemoteInstanceConnectionBean> entry : remoteInstanceMap.entrySet()) {
            JcRemoteInstanceConnectionBean ri = entry.getValue();

            //we have to check the app name because there is another apps that makes only inboudn connections
            if (appNameList.contains(ri.getAppName())
                    || !Collections.disjoint(ri.getRemoteAppDesc().getTopicList(), topicList)) {
                ri.validateOutboundConnectionCount(JcAppConfig.getINSTANCE().getMinConnections());
            }

        }

        //Create INBOUND incase this instance is isolated
        if (selfDesc.isIsolated()) {
            for (Map.Entry<String, JcRemoteInstanceConnectionBean> entry : remoteInstanceMap.entrySet()) {
                JcRemoteInstanceConnectionBean ri = entry.getValue();

                //we have to check the app name because there is another apps that makes only inboudn connections
                if (ri.getRemoteAppDesc().getOutboundAppNameList().contains(selfDesc.getAppName())
                        || !Collections.disjoint(ri.getRemoteAppDesc().getTopicList(), topicList)) {

                    ri.validateInboundConnectionCount(ri.getRemoteAppDesc().getOutBoundMinConnection());
                }

            }
        }

    }

    protected JcRemoteInstanceConnectionBean getRemoteInstance(String instanceId) {
        JcRemoteInstanceConnectionBean ric = remoteInstanceMap.get(instanceId);
        return ric;
    }

    private int broadcastSend(JcProxyMethod proxyMethod, Object[] args) {
        int instanceBroadcastedTo = 0;
        for (Map.Entry<String, JcRemoteInstanceConnectionBean> entry : remoteInstanceMap.entrySet()) {
            JcRemoteInstanceConnectionBean ri = entry.getValue();

            if (proxyMethod.isTopic()) {
                if (!ri.getRemoteAppDesc().getTopicList().contains(proxyMethod.getTopicName())) {
                    continue;
                }
            } else if (!ri.getAppName().equals(proxyMethod.getAppName())) {
                continue;
            }

            try {
                ri.send(proxyMethod, args);
                instanceBroadcastedTo++;
            } catch (Exception e) {
            }
        }
        return instanceBroadcastedTo;
    }

    private Object filteredSend(JcProxyMethod proxyMethod, Object[] args) throws JcIOException {
        //find all instances by filter
        int successs = 0;
        instanceLoop:
        for (Map.Entry<String, JcMember> entry : jcMemberMap.entrySet()) {
            String remInstanceId = entry.getKey();
            JcAppDescriptor rid = entry.getValue().getDesc();
            //first filter by appname to avoid unnecessary filter matching
            if (!proxyMethod.isGlobal()) {
                if (proxyMethod.isTopic()) {
                    if (!rid.getTopicList().contains(proxyMethod.getTopicName())) {
                        continue;
                    }
                } else if (!rid.getAppName().equals(proxyMethod.getAppName())) {
                    continue;
                }
            }
            //skip ourselvse since this instance exist in Hz map
            if (rid.getInstanceId().equals(selfDesc.getInstanceId())) {
                continue;
            }

            //try to match filters
            Map<String, Integer> paramNameIdxMap = proxyMethod.getParamNameIdxMap();
            for (Map.Entry<String, Integer> entry1 : paramNameIdxMap.entrySet()) {
                String filterKey = entry1.getKey();
                Object filterVal = args[entry1.getValue()];

//                HashSet<Object> valueSet = rid.getFilterMap().get(filterKey);
//                //check if instance has filter key first otherwise go to next instance
//                if (valueSet == null || valueSet.isEmpty()) {
//                    continue instanceLoop;
//                }
//
//                //check if instance has filter value first otherwise go to next instance
//                if (valueSet.contains(filterVal)) {
//                    JcRemoteInstanceConnectionBean ri = remoteInstanceMap.get(remInstanceId);
//                    if (ri == null) {
//                        throw new JcIOException("Instance  [" + proxyMethod.getAppName() + "." + remInstanceId + "] not connected.");
//                    }
//                    if (proxyMethod.isBroadcast()) {
//                        ri.send(proxyMethod, args);
//                        successs++;
//                    } else {
//                        return ri.send(proxyMethod, args);
//                    }
//                }
            }
        }

        if (successs == 0) {
            throw new JcIOException(
                    "No Instance found App: " + proxyMethod.getAppName() + "  " + proxyMethod.printFilters(args));
        }
        return successs;
    }

    public Object send(JcProxyMethod proxyMethod, Object[] args) throws JcRuntimeException {

        if (proxyMethod.isInstanceFilter()) {//no app name needed if send is specific for remote instance
            return filteredSend(proxyMethod, args);
        } else if (proxyMethod.isBroadcast()) {
            int broadcastSend = broadcastSend(proxyMethod, args);
            if (broadcastSend == 0) {
                throw new JcClusterNotFoundException("No cluster instance available for Broadcast@: " + proxyMethod.getAppName());
            }
            return broadcastSend;
        } else {
            for (Map.Entry<String, JcRemoteInstanceConnectionBean> entry : remoteInstanceMap.entrySet()) {
                JcRemoteInstanceConnectionBean ri = entry.getValue();
                //check if this is inside same APP and has output connections
                if (proxyMethod.isTopic()) {
                    if (!ri.getRemoteAppDesc().getTopicList().contains(proxyMethod.getTopicName())) {
                        continue;
                    }
                } else if (!ri.getAppName().equals(proxyMethod.getAppName())) {
                    continue;
                }

                if (ri.isOutboundAvailable()) {
                    try {
                        return ri.send(proxyMethod, args);
                    } catch (JcIOException e) {
                        throw new JcIOException(e.getMessage());
                    }
                }
            }
            throw new JcClusterNotFoundException("No cluster instance available for: " + proxyMethod.getAppName());
        }
    }

    public JcAppDescriptor getInstanceAppDesc() {
        return selfDesc;
    }

    public int closeAllInstanceConnections(String instanceId) {

        int totalClosed = 0;
        for (Map.Entry<String, JcRemoteInstanceConnectionBean> entry : remoteInstanceMap.entrySet()) {
            JcRemoteInstanceConnectionBean ri = entry.getValue();
            totalClosed += ri.removeAllConnection();
        }
        return totalClosed;
    }

    public ManagedExecutorService getExecutorService() {
        return executorService;
    }

    public ManagedThreadFactory getThreadFactory() {
        return threadFactory;
    }

    public HashSet<String> getRemoteAppNameList() {
        HashSet<String> remoteAppNameList = new HashSet<>();
        for (Map.Entry<String, JcMember> entry : jcMemberMap.entrySet()) {
            String key = entry.getKey();
            JcAppDescriptor desc = entry.getValue().getDesc();

            if (!selfDesc.getInstanceId().equals(key)) {
//                remoteAppNameList.add(desc.getServerName());

            }
        }

//        for (Map.Entry<String, JcRemoteInstanceConnectionBean> entry : remoteInstanceMap.entrySet()) {
//            String id = entry.getKey();
//            JcAppDescriptor desc = entry.getValue().getRemoteAppDesc();
//            
//        }
        return remoteAppNameList;
    }

    public void destroy() {
        for (Map.Entry<String, JcRemoteInstanceConnectionBean> entry : remoteInstanceMap.entrySet()) {
            entry.getValue().destroy();
        }
        remoteInstanceMap.clear();
        appNameList.clear();

        //remove self from hzCast
        jcMemberMap.remove(selfDesc.getInstanceId());
        if (serverEndpoint != null) {
            serverEndpoint.destroy();
        }
        running = false;

        for (Map.Entry<String, JcRemoteInstanceConnectionBean> entry : remoteInstanceMap.entrySet()) {
            JcRemoteInstanceConnectionBean ri = entry.getValue();

//            String metricsId = instanceDesc.getServerName() + "->" + ri.getRemoteAppDesc().getServerName();
//            jcMetricsMap.delete(metricsId);
        }

//        DiscoveryService.getInstance().destroy();
    }

//    public Map<String, JcAppDescriptor> getHzAppDescMap() {
//        return jcMemberMap;
//    }
}
