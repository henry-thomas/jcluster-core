/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.monitor;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ejb.Stateless;
import org.jcluster.core.JcCoreService;

/**
 *
 * @author henry
 */
@Stateless
public class AppMetricsMonitor implements AppMetricMonitorInterface {

    @Override
    public JcMetrics getMetrics(String instanceId) {
        return JcCoreService.getInstance().getMetrics();
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
    public Map<String, String> getVisibleMembers(String instanceId) {
        return JcCoreService.getMemberMap().values().stream().collect(Collectors.toMap((u) -> u.getId(), (t) -> t.getDesc().getAppName()));
    }

}
