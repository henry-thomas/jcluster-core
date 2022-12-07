/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

import com.hazelcast.org.apache.calcite.jdbc.CalcitePrepare.Dummy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import org.jcluster.core.cluster.JcFactory;
import org.jcluster.core.config.JcAppConfig;

/**
 *
 * @author henry
 */
@Singleton
@Startup
public class JcLifeCycleHooks {

    private static final Logger LOG = Logger.getLogger(JcLifeCycleHooks.class.getName());

    private final HashMap<String, Dummy> dataMap = new HashMap<>();

    private final List<String> emptyMsg = new ArrayList<>();
    private final List<String> smallData = new ArrayList<>();
    private final List<String> bigData = new ArrayList<>();

    @PostConstruct
    public void init() {

        Integer port = JcAppConfig.getINSTANCE().getPort();
        String hostName = JcAppConfig.getINSTANCE().getHostName();
        String appName = JcAppConfig.getINSTANCE().getAppName();

        //Initialize J-Cluster for this app
        JcFactory.initManager(appName, hostName, port);
        LOG.log(Level.INFO, "LifecycleListener: contextInitialized() HOSTNAME: {0} PORT: {1} APPNAME: {2}", new Object[]{hostName, port, appName});
    }

    public HashMap<String, Dummy> getDataMap() {
        return dataMap;
    }

    public List<String> getEmptyMsg() {
        return emptyMsg;
    }

    public List<String> getSmallData() {
        return smallData;
    }

    public List<String> getBigData() {
        return bigData;
    }

    @PreDestroy
    public void destroy() {
        JcFactory.getManager().destroy();
        LOG.info("DataInitializer: destroy()");
    }

}
