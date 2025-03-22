/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.monitor;

import java.io.Serializable;

/**
 *
 * @author henry
 */
public class JcMemberMetrics implements Serializable {

    private static final long serialVersionUID = 3468516841763326041L;

    private Integer filterSize;
    private String appName;
    private String instanceId;

    private final JcConnMetrics inbound = new JcConnMetrics();
    private final JcConnMetrics outbound = new JcConnMetrics();

    public JcMemberMetrics() {
    }

    public void clear() {
        inbound.clear();
        outbound.clear();
    }

    public String getAppName() {
        return appName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public JcConnMetrics getInbound() {
        return inbound;
    }

    public JcConnMetrics getOutbound() {
        return outbound;
    }

    public Integer getFilterSize() {
        return filterSize;
    }

    public void setFilterSize(Integer filterSize) {
        this.filterSize = filterSize;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

}
