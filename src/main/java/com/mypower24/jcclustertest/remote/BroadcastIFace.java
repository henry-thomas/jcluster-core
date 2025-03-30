package com.mypower24.jcclustertest.remote;

import org.jcluster.lib.annotation.JcBroadcast;
import org.jcluster.lib.annotation.JcFilter;
import org.jcluster.lib.annotation.JcRemote;

/**
 *
 * @author henry
 */
@JcRemote(topic = "testMetrics")
public interface BroadcastIFace {
    public final static String SUBSCRIBED_APP_FILTER = "bcTestFilter";
    
    @JcBroadcast
    public void onTopicMessage(@JcFilter(filterName = SUBSCRIBED_APP_FILTER) String appIdFilter, String message);
    
    @JcBroadcast
    public void onBcMessage(String message);
}
 