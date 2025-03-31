/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package org.jcluster.core.test;

import org.jcluster.core.monitor.AppMetricMonitorInterface;
import org.jcluster.lib.annotation.JcFilter;
import org.jcluster.lib.annotation.JcRemote;
import org.jcluster.lib.annotation.JcTimeout;

/**
 *
 * @author henry
 */
@JcRemote
public interface JcTestIFace {

    public static final String JC_INSTANCE_FILTER = AppMetricMonitorInterface.JC_INSTANCE_FILTER;

    @JcTimeout(timeout = Integer.MAX_VALUE)
    public Object blockExec(@JcFilter(filterName = JC_INSTANCE_FILTER) String instanceId, int blockTime);

    @JcTimeout(timeout = 5000)
    public byte[] getCustomDataSize(@JcFilter(filterName = JC_INSTANCE_FILTER) String instanceId, int size);

    public void throwEx(@JcFilter(filterName = JC_INSTANCE_FILTER) String instanceId, Throwable e) throws Throwable;

    public void throwRuntimeEx(@JcFilter(filterName = JC_INSTANCE_FILTER) String instanceId, RuntimeException e);
;
}
