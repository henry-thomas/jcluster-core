/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package org.jcluster.core.monitor;

import javax.ejb.Remote;
import org.jcluster.core.bean.JcMetrics;
import org.jcluster.lib.annotation.JcFilter;
import org.jcluster.lib.annotation.JcRemote;

/**
 *
 * @author henry
 */
@JcRemote
public interface AppMetricMonitorInterface {
    public static final String JC_INSTANCE_FILTER = "jcInstance";

    
    public JcMetrics getMetricsMap(@JcFilter(filterName = JC_INSTANCE_FILTER) String instanceId);
    
    public String testReq(@JcFilter(filterName = JC_INSTANCE_FILTER) String instanceId);
    
}
