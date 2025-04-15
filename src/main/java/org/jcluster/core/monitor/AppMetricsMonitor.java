/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.monitor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ejb.Stateless;
import org.jcluster.core.JcCoreService;
import org.jcluster.core.JcManager;
import org.jcluster.core.RemMembFilter;
import org.jcluster.core.bean.JcAppDescriptor;
import org.slf4j.LoggerFactory;

/**
 *
 * @author henry
 */
@Stateless
public class AppMetricsMonitor implements AppMetricMonitorInterface {

    private static final ch.qos.logback.classic.Logger LOG = (Logger) LoggerFactory.getLogger(AppMetricsMonitor.class);

    @Override
    public JcMetrics getMetrics(String instanceId) {
        return JcCoreService.getInstance().getAllMetrics();
    }

    @Override
    public String testReq(String instanceId) {
        return "Hello from me: " + JcCoreService.getInstance().getSelfDesc().getInstanceId();
    }

    @Override
    public String callRemote(String instanceId, String remoteInstanceId) {
        AppMetricMonitorInterface metricsMonitor = JcManager.generateProxy(AppMetricMonitorInterface.class);
        return "Connection OK: " + metricsMonitor.testReq(remoteInstanceId);
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
    public Map<String, JcAppDescriptor> getVisibleMembers(String instanceId) {
        return JcCoreService.getMemberMap().values().stream().collect(Collectors.toMap((u) -> u.getId(), (t) -> t.getDesc()));
    }

    @Override
    public void clearAllMetrics(String instanceId) {
        JcCoreService.getInstance().getAllMetrics().clearAllMetrics();
    }

    @Override
    public Map<String, Set<String>> getSubscAppFilterMap(String instanceId) {
        return JcCoreService.getInstance().getSubscAppFilterMap();
    }

    @Override
    public Map<String, RemMembFilter> getMemFilterMap(String instanceId, String memId) {
        return JcCoreService.getMemberMap().get(memId).getFilterMap();
    }

    @Override
    public Map<String, Integer> getLoggers(String instanceId) {
        Map<String, Integer> loggerMap = new HashMap<>();
        List<Logger> loggerList = LOG.getLoggerContext().getLoggerList();
        for (Logger logger : loggerList) {
            if (logger.getLevel() != null) {
                loggerMap.put(logger.getName(), logger.getLevel().levelInt);
            }
        }
        return loggerMap;
    }

    @Override
    public void setLogLevel(String instanceId, String name, Integer level) {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.getLogger(name).setLevel(Level.toLevel(level));
    }

}
