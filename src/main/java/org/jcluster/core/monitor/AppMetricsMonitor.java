/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.monitor;

import org.jcluster.core.bean.JcMetrics;

/**
 *
 * @author henry
 */
public class AppMetricsMonitor implements AppMetricMonitorInterface {

 

    @Override
    public JcMetrics getMetricsMap(String instanceId) {
        return null;
    }

    @Override
    public String testReq(String instanceId) {
        return "Hello from me"; 
    }

}
