/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.cluster;

import java.util.Map;
import org.jcluster.core.bean.JcAppDescriptor;
import org.jcluster.core.bean.jcCollections.jcmap.JcMap;
import org.jcluster.core.config.JcAppConfig;

/**
 *
 * @autor Henry Thomas
 */
public class DiscoveryService {

//    private String appId;
    private final JcMap<String, JcAppDescriptor> appMap;
    private static DiscoveryService INSTANCE = null;
    private final JcAppConfig appConfig;

    private DiscoveryService() {
        appConfig = JcAppConfig.getINSTANCE();
        boolean isPrimary = appConfig.isIsPrimary();
        if (!isPrimary) {
            String primaryMemAddress = appConfig.getJcHzPrimaryMember();

        }
        appMap = new JcMap<>();
        appMap.addEntryListener(new OnNewMemberConnect());
    }

    public synchronized static DiscoveryService getInstance() {

        if (INSTANCE == null) {
            INSTANCE = new DiscoveryService();
        }

        return INSTANCE;
    }

    public JcMap<String, JcAppDescriptor> getInstanceDescriptorMap() {
        return appMap;
    }

    public void showConnected() {
        for (Map.Entry<String, JcAppDescriptor> entry : appMap.entrySet()) {
            String appId = entry.getKey();
            String appName = entry.getValue().getAppName();

            System.out.println(appId + " is online as type: " + appName);
        }
    }

}
