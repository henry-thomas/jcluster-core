/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.monitor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import org.jcluster.core.JcMember;
import org.jcluster.core.bean.JcAppDescriptor;

/**
 *
 * @author henry
 */
public class JcMetrics implements Serializable {

    private static final long serialVersionUID = -3877967371056615756L;

    private final HashMap<String, List<JcMemberMetricsInOut>> connMetricsMap = new HashMap<>();
    private final HashMap<String, JcMemberMetrics> memMetricsMap = new HashMap<>();
    
    private final InstanceResMonitorBean resBean;
    private final JcAppDescriptor desc;

    public JcMetrics(JcAppDescriptor desc) {
        this.desc = desc;
        resBean = new InstanceResMonitorBean(desc.getIpStrPortStr());
    }

    public HashMap<String, JcMemberMetrics> getMemMetricsMap() {
        return memMetricsMap;
    }

    public JcAppDescriptor getDesc() {
        return desc;
    }

    public HashMap<String, List<JcMemberMetricsInOut>> getConnMetricsMap() {
        return connMetricsMap;
    }

    public InstanceResMonitorBean getResBean() {
        return resBean;
    }

}
