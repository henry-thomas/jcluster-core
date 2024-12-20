/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.discovery;

import java.util.Map;
import java.util.logging.Logger;
import org.jcluster.core.bean.JcAppDescriptor;

/**
 *
 * @author henry
 */
public class DiscoveryController implements Runnable {

    private static final Logger LOG = Logger.getLogger(DiscoveryController.class.getName());
    private boolean running = true;
    private Long lastUpdate = 0l;
    private final Map<String, JcAppDescriptor> appMap;

    public DiscoveryController(Map<String, JcAppDescriptor> appMap) {
        this.appMap = appMap;
    }

    public void updateKnownInstances() {
        for (Map.Entry<String, JcAppDescriptor> entry : appMap.entrySet()) {
            String appId = entry.getKey();
            JcAppDescriptor val = entry.getValue();
            
        }
        lastUpdate = System.currentTimeMillis();
    }

    @Override
    public void run() {
        while (running) {
            updateKnownInstances();
        }
    }

    public void destroy() {
        running = false;
    }

}
