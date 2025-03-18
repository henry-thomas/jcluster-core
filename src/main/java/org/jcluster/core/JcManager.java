/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

import ch.qos.logback.classic.Logger;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.naming.NamingException;
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

    protected static Map<String, Object> getDefaultConfig(boolean enterprice) {
        Map<String, Object> config = new HashMap();
        config.put("appName", readProp("JC_APP_NAME", "unknown"));
        config.put("selfIpAddress", readProp("JC_HOSTNAME"));

        config.put("udpListenPort", getConfigUdpListenerPorts("JC_UDPLISTENER_PORTS", "4445-4448"));
        config.put("tcpListenPort", getConfigUdpListenerPorts("JC_TCPLISTENER_PORTS", "2201"));

        String primMemberStr = readProp("JC_PRIMARY_MEMBER_ADDRESS");
        if (primMemberStr != null) {
            List<String> list = new ArrayList<>();
            String[] split = primMemberStr.split(",");
            for (String primMembSpl : split) {
                if (!primMembSpl.contains(":")) {
                    throw new JcRuntimeException("Primary member addres must container port. found: [" + primMembSpl + "] expected: ###.###.###.###:####");
                }
                list.add(primMembSpl);
            }

            config.put("primaryMembers", list);
        }

        ManagedExecutorService exs = null; //executorService
        ManagedThreadFactory th = null;//threadFactory
        //threadFactory

        if (enterprice) {

            try {
                exs = (ManagedExecutorService) ServiceLookup.getServiceEnterprise("concurrent/__defaultManagedExecutorService", null);
                th = (ManagedThreadFactory) ServiceLookup.getServiceEnterprise("concurrent/__defaultManagedThreadFactory", null);
                config.put("executorService", exs);
                config.put("threadFactory", th);

            } catch (NamingException ex) {
                LOG.error(null, ex);
            }
        }

        return config;
    }

    private static List<Integer> getConfigUdpListenerPorts(String portRangeKey, String defaultValue) {
        List<Integer> list = new ArrayList<>();
        String strPort = readProp(portRangeKey, defaultValue);
        if (strPort != null) {
            String[] split = strPort.split(",");
            for (String portSubStr : split) {
                if (portSubStr.contains("-")) {
                    String[] portRange = portSubStr.split("-");
                    int start = Integer.parseInt(portRange[0]);
                    int end = Integer.parseInt(portRange[1]);
                    if (start >= end) {
                        throw new JcRuntimeException("Invalid port configuration [" + portRangeKey + "] String [" + strPort + "]");
                    }
                    for (int i = start; i <= end; i++) {
                        list.add(i);
                    }
                } else {
                    list.add(Integer.valueOf(portSubStr));
                }
            }
        } else {

        }

        return list;
    }

    public static List<String> getPkgFilterList() {
        List<String> pkgFilterList = new ArrayList<>();
        String pkgFilter = readProp("JC_SCAN_PKG_NAME", "");

        if (pkgFilter.equals(
                "")) {
            pkgFilterList.add("");
            LOG.warn("JCluster set to scan the entire package, this can slow down application startup. "
                    + "Please set correct package to scan in domain.xml -> configs -> server-config example: "
                    + "<system-property name=\"JC_SCAN_PKG_NAME\" value=\"com.myPower24.commonLib\"></system-property>");
        } else {
            String[] split = null;
            if (pkgFilter.contains(";")) {
                split = pkgFilter.split(";");
            } else if (pkgFilter.contains(",")) {
                split = pkgFilter.split(",");
            } else if (pkgFilter.contains(" ")) {
                split = pkgFilter.split(" ");
            } else {
                split = new String[]{pkgFilter};
            }

            if (split != null) {
                for (String pkg : split) {
                    pkgFilterList.add(pkg.trim());
                }
            }
        }
        return pkgFilterList;
    }

    private static String readProp(String propName) {
        return readProp(propName, null);
    }

    private static final boolean readProp(String propName, boolean defaultValue) {
        String readProp = readProp(propName, String.valueOf(defaultValue));
        return Boolean.parseBoolean(readProp);
    }

    private static final String readProp(String propName, String defaultValue) {
        Properties properties = System.getProperties();
        String prop = properties.getProperty(propName);
        if (prop == null) {
            LOG.warn("{} property not set!", propName);
            return defaultValue;
        }
        return prop;
    }

    public static boolean containsFilterValue(String app, String fName, Object fValue) {
        return JcCoreService.getInstance().containsFilterValue(app, fName, fValue);
    }

    public static int getFilterValuesCount(String app, String fName) {
        return JcCoreService.getInstance().getFilterValuesCount(app, fName);
    }
}
