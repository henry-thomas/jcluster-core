/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package org.jcluster.core.monitor;

import java.util.Map;
import java.util.Set;
import org.jcluster.core.RemMembFilter;
import org.jcluster.core.bean.JcAppDescriptor;
import org.jcluster.lib.annotation.JcFilter;
import org.jcluster.lib.annotation.JcRemote;

/**
 *
 * @author henry
 */
@JcRemote
public interface AppMetricMonitorInterface {

    public static final String JC_INSTANCE_FILTER = "jcInstance";

    public JcMetrics getMetrics(@JcFilter(filterName = JC_INSTANCE_FILTER) String instanceId);

    public String testReq(@JcFilter(filterName = JC_INSTANCE_FILTER) String instanceId);

    public Map<String, Integer> getFilterList(@JcFilter(filterName = JC_INSTANCE_FILTER) String instanceId);

    public Set<Object> getFilterValues(@JcFilter(filterName = JC_INSTANCE_FILTER) String instanceId, String filterName);

    public Map<String, Set<String>> getSubscAppFilterMap(@JcFilter(filterName = JC_INSTANCE_FILTER) String instanceId);

    public Map<String, RemMembFilter> getMemFilterMap(@JcFilter(filterName = JC_INSTANCE_FILTER) String instanceId, String memId);

    public Map<String, JcAppDescriptor> getVisibleMembers(@JcFilter(filterName = JC_INSTANCE_FILTER) String instanceId);

    public void clearAllMetrics(@JcFilter(filterName = JC_INSTANCE_FILTER) String instanceId);

    public String callRemote(@JcFilter(filterName = JC_INSTANCE_FILTER) String instanceId, String remoteInstanceId);

    public void setLogLevel(@JcFilter(filterName = JC_INSTANCE_FILTER) String instanceId, String name, Integer level);

    public Map<String, Integer> getLoggers(@JcFilter(filterName = JC_INSTANCE_FILTER) String instanceId);

}
