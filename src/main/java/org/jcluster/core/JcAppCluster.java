package org.jcluster.core;

///*
// * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
// * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
// */
//package org.jcluster.core.cluster;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import org.jcluster.core.bean.JcAppInstanceDescriptor;
//import org.jcluster.core.bean.JcAppInstance;
//import org.jcluster.core.cluster.hzUtils.HzController;
//import org.jcluster.core.exception.cluster.JcClusterNotFoundException;
//import org.jcluster.core.exception.cluster.JcFilterNotFoundException;
//import org.jcluster.core.exception.cluster.JcInstanceNotFoundException;
//import org.jcluster.core.messages.JcMessage;
//import org.jcluster.core.proxy.JcProxyMethod;
//
///**
// *
// * @autor Henry Thomas
// */
//public class JcAppCluster {
//
//    private static final Logger LOG = Logger.getLogger(JcAppCluster.class.getName());
//
//    private final String jcAppName;
//    private final Map<String, JcAppInstance> appInstanceMap = new HashMap<>(); //connections for this app
//
////    private final Map<String, JcClientConnection> instanceMap = new HashMap<>(); //connections for this app
//    private int lastSendAppIndex = 0;
////    private final Map<Integer, JcMessage> jcMsgMap = new ConcurrentHashMap<>();
//
//    public JcAppCluster(String jcAppName) {
//        this.jcAppName = jcAppName;
//    }
//
//    public Map<String, JcAppInstance> getAppInstanceMap() {
//        return appInstanceMap;
//    }
//
//    public boolean removeConnection(JcAppInstanceDescriptor instance) {
//
//        JcClientConnection instanceConnection = instanceMap.get(instance.getInstanceId());
//        if (instanceConnection != null) {
//            instanceConnection.destroy();
//        }
//
//        JcClientConnection remove = instanceMap.remove(instance.getInstanceId());
//        return remove != null;
//    }
//
//    private Map<String, JcAppInstanceDescriptor> getIdDescMap(JcAppCluster cluster) {
//        Map<String, JcAppInstanceDescriptor> idDescMap = new HashMap<>();
//
//        for (Map.Entry<String, JcClientConnection> entry : instanceMap.entrySet()) {
//            String instanceId = entry.getKey();
//
//            JcAppInstanceDescriptor desc = HzController.getInstance().getMap().get(instanceId);
//
//            idDescMap.put(instanceId, desc);
//        }
//        return idDescMap;
//    }
//
//    public Object send(JcProxyMethod proxyMethod, Object[] args) throws JcFilterNotFoundException, JcInstanceNotFoundException {
//        //Logic to send to correct app
//        try {
//            if (proxyMethod.isInstanceFilter()) {
//                //find all instances by filter
//                Map<String, JcAppInstanceDescriptor> idDescMap = new HashMap<>();
//                for (Map.Entry<String, JcClientConnection> entry : instanceMap.entrySet()) {
//                    String instanceId = entry.getKey();
//
//                    JcAppInstanceDescriptor desc = HzController.getInstance().getMap().get(instanceId);
//
//                    idDescMap.put(instanceId, desc);
//                }
//
//                LOG.log(Level.INFO, "Sending to single instance: {0}", proxyMethod.getMethodSignature());
//
//                Map<String, Integer> paramNameIdxMap = proxyMethod.getParamNameIdxMap();
//
//                String sendInstanceId = getSendInstance(idDescMap, paramNameIdxMap, args);
//
//                if (sendInstanceId == null) {
//                    String filters = "[ ";
//                    for (Map.Entry<String, Integer> entry : proxyMethod.getParamNameIdxMap().entrySet()) {
//                        filters += "(" + entry.getKey() + "=" + args[entry.getValue()] + ") ";
//                    }
//                    filters += "]";
//                    //ex
//                    throw new JcInstanceNotFoundException("No remote instance available for cluster [" + proxyMethod.getAppName() + "] with Filters: " + filters);
//                }
//
//                return this.send(proxyMethod, args, sendInstanceId);
//            } else if (proxyMethod.isBroadcast()) {
//
//                LOG.log(Level.INFO, "Sending broadcast: {0}", proxyMethod.getMethodSignature());
//                return this.broadcast(proxyMethod, args);
//
//            } else {
//
//                LOG.log(Level.INFO, "Sending loadbalancing: {0}", proxyMethod.getMethodSignature());
//                return this.sendWithLoadBalancing(proxyMethod, args);
//
//            }
//
//        } catch (IOException ex) {
//
//        }
//
//        return null;
//    }
//
//    public Object send(JcProxyMethod proxyMethod, Object[] args, String sendInstanceId) throws IOException {
//        JcMessage msg = new JcMessage(proxyMethod.getMethodSignature(), proxyMethod.getClassName(), args);
////        return null;
//        if (!instanceMap.get(sendInstanceId).isRunning()) {
//            LOG.warning("We have an instance that is not running");
//        }
//        return instanceMap.get(sendInstanceId).send(msg, proxyMethod.getTimeout()).getData();
//    }
//
//    private String getSendInstance(Map<String, JcAppInstanceDescriptor> idDescMap, Map<String, Integer> paramNameIdxMap, Object[] args) {
//        for (Map.Entry<String, JcAppInstanceDescriptor> entry : idDescMap.entrySet()) {
//            String descId = entry.getKey();
//            JcAppInstanceDescriptor desc = entry.getValue();
//
//            //Checking parameters of instanceDesc with args of method
//            for (Map.Entry<String, Integer> entry1 : paramNameIdxMap.entrySet()) {
//                String filterName = entry1.getKey();
//                Integer idx = entry1.getValue();
//
//                HashSet<Object> filterSet = desc.getFilterMap().get(filterName);
//                if (filterSet != null) {
//                    if (filterSet.contains(args[idx])) {
////                        LOG.log(Level.INFO, "FOUND WHO HAS [{0}] {1}", new Object[]{filterName, args[idx]});
//                        return descId;
//                    }
//
//                }
//            }
//        }
////        throw new JcFilterNotFoundException("No Instance found that contains value for arguments: " + Arrays.toString(args));
//        return null;
//    }
//
//    public boolean broadcast(JcProxyMethod proxyMethod, Object[] args) throws IOException {
//        //if broadcast to 0 instances, fail. Otherwise return true
//        for (Map.Entry<String, JcClientConnection> entry : instanceMap.entrySet()) {
//            String id = entry.getKey();
//            JcClientConnection instance = entry.getValue();
//
//            JcMessage msg = new JcMessage(proxyMethod.getMethodSignature(), proxyMethod.getClassName(), args);
//            instance.send(msg, proxyMethod.getTimeout());
//        }
//        return true;
//    }
//
//    public Object sendWithLoadBalancing(JcProxyMethod proxyMethod, Object[] args) throws IOException {
//        int size = instanceMap.size();
//
//        List<JcClientConnection> connList = new ArrayList<>();
//        for (Map.Entry<String, JcClientConnection> entry : instanceMap.entrySet()) {
//            String id = entry.getKey();
//            JcClientConnection val = entry.getValue();
//
//            connList.add(val);
//        }
//
//        if (size > 0) {
//            //this is a case where an app left the cluster when last sent was at the size of the previous map size
//            if (lastSendAppIndex >= size) {
//                lastSendAppIndex = 0;
//            }
//
//            String instanceId = connList.get(lastSendAppIndex).getRemoteAppDesc().getInstanceId();
//            LOG.log(Level.INFO, "SENDING to:{0} METHOD: {1} PARAMS: {2}", new Object[]{proxyMethod.getAppName(), proxyMethod.getMethodSignature(), Arrays.toString(args)});
//
//            if (lastSendAppIndex < size - 1) {
//                lastSendAppIndex++;
//            } else {
//                lastSendAppIndex = 0;
//            }
//
//            return send(proxyMethod, args, instanceId);
//        } else {
//            throw new JcClusterNotFoundException("No cluster instance available for: " + proxyMethod.getAppName());
//        }
//    }
//
//    public void addConnection(JcClientConnection conn) {
//        instanceMap.put(conn.getRemoteAppDesc().getInstanceId(), conn);
//    }
//
//    public String getJcAppName() {
//        return jcAppName;
//    }
//
//    public Map<String, JcClientConnection> getInstanceMap() {
//        return instanceMap;
//    }
//
//    public void destroy() {
//        for (Map.Entry<String, JcClientConnection> entry : instanceMap.entrySet()) {
//            JcClientConnection conn = entry.getValue();
//            conn.destroy();
//        }
//    }
//
//}
