/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

import ch.qos.logback.classic.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.naming.NamingException;
import org.jcluster.core.bean.JcMemFilterMetric;
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
    private static final Properties startupProp = new Properties();

    private static final JcManager INSTANCE = new JcManager();
    public static final int DEFAULT_IPPORT = 8201;

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
        List<JcMember> riList = getMatchingInstances(pm, args);

        if (riList != null) {
            for (JcMember ri : riList) {
                try {
                    ri.send(pm, args);
                    instanceBroadcastedTo++;
                } catch (Exception e) {
                    LOG.warn("Failed to send to instance: " + ri, e);
                }
            }
        }
        return instanceBroadcastedTo;
    }

    private static Object filteredSend(JcProxyMethod pm, Object[] args) throws JcIOException {
        //find all instances by filter
        JcMember ri = getFilteredInstance(pm, args);

        if (ri == null) {
            throw new JcIOException(
                    "No Instance found App: " + pm.getAppName() + "  " + pm.printFilters(args));
        }

//        System.out.println("exec: " + pm.getMethodSignature() + " Send to instance: " + ri.getMember().getId());
        return ri.send(pm, args);
    }

    public static Object send(JcProxyMethod pm, Object[] args) throws JcRuntimeException {

        if (pm.isBroadcast()) {
            int broadcastSend = broadcastSend(pm, args);
            if (broadcastSend == 0) {
//                throw new JcClusterNotFoundException("No cluster instance available for Broadcast@: " + pm);
            }
            return broadcastSend;
        }

        if (pm.isInstanceFilter()) {
            return filteredSend(pm, args);
        }

        JcMember ri = getSingleInstance(pm, args);
        if (ri != null) {
            return ri.send(pm, args);
        }

        throw new JcIOException(
                "No remote connection found for: " + pm.toString());
        //no app name needed if send is specific for remote instance
    }

    private static JcMember getFilteredInstance(JcProxyMethod pm, Object[] args) {
        if (pm.isTopic()) {
            return JcCoreService.getInstance().getMemConByTopicAndFilter(pm.getTopicName(), buildFilterMap(pm, args));
        }
        if (pm.getAppName() != null) {
            return JcCoreService.getInstance().getMemConByAppAndFilter(pm.getAppName(), buildFilterMap(pm, args));
        }
        if (pm.isGlobal()) {
            return JcCoreService.getInstance().getMemConByFilter(buildFilterMap(pm, args));
        }
        throw new JcIOException(
                "Invalid JC destination  App: " + pm.getAppName() + "  Topic: " + pm.getTopicName());

    }

    private static JcMember getSingleInstance(JcProxyMethod pm, Object[] args) {
        if (pm.isTopic()) {
            return JcCoreService.getInstance().getMemConByTopicSingle(pm.getTopicName());
        }
        if (pm.getAppName() != null) {
            return JcCoreService.getInstance().getMemConByAppSingle(pm.getAppName());
        }

        throw new JcIOException(
                "Invalid JC destination  App: " + pm.getAppName() + "  Topic: " + pm.getTopicName());

    }

    private static List<JcMember> getMatchingInstances(JcProxyMethod pm, Object[] args) {
        if (pm.isTopic()) {
            return pm.isInstanceFilter()
                    ? JcCoreService.getInstance().getMemConListByTopicAndFilter(pm.getTopicName(), buildFilterMap(pm, args))
                    : JcCoreService.getInstance().getMemConByTopic(pm.getTopicName());
        } else if (pm.getAppName() != null) {
            return JcCoreService.getInstance().getMemConByTopic(pm.getAppName());
        }
        return Collections.emptyList();
    }

    private static Map<String, Object> buildFilterMap(JcProxyMethod pm, Object[] args) {
        Map<String, Object> fMap = new HashMap<>();
        for (Map.Entry<String, Integer> entry : pm.getParamNameIdxMap().entrySet()) {
            fMap.put(entry.getKey(), args[entry.getValue()]);
        }
        return fMap;
    }

    public static final void registerLocalClassImplementation(Class clazz) throws JcException {
        ServiceLookup.getINSTANCE().registerLocalClassImplementation(clazz);
    }

    public static final <T> T generateProxy(Class<T> iClazz) {
        JcRemote jcRemoteAnn = (JcRemote) iClazz.getAnnotation(JcRemote.class);
        if (jcRemoteAnn == null) {
            throw new JcRuntimeException("Invalid class, JcRemote annotation expected.");
        }

        ServiceLookup.getINSTANCE().scanProxyAnnotationFilters(iClazz);
        return (T) Proxy.newProxyInstance(JcRemote.class.getClassLoader(), new Class[]{iClazz}, new JcRemoteInvocationHandler(iClazz));
    }

    protected static Map<String, Object> getDefaultConfig(boolean enterprice) {
        return getDefaultConfig(null, enterprice);
    }

    public static void processStartupArgs(String[] args) {
        Map<String, String> argMap = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String[] strArr = args[i].split("=");

            if (strArr.length == 2) {
                argMap.put(strArr[0], strArr[1]);
            } else if (strArr.length == 1) {
                argMap.put(strArr[0], null);
            }
        }

        String prop = argMap.get("title");
        if (prop != null) {
            startupProp.put("JC_TITLE", prop);
        }
        prop = argMap.get("appName");
        if (prop != null) {
            startupProp.put("JC_APP_NAME", prop);
        }
        prop = argMap.get("primaryMembers");
        if (prop != null) {
            startupProp.put("JC_PRIMARY_MEMBER_ADDRESS", prop);
        }
        prop = argMap.get("udpListenPort");
        if (prop != null) {
            startupProp.put("JC_UDPLISTENER_PORTS", prop);
        }
        prop = argMap.get("tcpListenPort");
        if (prop != null) {
            startupProp.put("JC_TCPLISTENER_PORTS", prop);
        }
        prop = argMap.get("selfIpAddress");
        if (prop != null) {
            startupProp.put("JC_HOSTNAME", prop);
        }
    }

    protected static Map<String, Object> getDefaultConfig(String[] args, boolean enterprice) {
        Map<String, Object> config = new HashMap();

        if (args != null) {
            processStartupArgs(args);
        }

        config.put("appName", readProp("JC_APP_NAME", "unknown"));
        config.put("title", readProp("JC_TITLE", ""));

        String hostName = readProp("JC_HOSTNAME");
        if (hostName == null || hostName.isEmpty()) {
            config.put("selfIpAddress", null);
        } else if (hostName.toLowerCase().equals("auto")) {
            config.put("selfIpAddress", getHostName());
        } else {
            config.put("selfIpAddress", hostName);
        }

        config.put("tcpListenPort", getConfigIpPort("JC_TCPLISTENER_PORTS", String.valueOf(DEFAULT_IPPORT)));

        String primMemberStr = readProp("JC_PRIMARY_MEMBER_ADDRESS");
        if (primMemberStr != null) {
            List<String> list = new ArrayList<>();
            String[] split = primMemberStr.split(",");
            for (String primMembSpl : split) {
//                if (!primMembSpl.contains(":")) {
//                    throw new JcRuntimeException("Primary member addres must container port. found: [" + primMembSpl + "] expected: ###.###.###.###:####");
//                }
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
        String topicStr = readProp("JC_TOPICS");
        if (topicStr != null) {
            Set<String> tset = new HashSet<>();
            String tarr[] = topicStr.split(",");
            for (String t : tarr) {
                tset.add(t.trim());
            }
            config.put("topics", tset);
        }
        return config;
    }

    private static String getHostName() {
        try {

            String urlString = "http://ipecho.net/plain";
            URL url = new URL(urlString);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
                return br.readLine();
            } catch (IOException ex) {
                LOG.error(null, ex);
            }

        } catch (MalformedURLException ex) {
            java.util.logging.Logger.getLogger(JcManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private static List<Integer> getConfigIpPort(String portRangeKey, String defaultValue) {
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

        if (pkgFilter.equals("*")) {
            pkgFilterList.add("");
            LOG.warn("JCluster set to scan the entire package, this can slow down application startup. "
                    + "Please set correct package to scan in domain.xml -> configs -> server-config example: "
                    + "<system-property name=\"JC_SCAN_PKG_NAME\" value=\"com.myPower24.commonLib\"></system-property>");
        } else if (pkgFilter.equals("")) {
//            pkgFilterList.add("");
            LOG.warn("JCluster will not scan any packages!"
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

    private static boolean readProp(String propName, boolean defaultValue) {
        String readProp = readProp(propName, String.valueOf(defaultValue));
        return Boolean.parseBoolean(readProp);
    }

    private static String readProp(String propName, String defaultValue) {
        Properties properties = System.getProperties();
        String prop = properties.getProperty(propName);
        if (prop == null) {
            prop = startupProp.getProperty(propName);
        }
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

    public static List<JcMemFilterMetric> getMemFilterValuesCount(String app, String fName) {
        return JcCoreService.getInstance().getMemFilterValuesCount(app, fName);
    }
    
    
}
