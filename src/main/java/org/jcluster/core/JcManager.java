/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

import ch.qos.logback.classic.Logger;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import static org.jcluster.core.JcBootstrap.appNameList;
import static org.jcluster.core.JcBootstrap.topicNameList;
import org.jcluster.core.exception.cluster.JcClusterNotFoundException;
import org.jcluster.core.exception.JcException;
import org.jcluster.core.exception.JcRuntimeException;
import org.jcluster.core.exception.cluster.JcIOException;
import org.jcluster.core.proxy.JcProxyMethod;
import org.jcluster.core.proxy.JcRemoteInvocationHandler;
import org.jcluster.lib.annotation.JcRemote;
import org.slf4j.LoggerFactory;

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

    private static final ch.qos.logback.classic.Logger LOG = (Logger) LoggerFactory.getLogger(JcManager.class);
    private static final JcManager INSTANCE = new JcManager();

    private JcManager() {
    }

    protected static JcManager getInstance() {
        return INSTANCE;
    }

    public static void addFilter(String filterName, Object value) {
        JcCoreService.getInstance().addSelfFilterValue(filterName, value);
    }

    public static void removeFilter(String filterName, Object value) {
        JcCoreService.getInstance().removeSelfFilterValue(filterName, value);
    }

    private static int broadcastSend(JcProxyMethod pm, Object[] args) {
        int instanceBroadcastedTo = 0;
        List<JcRemoteInstanceConnectionBean> riList = null;
        if (pm.isTopic()) {
            riList = JcCoreService.getInstance().getMemConByTopic(pm.getTopicName());
        } else if (pm.getAppName() != null) {
            riList = JcCoreService.getInstance().getMemConByTopic(pm.getAppName());
        }

        if (riList != null) {
            for (JcRemoteInstanceConnectionBean ri : riList) {
                try {
                    ri.send(pm, args);
                    instanceBroadcastedTo++;
                } catch (Exception e) {
                    LOG.warn(null, e);
                }
            }
        }
        return instanceBroadcastedTo;
    }

    private static Object filteredSend(JcProxyMethod pm, Object[] args) throws JcIOException {
        //find all instances by filter

        Map<String, Object> fMap = new HashMap<>();
        //build filter map
        for (Map.Entry<String, Integer> entry : pm.getParamNameIdxMap().entrySet()) {
            String filterName = entry.getKey();
            Integer filtIdx = entry.getValue();
            fMap.put(filterName, args[filtIdx]);
        }

        JcRemoteInstanceConnectionBean ri = null;
        if (!pm.isGlobal()) {
            if (pm.isTopic()) {
                ri = JcCoreService.getInstance().getMemConByTopicAndFilter(pm.getTopicName(), fMap);
            } else if (pm.getAppName() != null) {
                ri = JcCoreService.getInstance().getMemConByAppAndFilter(pm.getAppName(), fMap);
            } else {
                throw new JcIOException(
                        "Invalid JC destination  App: " + pm.getAppName() + "  Toppic: " + pm.getTopicName());
            }
        } else {
            ri = JcCoreService.getInstance().getMemConByFilter(fMap);
        }

        if (ri == null) {
            throw new JcIOException(
                    "No Instance found App: " + pm.getAppName() + "  " + pm.printFilters(args));
        }

        return ri.send(pm, args);
    }

    public static Object send(JcProxyMethod pm, Object[] args) throws JcRuntimeException {

        if (pm.isInstanceFilter()) {//no app name needed if send is specific for remote instance
            return filteredSend(pm, args);
        } else if (pm.isBroadcast()) {
            int broadcastSend = broadcastSend(pm, args);
            if (broadcastSend == 0) {
                throw new JcClusterNotFoundException("No cluster instance available for Broadcast@: " + pm.getAppName());
            }
            return broadcastSend;
        } else {
            JcRemoteInstanceConnectionBean ri = null;
            if (!pm.isGlobal()) {
                if (pm.isTopic()) {
                    ri = JcCoreService.getInstance().getMemConByTopicSingle(pm.getTopicName());
                } else if (pm.getAppName() != null) {
                    ri = JcCoreService.getInstance().getMemConByAppSingle(pm.getAppName());
                } else {
                    throw new JcIOException(
                            "Invalid JC destination  App: " + pm.getAppName() + "  Topic: " + pm.getTopicName());
                }
            }

            if (ri == null) {
                throw new JcIOException(
                        "No Instance found App: " + (pm.isTopic() ? pm.getTopicName() : pm.getAppName()));
            }

            return ri.send(pm, args);
        }
    }

    public final void registerLocalClassImplementation(Class clazz) throws JcException {
        ServiceLookup.getINSTANCE().registerLocalClassImplementation(clazz);
    }

    public static final <T> T generateProxy(Class<T> iClazz) {
        JcRemote jcRemoteAnn = (JcRemote) iClazz.getAnnotation(JcRemote.class);
        if (jcRemoteAnn == null) {
            throw new JcRuntimeException("Invalid class, JcRemote annotation expected.");
        }
        
        ServiceLookup.getINSTANCE().scanAnnotationFilters(iClazz);
        return (T) Proxy.newProxyInstance(JcRemote.class.getClassLoader(), new Class[]{iClazz}, new JcRemoteInvocationHandler());
    }

}
