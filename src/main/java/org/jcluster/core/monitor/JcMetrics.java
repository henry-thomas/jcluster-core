/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.monitor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jcluster.core.bean.JcAppDescriptor;

/**
 *
 * @author henry
 */
public class JcMetrics implements Serializable {

    private static final long serialVersionUID = -3877967371056615756L;

    private final HashMap<String, JcMemberMetrics> memMetricsMap = new HashMap<>();

    private final JcConnMetrics inbound = new JcConnMetrics();
    private final JcConnMetrics outbound = new JcConnMetrics();

    private final InstanceResMonitorBean resBean;
    private final JcAppDescriptor desc;

    public JcMetrics(JcAppDescriptor desc) {
        this.desc = desc;
        this.resBean = new InstanceResMonitorBean(desc.getIpStrPortStr());
    }

    public HashMap<String, JcMemberMetrics> getMemMetricsMap() {
        return memMetricsMap;
    }

    public JcAppDescriptor getDesc() {
        return desc;
    }

    public InstanceResMonitorBean getResBean() {
        return resBean;
    }

    public JcConnMetrics getInbound() {
        return inbound.sumMetrics(memMetricsMap.values().stream().map(m -> m.getInbound()).collect(Collectors.toList()));
    }

    public JcConnMetrics getOutbound() {
        return outbound.sumMetrics(memMetricsMap.values().stream().map(m -> m.getOutbound()).collect(Collectors.toList()));
    }
    
    public void clearAllMetrics(){
        inbound.clear();
        outbound.clear();
        memMetricsMap.entrySet().stream().map(entry -> entry.getValue()).forEachOrdered(met -> {
            met.clear();
        });
    }

}
