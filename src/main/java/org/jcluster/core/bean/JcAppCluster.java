/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.bean;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.jcluster.core.messages.JcMessage;
import org.jcluster.core.proxy.JcProxyMethod;
import org.jcluster.core.sockets.JcClientConnection;

/**
 *
 * @author henry
 */
public class JcAppCluster {

    private static final Logger LOG = Logger.getLogger(JcAppCluster.class.getName());

    private final String jcAppName;
    private final Map<String, JcClientConnection> instanceMap = new HashMap<>(); //connections for this app
//    private final Map<Integer, JcMessage> jcMsgMap = new ConcurrentHashMap<>();

    public JcAppCluster(String jcAppName) {
        this.jcAppName = jcAppName;
    }

    public Object send(JcProxyMethod proxyMethod, Object[] args, String sendInstanceId) {
        JcMessage msg = new JcMessage(proxyMethod.getMethodName(), proxyMethod.getClassName(), args);
//        return null;
        if (!instanceMap.get(sendInstanceId).isRunning()) {
            LOG.warning("We have an instance that is not running");
        }
        return instanceMap.get(sendInstanceId).send(msg).getData();
    }

    public boolean removeConnection(JcAppDescriptor instance) {

        JcClientConnection instanceConnection = instanceMap.get(instance.getInstanceId());
        if (instanceConnection != null) {
            instanceConnection.destroy();
        }

        JcClientConnection remove = instanceMap.remove(instance.getInstanceId());
        return remove != null;
    }

    public boolean broadcast(JcProxyMethod proxyMethod, Object[] args) {
        //if broadcast to 0 instances, fail. Otherwise return true
        for (Map.Entry<String, JcClientConnection> entry : instanceMap.entrySet()) {
            String id = entry.getKey();
            JcClientConnection instance = entry.getValue();

            JcMessage msg = new JcMessage(proxyMethod.getMethodName(), proxyMethod.getClassName(), args);
            instance.send(msg);
        }
        return true;
    }

    public void addConnection(JcClientConnection conn) {
        instanceMap.put(conn.getDesc().getInstanceId(), conn);
    }

    public String getJcAppName() {
        return jcAppName;
    }

    public Map<String, JcClientConnection> getInstanceMap() {
        return instanceMap;
    }

    public void destroy() {
        for (Map.Entry<String, JcClientConnection> entry : instanceMap.entrySet()) {
            JcClientConnection conn = entry.getValue();
            conn.destroy();
        }
    }

}
