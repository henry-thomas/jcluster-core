/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.cluster;

import com.hazelcast.map.IMap;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.naming.NamingException;
import org.jcluster.core.ServiceLookup;
import org.jcluster.core.bean.JcAppDescriptor;
import org.jcluster.core.bean.JcAppInstanceData;
import org.jcluster.core.exception.cluster.JcClusterNotFoundException;
import org.jcluster.core.exception.cluster.JcFilterNotFoundException;
import org.jcluster.core.exception.cluster.JcInstanceNotFoundException;
import org.jcluster.core.cluster.hzUtils.HzController;
import org.jcluster.core.config.JcAppConfig;
import org.jcluster.core.messages.JcMessage;
import org.jcluster.core.messages.JcMsgResponse;
import org.jcluster.core.proxy.JcProxyMethod;
import org.jcluster.core.sockets.JcClientConnection;
import org.jcluster.core.sockets.JcServerEndpoint;

/**
 *
 * @author henry
 *
 * Keeps record of all connected apps. Also has the logic for sending to the
 * correct app.
 */
public final class ClusterManager {

    private static final Logger LOG = Logger.getLogger(ClusterManager.class.getName());

    private IMap<String, JcAppDescriptor> appMap = null; //This map will be managed by Hazelcast

    private final Map<String, JcAppCluster> clusterMap = new HashMap<>();
//    private final Map<String, JcAppInstanceZ> clientMap = new HashMap<>();
    private final JcAppDescriptor thisDescriptor; //representst this app instance, configured at bootstrap

    private static final ClusterManager INSTANCE = new ClusterManager();
    private boolean running = false;
    private boolean configDone = false;
    private JcServerEndpoint server;
    private ManagedExecutorService executorService = null;

    private ClusterManager() {
        Integer port = JcAppConfig.getINSTANCE().getPort();
        String hostName = JcAppConfig.getINSTANCE().getHostName();
        String appName = JcAppConfig.getINSTANCE().getAppName();
        thisDescriptor = new JcAppDescriptor(hostName, port, appName);

        HzController hzController = HzController.getInstance();
        appMap = hzController.getMap();
        try {
            executorService = (ManagedExecutorService) ServiceLookup.getService("concurrent/__defaultManagedExecutorService");
            LOG.info("executorService found");
        } catch (NamingException ex) {
            Logger.getLogger(ClusterManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        LOG.log(Level.INFO, "ClusterManager: initConfig() HOSTNAME: {0} PORT: {1} APPNAME: {2}", new Object[]{hostName, port, appName});

    }

    protected static ClusterManager getInstance() {
        return INSTANCE;
    }

    public Map<String, HashSet<Object>> getFilterMap() {
        return thisDescriptor.getFilterMap();
    }

    public void addFilter(String filterName, Object value) {
        HashSet<Object> filterSet = thisDescriptor.getFilterMap().get(filterName);
        if (filterSet == null) {
            thisDescriptor.getFilterMap().put(filterName, new HashSet<>());
            filterSet = thisDescriptor.getFilterMap().get(filterName);
        }
        filterSet.add(value);
        //update distributed map
        appMap.put(thisDescriptor.getInstanceId(), thisDescriptor);
        LOG.log(Level.INFO, "Added filter: [{0}] with value: [{1}]", new Object[]{filterName, String.valueOf(value)});
    }

    public void remove(String filterName, Object value) {
        HashSet<Object> filterSet = thisDescriptor.getFilterMap().get(filterName);
        if (filterSet == null) {
            LOG.log(Level.INFO, "Filter does not exist: [{0}] with value: [{1}]", new Object[]{filterName, String.valueOf(value)});
            return;
        }
        filterSet.remove(value);
        //update distributed map
        appMap.put(thisDescriptor.getInstanceId(), thisDescriptor);
        LOG.log(Level.INFO, "Removed filter: [{0}] with value: [{1}]", new Object[]{filterName, String.valueOf(value)});
    }

    protected ClusterManager initConfig() {

        if (!running) {

            configDone = true;
            init();
        } else {
            LOG.log(Level.WARNING, "Cannot set JC App instance config, already running! instance ID: {0}", thisDescriptor.getInstanceId());
        }
        return INSTANCE;
    }

    private synchronized void init() {
        if (!running) {
            if (!configDone) {
                LOG.warning("JCLUSTER -- App instance is not configured, using default settings. Consider calling JcFactory.initManager(appName, ipAddress, port)");
            }
            LOG.info("JCLUSTER -- Startup...");
//            bindAddress = "tcp://" + thisDescriptor.getIpAddress() + ":" + thisDescriptor.getIpPort();
            server = new JcServerEndpoint();
            executorService.submit(server);

            //adding this app to the shared appMap, 
            //which is visible to all other apps in the Hazelcast Cluster
            appMap.put(thisDescriptor.getInstanceId(), thisDescriptor);

//            executorService.submit(this::initConnectionChecker);
            Thread t = new Thread(this::initConnectionChecker);
            t.start();
            running = true;
        }
    }

    public void initConnectionChecker() {
        Thread.currentThread().setName("J-CLUSTER_Health_Checker_Thread");
        while (running) {

            Map<String, JcClientConnection> ouboundConnections = JcAppInstanceData.getInstance().getOuboundConnections();
            synchronized (ouboundConnections) {

                for (Map.Entry<String, JcClientConnection> entry : ouboundConnections.entrySet()) {
                    JcClientConnection conn = entry.getValue();
                    long now = System.currentTimeMillis();
                    if (now - conn.getLastSuccessfulSend() < JcAppConfig.getINSTANCE().getJcLastSendMaxTimeout()) {
                        continue;
                    }

                    JcMessage req = new JcMessage("ping", null, null);
                    JcMsgResponse resp = null;
                    try {
                        resp = conn.send(req, 1000);
                    } catch (IOException ex) {
                        conn.destroy();
                    }
                    LOG.log(Level.INFO, "pinging to: {0} {1}:{2}", new Object[]{conn.getConnId()});
                    if (resp == null || resp.getData() == null || !resp.getData().equals("pong")) {
                        JcAppDescriptor desc = conn.getDesc();

                        JcAppInstanceData.getInstance().getOuboundConnections().remove(conn.getConnId());
                        JcAppInstanceData.getInstance().incrReconnectCount();
                        conn.destroy();
                        onMemberLeave(desc);
                        onNewMemberJoin(desc);

                        LOG.log(Level.INFO, "Reconnected to: {0} {1}:{2}", new Object[]{desc.getAppName(), desc.getIpAddress(), desc.getIpPort()});
                    }
                }

            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
                Logger.getLogger(ClusterManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        LOG.info("Shutting down J-CLUSTER_Health_Checker_Thread");
    }

    public void onNewMemberJoin(JcAppDescriptor appDesc) {

        for (Map.Entry<String, JcAppDescriptor> entry : appMap.entrySet()) {

            String id = entry.getKey();
            JcAppDescriptor desc = entry.getValue();

            if (desc == null || desc.getIpAddress() == null || Objects.equals(desc.getIpAddress(), "null")) {
                LOG.severe("NULL value in HZ distributed map!");
//                try {
//                    Thread.sleep(500);
//                } catch (InterruptedException ex) {
//                    Logger.getLogger(ClusterManager.class.getName()).log(Level.SEVERE, null, ex);
//                }
//                onNewMemberJoin(appDesc);
                continue;
            }

            String addr = "tcp://" + desc.getIpAddress() + ":" + desc.getIpPort();

            JcAppCluster cluster = clusterMap.get(desc.getAppName());
            if (cluster == null) {
                cluster = new JcAppCluster(desc.getAppName());
                clusterMap.put(desc.getAppName(), cluster);
            }

            if (cluster.getInstanceMap().containsKey(id)
                    || Objects.equals(desc.getInstanceId(), thisDescriptor.getInstanceId())
                    || Objects.equals(desc.getIpAddress() + desc.getIpPort(), thisDescriptor.getIpAddress() + thisDescriptor.getIpPort())) {
                continue;
            }

            LOG.log(Level.INFO, "Connecting to app {0} at: {1}", new Object[]{desc.getAppName(), addr});

            //Creating an outbound connection as soon as a new member joins.
            JcClientConnection jcClientConnection = new JcClientConnection(desc);
//            executorService.submit(jcClientConnection);
            Thread t = new Thread(jcClientConnection);
            t.start();

            JcAppInstanceData.getInstance().addOutboundConnection(jcClientConnection);
            cluster.addConnection(jcClientConnection);
            JcClientConnection putIfAbsent = cluster.getInstanceMap().putIfAbsent(id, jcClientConnection);
        }

    }

    public void onMemberLeave(JcAppDescriptor instance) {

        if (instance != null) {
            JcAppCluster cluster = clusterMap.get(instance.getAppName());

            if (cluster != null) {

                if (!cluster.removeConnection(instance)) {
                    LOG.log(Level.WARNING, "AppInstance not in cluster!: {0}", instance.getInstanceId());
                }

            } else {
                LOG.log(Level.WARNING, "Tried to remove instance from cluster that does not exist!");
            }

        }
    }

    public Object send(JcProxyMethod proxyMethod, Object[] args) throws JcFilterNotFoundException, JcInstanceNotFoundException {
        //Logic to send to correct app

        JcAppCluster cluster = clusterMap.get(proxyMethod.getAppName());

        if (cluster == null) {
            //ex   
            throw new JcClusterNotFoundException("Cluster not found for " + proxyMethod.getAppName());
        }

        try {

            if (proxyMethod.isInstanceFilter()) {
                Map<String, JcAppDescriptor> idDescMap = getIdDescMap(cluster);

                Map<String, Integer> paramNameIdxMap = proxyMethod.getParamNameIdxMap();

                String sendInstanceId = getSendInstance(idDescMap, paramNameIdxMap, args);

                if (sendInstanceId == null) {
                    //ex
                    throw new JcInstanceNotFoundException("Instance not found for [" + proxyMethod.getMethodSignature() + "] with params: [" + Arrays.toString(args) + "]");
                }

                return cluster.send(proxyMethod, args, sendInstanceId);
            } else if (proxyMethod.isBroadcast()) {

                return cluster.broadcast(proxyMethod, args);

            } else {

                return cluster.sendWithLoadBalancing(proxyMethod, args);

            }

        } catch (IOException ex) {

        }

        return null;
    }

    private Map<String, JcAppDescriptor> getIdDescMap(JcAppCluster cluster) {
        Map<String, JcClientConnection> instanceMap = cluster.getInstanceMap();
        Map<String, JcAppDescriptor> idDescMap = new HashMap<>();

        for (Map.Entry<String, JcClientConnection> entry : instanceMap.entrySet()) {
            String instanceId = entry.getKey();

            JcAppDescriptor desc = appMap.get(instanceId);

            idDescMap.put(instanceId, desc);
        }
        return idDescMap;
    }

    private String getSendInstance(Map<String, JcAppDescriptor> idDescMap, Map<String, Integer> paramNameIdxMap, Object[] args) throws JcFilterNotFoundException {
        for (Map.Entry<String, JcAppDescriptor> entry : idDescMap.entrySet()) {
            String descId = entry.getKey();
            JcAppDescriptor desc = entry.getValue();

            //Checking parameters of instanceDesc with args of method
            for (Map.Entry<String, Integer> entry1 : paramNameIdxMap.entrySet()) {
                String filterName = entry1.getKey();
                Integer idx = entry1.getValue();

                HashSet<Object> filterSet = desc.getFilterMap().get(filterName);
                if (filterSet != null) {

                    if (filterSet.contains(args[idx])) {
//                        LOG.log(Level.INFO, "FOUND WHO HAS [{0}] {1}", new Object[]{filterName, args[idx]});
                        return descId;
                    }

                } else {
                    throw new JcFilterNotFoundException("Filter does not exist in map... filterName: [" + filterName + "] Argument: [" + args[idx] + "]");
                }

            }
        }
        return null;
    }

    public JcAppDescriptor getThisDescriptor() {
        return thisDescriptor;
    }

    public void socketStopTest() {
        Map<String, JcClientConnection> ouboundConnections = JcAppInstanceData.getInstance().getOuboundConnections();
        for (Map.Entry<String, JcClientConnection> entry : ouboundConnections.entrySet()) {
            JcClientConnection conn = entry.getValue();
            conn.destroy();
        }
    }

    @PreDestroy
    public void destroy() {
        LOG.info("JCLUSTER -- Stopping...");
        for (Map.Entry<String, JcAppCluster> entry : clusterMap.entrySet()) {
            String key = entry.getKey();
            JcAppCluster cluster = entry.getValue();
            cluster.destroy();
        }
        appMap.remove(thisDescriptor.getInstanceId());
        server.destroy();
        running = false;
        HzController.getInstance().destroy();
    }

    public ManagedExecutorService getExecutorService() {
        return executorService;
    }

}
