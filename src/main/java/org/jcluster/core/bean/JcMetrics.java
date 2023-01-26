/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.bean;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import org.jcluster.core.monitor.InstanceResMonitorBean;

/**
 *
 * @author henry
 */
public class JcMetrics implements Serializable{

    private final HashMap<String, List<JcConnectionMetrics>> connMetricsMap;
    private final InstanceResMonitorBean resBean;

    public JcMetrics(HashMap<String, List<JcConnectionMetrics>> connMetricsMap, InstanceResMonitorBean resBean) {
        this.connMetricsMap = connMetricsMap;
        this.resBean = resBean;
    }

    public HashMap<String, List<JcConnectionMetrics>> getConnMetricsMap() {
        return connMetricsMap;
    }

    public InstanceResMonitorBean getResBean() {
        return resBean;
    }

}
