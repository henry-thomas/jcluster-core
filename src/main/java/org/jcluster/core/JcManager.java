/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

import com.hazelcast.map.IMap;
import static java.lang.System.currentTimeMillis;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.naming.NamingException;
import org.jcluster.core.bean.JcAppDescriptor;
import org.jcluster.core.bean.JcConnectionMetrics;
import org.jcluster.core.bean.JcMeberEvent;
import org.jcluster.core.bean.JcMemerEventTypeEnum;
import org.jcluster.core.exception.cluster.JcClusterNotFoundException;
import org.jcluster.core.hzUtils.HzController;
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

    private IMap<String, JcAppDescriptor> hzAppDescMap = null; //This map will be managed by Hazelcast
    protected final Queue<JcMeberEvent> memberEventQueue = new ArrayDeque<>(512);
//    private final JcInstance thisAppInstance = new JcInstance(); //representst this app instance, configured at bootstrap
    private final JcAppDescriptor instanceDesc = new JcAppDescriptor();

    //this maps hold object for each remote instance
    private final Map<String, JcRemoteInstanceConnectionBean> remoteInstanceMap = new ConcurrentHashMap<>(); //

    private static final JcManager INSTANCE = new JcManager();
    private static boolean running = false;
    private JcServerEndpoint serverEndpoint;
    private final ManagedExecutorService executorService;
    private final ManagedThreadFactory threadFactory;

    private long lastUpdateHzDesc = 0l;
    private long lastPingTimestamp = 0l;
    private final Set<String> appNameList;
//    private final List<String> onlineInstanceIdList = new ArrayList<>();

    private JcManager() {
        HzController hzController = HzController.getInstance();
        hzAppDescMap = hzController.getMap();
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

        appNameList = JcBootstrap.appNameList;
//        LOG.log(Level.INFO, "ClusterManager: initConfig() {0}", instanceDesc);
    }

    protected static JcManager getInstance() {
        return INSTANCE;
    }

    private void updateThisHzDescriptor() {
        instanceDesc.updateTimestamp();
        hzAppDescMap.put(instanceDesc.getInstanceId(), instanceDesc);
    }

    public HashMap<String, List<JcConnectionMetrics>> getAllMetrics() {
        HashMap<String, List<JcConnectionMetrics>> metricsMap = new HashMap<>();
        for (Map.Entry<String, JcRemoteInstanceConnectionBean> entry : remoteInstanceMap.entrySet()) {
            metricsMap.put(entry.getKey(), entry.getValue().getAllMetrics());
        }
        return metricsMap;
    }

    public void addFilter(String filterName, Object value) {
        Map<String, HashSet<Object>> filterMap = instanceDesc.getFilterMap();

        HashSet<Object> filterSet = filterMap.get(filterName);
        if (filterSet == null) {
            filterSet = new HashSet<>();
            filterMap.put(filterName, filterSet);
        }

        filterSet.add(value);

        updateThisHzDescriptor();
        LOG.log(Level.INFO, "Added filter: [{0}] with value: [{1}]", new Object[]{filterName, String.valueOf(value)});
    }

    public void removeFilter(String filterName, Object value) {
        HashSet<Object> filterSet = instanceDesc.getFilterMap().get(filterName);
        if (filterSet == null) {
            LOG.log(Level.INFO, "Filter does not exist: [{0}] with value: [{1}]", new Object[]{filterName, String.valueOf(value)});
            return;
        } else {
            filterSet.remove(value);
        }

        updateThisHzDescriptor();
        LOG.log(Level.INFO, "Removed filter: [{0}] with value: [{1}]", new Object[]{filterName, String.valueOf(value)});

    }

    private void proccessNewEvent(JcMeberEvent ev) {
        JcAppDescriptor remDesc = ev.getAppDescriptor();
        if (ev.getEventType() == JcMemerEventTypeEnum.MEMBER_ADD) {
            //precess member of appNames that are rquired only
            if (appNameList.contains(ev.getAppDescriptor().getAppName())) {
                if (!remoteInstanceMap.containsKey(remDesc.getInstanceId())) {
                    JcRemoteInstanceConnectionBean ric = new JcRemoteInstanceConnectionBean(remDesc);
                    remoteInstanceMap.put(ev.getAppDescriptor().getInstanceId(), ric);
                }
            }
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
                    JcMeberEvent ev;
                    while ((ev = memberEventQueue.poll()) != null) {
                        proccessNewEvent(ev);
                    }
                }
            } catch (Exception e) {
            }

            try {
                mainLoop();
            } catch (Exception e) {
                LOG.log(Level.INFO, null, e);
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
            LOG.log(Level.INFO, "JCLUSTER --- STARTUP. AppName: {0} InstanceId: {1}", new Object[]{instanceDesc.getAppName(), instanceDesc.getInstanceId()});
            serverEndpoint = new JcServerEndpoint(threadFactory);
            Thread serverThread = threadFactory.newThread(serverEndpoint);
            serverThread.setName("JcServerEndpoint");
            serverThread.start();

            //adding this app to the shared appMap, 
            //which is visible to all other apps in the Hazelcast Cluster
            updateThisHzDescriptor();

            running = true;
            Thread jcManagerThread = threadFactory.newThread(this::jcMainThreadExecution);
            jcManagerThread.start();
        }
        return INSTANCE;
    }

    protected void onNewMemberEvent(JcMeberEvent event) {
        //do not connect to yourself 
        if (event.getAppDescriptor() == null || event.getAppDescriptor().getInstanceId().equals(instanceDesc.getInstanceId())) {
            return;
        }
        synchronized (memberEventQueue) {
            memberEventQueue.add(event);
        }
        synchronized (this) {
            this.notifyAll();
        }
    }

    private void checkHzInstanceForTimeouts() {
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, JcAppDescriptor> entry : hzAppDescMap.entrySet()) {
            String instId = entry.getKey();
            JcAppDescriptor desc = entry.getValue();
            //skip ourselvse
            if (!desc.getInstanceId().equals(instanceDesc.getInstanceId())) {
                if (currentTimeMillis() - desc.getLastAlive() > 1000 * 60 * 2) {
                    toRemove.add(instId);
                    JcRemoteInstanceConnectionBean ri = remoteInstanceMap.remove(instId);
                    if (ri != null) {
                        ri.destroy();
                    }
                }
            }
        }

        for (String instId : toRemove) {
            LOG.log(Level.INFO, "Removing old instance from HZ cluster map with instanceId: {0}", instId);
            hzAppDescMap.remove(instId);
        }
    }

    protected JcRemoteInstanceConnectionBean addRemoteInstanceConnection(JcAppDescriptor desc) {
        boolean outboundEnabled = appNameList.contains(desc.getAppName());
        JcRemoteInstanceConnectionBean ric = remoteInstanceMap.put(desc.getInstanceId(), new JcRemoteInstanceConnectionBean(desc, outboundEnabled));
        return ric;
    }

    private void resynchronizeHzMap() {
        for (Map.Entry<String, JcAppDescriptor> entry : hzAppDescMap.entrySet()) {
            String instId = entry.getKey();
            JcAppDescriptor desc = entry.getValue();
            if (!instId.equals(instanceDesc.getInstanceId()) && !remoteInstanceMap.containsKey(instId)) {
                if (appNameList.contains(desc.getAppName())) {
                    LOG.info("Synchronization between JcManager remote instance map and Hz map found new Instance: " + desc);
                    remoteInstanceMap.put(instId, new JcRemoteInstanceConnectionBean(desc));
                }
            }
        }
        //check if hz map has less instances, this can happen if we missed remove event somehow.

        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, JcRemoteInstanceConnectionBean> entry : remoteInstanceMap.entrySet()) {
            String instId = entry.getKey();
            if (!hzAppDescMap.containsKey(instId)) {
                toRemove.add(instId);
                entry.getValue().destroy();
            }
        }
        for (String instId : toRemove) {
            LOG.log(Level.INFO, "Removing old instance from HZ cluster map with instanceId: {0}", instId);
            remoteInstanceMap.remove(instId);
        }
    }

    private void mainLoop() {
        if (currentTimeMillis() - lastUpdateHzDesc >= 30 * 1000) {
            lastUpdateHzDesc = currentTimeMillis();
            updateThisHzDescriptor();

            checkHzInstanceForTimeouts();
            resynchronizeHzMap();
        }

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
            if (appNameList.contains(ri.getAppName())) {
                ri.validateOutboundConnectionCount(JcAppConfig.getINSTANCE().getMinConnections());
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
            if (ri.getAppName().equals(proxyMethod.getAppName())) {
                try {
                    ri.send(proxyMethod, args);
                    instanceBroadcastedTo++;
                } catch (Exception e) {
                }
            }
        }
        return instanceBroadcastedTo;
    }

    private Object filteredSend(JcProxyMethod proxyMethod, Object[] args) throws JcIOException {
        //find all instances by filter
        int successs = 0;
        instanceLoop:
        for (Map.Entry<String, JcAppDescriptor> entry : hzAppDescMap.entrySet()) {
            String remInstanceId = entry.getKey();
            JcAppDescriptor rid = entry.getValue();
            //first filter by appname to avoid unnecessary filter matching
            if (!rid.getAppName().equals(proxyMethod.getAppName())) {
                continue;
            }
            //skip ourselvse since this instance exist in Hz map
            if (rid.getInstanceId().equals(instanceDesc.getInstanceId())) {
                continue;
            }

            //try to match filters
            Map<String, Integer> paramNameIdxMap = proxyMethod.getParamNameIdxMap();
            for (Map.Entry<String, Integer> entry1 : paramNameIdxMap.entrySet()) {
                String filterKey = entry1.getKey();
                Object filterVal = args[entry1.getValue()];

                HashSet<Object> valueSet = rid.getFilterMap().get(filterKey);
                //check if instance has filter key first otherwise go to next instance
                if (valueSet == null || valueSet.isEmpty()) {
                    continue instanceLoop;
                }

                //check if instance has filter value first otherwise go to next instance
                if (valueSet.contains(filterVal)) {
                    JcRemoteInstanceConnectionBean ri = remoteInstanceMap.get(remInstanceId);
                    if (ri == null) {
                        throw new JcIOException("Instance  [" + proxyMethod.getAppName() + "." + remInstanceId + "] not connected.");
                    }
                    if (proxyMethod.isBroadcast()) {
                        ri.send(proxyMethod, args);
                        successs++;
                    } else {
                        return ri.send(proxyMethod, args);
                    }
                }
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
                if (ri.getAppName().equals(proxyMethod.getAppName()) && ri.isOutboundAvailable()) {
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
        return instanceDesc;
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

    public void destroy() {
        for (Map.Entry<String, JcRemoteInstanceConnectionBean> entry : remoteInstanceMap.entrySet()) {
            entry.getValue().destroy();
        }
        remoteInstanceMap.clear();
        appNameList.clear();

        //remove self from hzCast
        hzAppDescMap.remove(instanceDesc.getInstanceId());
        if (serverEndpoint != null) {
            serverEndpoint.destroy();
        }
        running = false;
        HzController.getInstance().destroy();
    }

}
