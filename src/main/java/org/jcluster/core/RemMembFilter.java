/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import java.util.HashSet;
import java.util.Set;
import org.jcluster.core.messages.PublishMsg;
import org.slf4j.LoggerFactory;

/**
 *
 * @author platar86
 */
public class RemMembFilter {

    private static final Logger LOG = (Logger) LoggerFactory.getLogger(RemMembFilter.class);

    private long trIdxMisses = 0;
    private long trIdx = 0;
    private long lastMissTimestamp = 0;
    private final Set<Object> valueSet = new HashSet<>();
    private final String filterName;

    private final JcMember mem;

    public RemMembFilter(String filterName, JcMember mem) {
        this.filterName = filterName;
        this.mem = mem;
        LOG.setLevel(Level.ALL);
    }

    public boolean containsFilterValue(Object val) {
        return valueSet.contains(val);
    }

    public boolean addFilterValue(Object value) {
        return valueSet.add(value);
    }

    public boolean removeFilterValue(Object value) {
        return valueSet.remove(value);
    }

    public synchronized void onFilterPublishMsg(PublishMsg pm) {
        boolean mustVerifyIndex;

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

                valueSet.clear();
                valueSet.addAll(pm.getValueSet());

                trIdxMisses = 0;
                lastMissTimestamp = 0;

                mustVerifyIndex = false;
                LOG.trace("Filter [{}] Add Bulk values  trIdx[{}]  Size: {}", filterName, trIdx, valueSet.size());
                break;
            }
            default: {
                LOG.warn("Invalid filter subsc operation: " + pm.getOperationType());
                return;
            }
        }

        if (mustVerifyIndex) {
            //DEBUG ONLY TODO Remove in production
            if (pm.getValue() != null && pm.getValue().toString().equals("skip")) {
                trIdxMisses++;
            }

            long missing = pm.getTransCount() - trIdx - 1;

            trIdxMisses += missing;

            if (trIdxMisses != 0) {
                lastMissTimestamp = System.currentTimeMillis();
            } else {
                lastMissTimestamp = 0;
            }
        }
        trIdx = pm.getTransCount();
    }

    protected void resetIntegrityTimechek() {
        lastMissTimestamp = System.currentTimeMillis();
    }

    protected boolean checkIntegrity() {
        if (lastMissTimestamp != 0) {
            long timeUnconsistant = System.currentTimeMillis() - lastMissTimestamp;
            if (timeUnconsistant > 10000) {
                LOG.warn("Filter [{}] inconsistent [{} : {}]  since: {} ms Forcing resubscribe", filterName, trIdx, trIdxMisses, timeUnconsistant);
                return false;
            } else {
                LOG.info("Filter [{}] inconsistent  [{} : {}]  since: {} ms  Size: {}", filterName, trIdx, trIdxMisses, timeUnconsistant, valueSet.size());
            }
        } else {
//            LOG.trace("Filter [{}] correct integrity  [{} : {}]  Size: {}", filterName, trIdx, trIdxMisses, valueSet.size());
        }
        return true;
    }

    public String getFilterName() {
        return filterName;
    }

}
