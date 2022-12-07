/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

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

    @PostConstruct
    public void init() {
        Integer port = JcAppConfig.getINSTANCE().getPort();
        String hostName = JcAppConfig.getINSTANCE().getHostName();
        String appName = JcAppConfig.getINSTANCE().getAppName();

        //Initialize J-Cluster for this app
        JcFactory.initManager(appName, hostName, port);
        LOG.log(Level.INFO, "LifecycleListener: contextInitialized() HOSTNAME: {0} PORT: {1} APPNAME: {2}", new Object[]{hostName, port, appName});
    }

    @PreDestroy
    public void destroy() {
        JcFactory.getManager().destroy();
        LOG.info("DataInitializer: destroy()");
    }

}
