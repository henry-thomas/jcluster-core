/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.monitor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jcluster.core.JcCoreService;
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

    @Override
    public Map<String, Integer> getFilterList(String instanceId) {
        return JcCoreService.getInstance().getSelfFilterValues()
                .stream()
                .collect(Collectors.toMap((t) -> t.getFilterName(), (f) -> f.getValueSet().size()));
    }

    @Override
    public Set<Object> getFilterValues(String instanceId, String filterName) {
        return JcCoreService.getInstance().getFilterValues(filterName);
    }

    @Override
    public Map<String, String> getVisibleMembers() {
        return JcCoreService.getMemberMap().values().stream().collect(Collectors.toMap((t) -> t.getDesc().getAppName(), (u) -> u.getId()));
    }

}
