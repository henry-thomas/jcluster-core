/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.monitor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.jcluster.core.bean.JcAppDescriptor;

/**
 *
 * @author henry
 */
public class JcMetrics implements Serializable {

    private static final long serialVersionUID = -3877967371056615756L;

    private final HashMap<String, JcMemberMetrics> memMetricsMap = new HashMap<>();
    private JcMemberMetrics selfMetrics;

    private final InstanceResMonitorBean resBean;
    private final JcAppDescriptor desc;

    public JcMetrics(JcAppDescriptor desc) {
        this.desc = desc;
        this.resBean = new InstanceResMonitorBean(desc.getIpStrPortStr());
    }

    public void calcSelfMetrics() {
        selfMetrics = new JcMemberMetrics();
        selfMetrics.setAppName(desc.getAppName());
        selfMetrics.setInstanceId(desc.getInstanceId());
        
        for (Map.Entry<String, JcMemberMetrics> entry : memMetricsMap.entrySet()) {
            JcMemberMetrics met = entry.getValue();

            selfMetrics.getInbound().addMetrics(met.getInbound());
            selfMetrics.getOutbound().addMetrics(met.getOutbound());
        }
    }

    public HashMap<String, JcMemberMetrics> getMemMetricsMap() {
        return memMetricsMap;
    }

    public JcMemberMetrics getSelfMetrics() {
        calcSelfMetrics();
        return selfMetrics;
    }

    public JcAppDescriptor getDesc() {
        return desc;
    }

    public InstanceResMonitorBean getResBean() {
        return resBean;
    }

}
