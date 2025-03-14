/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.bean;

import ch.qos.logback.classic.Logger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jcluster.core.messages.PublishMsg;
import org.slf4j.LoggerFactory;

/**
 *
 * @author platar86
 */
public class RemMembFilter {

    private static final Logger LOG = (Logger) LoggerFactory.getLogger(RemMembFilter.class);
    private final Map<String, Long> filterMapTrIdxValuesMiss = new HashMap<>();
    private long trIdxMisses = 0;
    private long trIdx = 0;
    private long lastMissTimestamp = 0;
    private final Set<Object> filterMapValues = new HashSet<>();

    public boolean containsFilterValue(Object val) {
        return filterMapValues.contains(val);
    }

    public boolean addFilterValue(Object value) {
        return filterMapValues.add(value);
    }

    public boolean removeFilterValue(Object value) {
        return filterMapValues.remove(value);
    }

    public void onFilterPublishMsg(PublishMsg pm) {
        boolean mustVerifyIndex = false;
       
        switch (pm.getOperationType()) {
            case PublishMsg.OPER_TYPE_ADD: {
                addFilterValue(pm.getValue());
                mustVerifyIndex = true;
                break;
            }
            case PublishMsg.OPER_TYPE_REMOVE: {
                removeFilterValue(pm.getValue());
                mustVerifyIndex = true;
                break;
            }
            case PublishMsg.OPER_TYPE_ADDBULK: {
                if (pm.getValueSet() == null) {
                    LOG.warn("Invalid Publish {} subsc operation: set is NULL", pm.getOperationType());
                    return;
                }

                filterMapValues.clear();
                filterMapValues.add(pm.getOperationType());

                trIdxMisses = 0;
                lastMissTimestamp = 0;

                mustVerifyIndex = false;
                break;
            }
            default: {
                LOG.warn("Invalid filter subsc operation: " + pm.getOperationType());
                return;
            }
        }

        if (mustVerifyIndex) {
            long missing = pm.getTransCount() - trIdx + 1;
            trIdxMisses -= missing;

            if (trIdxMisses != 0) {
                lastMissTimestamp = System.currentTimeMillis();
            } else {
                lastMissTimestamp = 0;
            }
        }
        trIdx = pm.getTransCount();
    }
    
    

}
