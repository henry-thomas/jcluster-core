/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import org.jcluster.core.messages.PublishMsg;
import org.jcluster.core.test.JcTestIFace;
import org.slf4j.LoggerFactory;

/**
 *
 * @author platar86
 */
public class RemMembFilter implements Serializable {

    private static final Logger LOG = (Logger) LoggerFactory.getLogger(RemMembFilter.class);

    private boolean staticFilter = false; // this is for hardcoded filter names
    private long trIdxMisses = 0;
    private long trIdx = 0;
    private long lastMissTimestamp = 0;
    private long lastReceiveTimestamp = 0;
    private final Set<Object> valueSet = new HashSet<>();

    private final Queue<PublishMsg> bufferMessages = new ArrayDeque<>();

    private final String filterName;

    private boolean inReset = false; //wait for bulk insert to remove in reset, if msg arrive buffer it

    public RemMembFilter(String filterName) {

        staticFilter = Objects.equals(filterName, JcTestIFace.JC_INSTANCE_FILTER);

        this.filterName = filterName;
//        this.mem = mem;
        LOG.setLevel(Level.ALL);
    }

    public boolean isLastReceivedExp() {
        if (staticFilter) { // this is for hardcoded filter names
            return false;
        }
        return System.currentTimeMillis() - lastReceiveTimestamp > 5000;
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

    private void onSubscribeStateResp(long receivedTrxCount) {
        long missing = receivedTrxCount - trIdx;

        trIdxMisses += missing;

        if (missing != 0) {
            LOG.trace("Received Sub State Response  new Misses: " + trIdxMisses);
        }
    }

    public synchronized void onFilterPublishMsg(PublishMsg pm) {
        boolean mustVerifyIndex;
        lastReceiveTimestamp = System.currentTimeMillis();

        if (pm.getOperationType() == PublishMsg.OPER_TYPE_SUBSCR_STAT_RESP) {
            onSubscribeStateResp(pm.getTransCount());
            return;
        }

        if (inReset && (pm.getOperationType() == PublishMsg.OPER_TYPE_ADD || pm.getOperationType() == PublishMsg.OPER_TYPE_REMOVE)) {
            bufferMessages.add(pm);
            LOG.info("Filter [{}] received in Reset state. Add to buffer totalSize:{} ", filterName, bufferMessages.size());
            return;
        }

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
                inReset = false; //first reset
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
                if (lastMissTimestamp == 0) {
                    lastMissTimestamp = System.currentTimeMillis();
                }
            } else {
                lastMissTimestamp = 0;
            }
        }
        trIdx = pm.getTransCount();

        //restore filter messages received durring waiting for bulk insertation
        if (pm.getOperationType() == PublishMsg.OPER_TYPE_ADDBULK) {
            //check buffer for filter received after trIdx
            PublishMsg poll;
            while ((poll = bufferMessages.poll()) != null) {
                if (poll.getTransCount() > trIdx) {
                    onFilterPublishMsg(pm);
                }
            }
        }
    }

    protected void onSubsciptionRequest() {
        lastMissTimestamp = System.currentTimeMillis();
        inReset = true;
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

    public Set<Object> getValueSet() {
        return valueSet;
    }

    public long getTrIdxMisses() {
        return trIdxMisses;
    }

    public long getTrIdx() {
        return trIdx;
    }

}
