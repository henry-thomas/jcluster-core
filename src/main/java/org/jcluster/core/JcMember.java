/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

import ch.qos.logback.classic.Level;
import java.io.IOException;
import org.jcluster.core.bean.JcAppDescriptor;
import org.jcluster.core.messages.JcDistMsg;
import ch.qos.logback.classic.Logger;
import java.io.NotSerializableException;
import static java.lang.System.currentTimeMillis;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jcluster.core.bean.FilterDescBean;
import org.jcluster.core.bean.jcCollections.RingConcurentList;
import org.jcluster.core.exception.JcRuntimeException;
import org.jcluster.core.exception.cluster.JcIOException;
import org.jcluster.core.messages.JcDistMsgType;
import org.jcluster.core.messages.JcMessage;
import org.jcluster.core.messages.PublishMsg;
import org.jcluster.core.monitor.AppMetricMonitorInterface;
import org.jcluster.core.monitor.JcMemberMetrics;
import org.jcluster.core.monitor.MethodExecMetric;
import org.jcluster.core.proxy.JcProxyMethod;
import org.slf4j.LoggerFactory;

/**
 *
 * @author henry
 */
public class JcMember {

    private static final Logger LOG = (Logger) LoggerFactory.getLogger(JcMember.class);

    //contains pointers only to outbound connection for quick access
    private final RingConcurentList<JcClientConnection> outboundList = new RingConcurentList<>();

    //Inbound list is managed by an isolated instance, where the remote instance can't make the connection over the network.
    private final RingConcurentList<JcClientConnection> inboundList = new RingConcurentList<>();

    private final JcAppDescriptor desc;
    private final String id;
    private final JcCoreService core;
//    private final JcClientManagedConnection managedConnection;

    private final RingConcurentList<JcClientManagedConnection> managedConnectionList = new RingConcurentList<>();

    //this is for verification and keep track if we are subscribe or not to a filter 
    //at this specific member
    //value is filterName
    private final Set<String> subscribtionSet = new HashSet<>();
    private final JcMemberMetrics metrics;
    private final Map<String, RemMembFilter> filterMap = new HashMap<>();

    private boolean outboundRequired = false;
    private boolean active = true;
    private long lastSeen;
    private boolean onDemandConnection = true;
    private boolean conRequested = true;

    public JcMember(JcClientManagedConnection managedClientCon, JcCoreService core, JcMemberMetrics metrics) {
//        LOG.setLevel(Level.ALL);
        this.core = core;

        this.desc = managedClientCon.getRemoteAppDesc();
//        this.managedConnection = managedClientCon;
        managedConnectionList.add(managedClientCon);
        this.metrics = metrics;
        lastSeen = System.currentTimeMillis();
        //after metrics are set 
        id = desc.getInstanceId();
    }

    protected void verifyFilterIntegrity() {
        filterMap.values().forEach((filter) -> {
            if (!filter.checkIntegrity()) {
                filter.onSubsciptionRequest();

                JcDistMsg jcDistMsg = new JcDistMsg(JcDistMsgType.SUBSCRIBE);
                jcDistMsg.setSrcDesc(core.selfDesc);
                jcDistMsg.setData(filter.getFilterName());

                LOG.info("Sending new unconsistent filter request filter:[{}] to {}",
                        filter.getFilterName(), getId());

                sendManagedMessage(jcDistMsg);
                core.requestMsgMap.put(jcDistMsg.getMsgId(), jcDistMsg);
            }
        });
    }

    public RemMembFilter getOrCreateFilterTarget(String filterName) {
        if (filterName == null) {
            throw new RuntimeException("Invalid filter name NULL");
        }
        RemMembFilter remFilter = filterMap.get(filterName);
        if (remFilter == null) {
            remFilter = new RemMembFilter(filterName);
            filterMap.put(filterName, remFilter);
        }
        return remFilter;
    }

    protected void onFilterPublishMsg(JcDistMsg msg) {
        if (msg == null || !(msg.getData() instanceof PublishMsg)) {
            LOG.warn("Receive invalid Publish filter : {}", msg);
            return;
        }
        PublishMsg pm = (PublishMsg) msg.getData();

        String filterName = pm.getFilterName();
        if (filterName == null) {
            LOG.warn("Receive invalid Publish filter  filterName is NULL");
            return;
        }

        RemMembFilter rmf = getOrCreateFilterTarget(filterName);
        rmf.onFilterPublishMsg(pm);
        LOG.trace("Receive published Filter {} ", pm.getFilterName());
    }

    protected void onSubscResponseMsg(JcDistMsg msg) {
        if (msg == null || !(msg.getData() instanceof PublishMsg)) {
            LOG.warn("Receive invalid Subscription response: {}", msg);
            return;
        }

        PublishMsg pm = (PublishMsg) msg.getData();
        String filterName = pm.getFilterName();
        if (filterName == null) {
            LOG.warn("Receive invalid Subscription response filterName is NULL");
            return;
        }

        //mark that this is already subscribed to
        getSubscribtionSet().add(filterName);

        RemMembFilter rmf = getOrCreateFilterTarget(filterName);
        rmf.onFilterPublishMsg(pm);

    }

    protected void sendPing(JcDistMsg ping) {
        try {
            JcClientManagedConnection conn = null;
            for (JcClientManagedConnection mcn : managedConnectionList) {
                if (conn == null) {
                    conn = mcn;
                    continue;
                }
                if (conn.getLastDataTimestamp() > mcn.getLastDataTimestamp()) {
                    conn = mcn;
                }
            }
            if (conn == null) {
                LOG.warn("sendPing no managed connection available");
                return;
            }
            sendManagedMessage(ping);
            LOG.trace("Sent PING msg MEMBER: {} MSG: {}", this, ping);
        } catch (Throwable e) {
            LOG.warn(null, e);
        }
    }

    protected void sendManagedMessage(JcDistMsg msg) {
        sendManagedMessage(msg, managedConnectionList.getNext());
    }

    protected void sendManagedMessage(JcDistMsg msg, JcClientManagedConnection managedConnection) {
        if (msg.hasTTLExpire()) {
            return;
        }
        try {
            managedConnection.writeAndFlushToOOS(msg);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(JcMember.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
    }

    public JcAppDescriptor getDesc() {
        return desc;
    }

    public boolean isLastSeenExpired() {

        return System.currentTimeMillis() - lastSeen > 30_000;
    }

    public void updateLastSeen() {
        lastSeen = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public Set<String> getSubscribtionSet() {
        return subscribtionSet;
    }

    public int filterSetSize(String fName) {
        RemMembFilter rf = this.filterMap.get(fName);
        if (rf == null) {
            return 0;
        }
        return rf.getValueSet().size();
    }

    public boolean containsFilter(String fName, Object fValue) {
        RemMembFilter rf = this.filterMap.get(fName);
        if (rf == null) {
            return false;
        }
        return rf.containsFilterValue(fValue);
    }

    public boolean containsFilter(Map<String, Object> fMap) {
        for (Map.Entry<String, Object> entry : fMap.entrySet()) {
            String fName = entry.getKey();
            Object fValue = entry.getValue();

            RemMembFilter rf = this.filterMap.get(fName);
            if (rf == null) {
                return false;
            }
            if (!rf.containsFilterValue(fValue)) {
                return false;
            }
        }

        return true;
    }

    protected boolean isSubscribed(String fName) {
        return subscribtionSet.contains(fName);
    }

    protected void notifyOnFilterValueAdd(String fName, Object fVal, FilterDescBean fd) {

        if (!subscribtionSet.contains(fName)) {
            return;
        }
        //from here , remote member is subscribe and we have to send message to give him the new value

    }

    protected void notifyOnFilterValueRemoved(String fName, Object fVal, FilterDescBean fd) {

        if (!subscribtionSet.contains(fName)) {
            return;
        }
        //from here , remote member is subscribe and we have to send message to give him the new value

    }

    public long getLastSeen() {
        return lastSeen;
    }

    public Collection<RemMembFilter> getFilterList() {
        return filterMap.values();
    }

    public JcMemberMetrics getMetrics() {
        return metrics;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 61 * hash + Objects.hashCode(this.desc);
        hash = 61 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final JcMember other = (JcMember) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        return Objects.equals(this.desc, other.desc);
    }

    private void validateTimeout() {
        synchronized (this) {

            List<JcClientConnection> toRemove = new ArrayList<>();

            for (JcClientConnection conn : outboundList) {
                if ((currentTimeMillis() - conn.getLastDataTimestamp()) > 60_000) {
                    if (conn.isClosed()) {
                        LOG.warn("{} Connection is connected, but closed. Removing: {}", conn.getType(), conn.getConnId());
                    }
                    toRemove.add(conn);

                } else if ((currentTimeMillis() - conn.getLastDataTimestamp()) > 10_000) {
                    JcMessage msg = JcMessage.createPingMsg();
                    try {
                        conn.send(msg);
                    } catch (IOException ex) {
                        toRemove.add(conn);
                    }
                }
            }

            if (!toRemove.isEmpty()) {
                for (JcClientConnection conn : toRemove) {
                    removeConnection(conn, "Idle timeout");
                }
            }
        }
    }

    //MOVED FROM JCRemoteInstanceConnectionBean
    private void validateInboundConnectionCount() {
        if (desc == null) {
            return;
        }
        int actualCount = inboundList.size();

        //Incase there is another isolated app connected, don't create an inbound connection to that app
//        if (actualCount < minCount) {
//            for (int i = 0; i < (minCount - actualCount); i++) {
//                JcClientConnection conn = startClientConnection(true);
//                if (conn != null) {
//                    JcCoreService.getInstance().getThreadFactory().newThread(conn).start();
//                    addConnection(conn);
//                } else {
//                    return;
//                }
//            }
//        }
//        LOG.trace("Added: {} new JcClientConnections to {}", actualCount - 1, getId());
    }

    private void validateOutboundConnectionCount() {
        if (desc == null) {
            return;
        }
        if (onDemandConnection && !conRequested) {
            return;
        }
        if (!outboundRequired) {
            return;
        }
        int actualCount = outboundList.size();
        if (actualCount >= 1) {
            return;
        }
        //isolated
        JcClientManagedConnection managedConnection = managedConnectionList.getNext();
        if (core.selfDesc.isIsolated()
                || managedConnection.getRemoteAppDesc().getIpAddress().trim().isEmpty()
                || managedConnection.getIoClientFailCounter() > 5) {
            managedConnection.resetIoClientErr();
            createReversedIoConnection();
        } else {
            try {
                if (managedConnection.getIoClientFailCounter() > 5) {

                } else {
                    JcClientIOConnection.createNewConnection(managedConnection, JcConnectionTypeEnum.OUTBOUND, (con) -> {
                        outboundList.add(con);
                    });
                }
            } catch (Exception e) {
                LOG.warn(null, e);
            }
        }

    }

    private void createReversedIoConnection() {
        JcDistMsg jcDistMsg = new JcDistMsg(JcDistMsgType.CREATE_IO);
        jcDistMsg.setSrcDesc(core.selfDesc);
        jcDistMsg.setData(JcConnectionTypeEnum.OUTBOUND);

        sendManagedMessage(jcDistMsg);
    }

    private void validateSubscription() {
        for (Map.Entry<String, RemMembFilter> entry : filterMap.entrySet()) {
            RemMembFilter filter = entry.getValue();

            if (filter.isLastReceivedExp()) {
                JcDistMsg msg = new JcDistMsg(JcDistMsgType.SUBSCRIBE_STATE_REQ);
                msg.setSrcDesc(JcCoreService.getSelfDesc());
                msg.setData(filter.getFilterName());
                sendManagedMessage(msg);
            }
            filter.checkIntegrity();
        }
    }

    public void onSubscribeStateResp(JcDistMsg msg) {
        PublishMsg pm = (PublishMsg) msg.getData();
        String fName = pm.getFilterName();
        RemMembFilter rmf = filterMap.get(fName);
        rmf.onFilterPublishMsg(pm);
    }

    public void onSubscribeStateReq(JcDistMsg msg) {
        if (msg.getData() != null) {
            String fName = msg.getData().toString();
            FilterDescBean selfFilterValues = JcCoreService.getInstance().getSelfFilterValues(fName);
            if (selfFilterValues == null) {
                if (!fName.equals(AppMetricMonitorInterface.JC_INSTANCE_FILTER)) {
                    LOG.warn("selfFilterValues is null for filter: " + fName);
                }
                return;
            }

            JcDistMsg resp = new JcDistMsg(JcDistMsgType.SUBSCRIBE_STATE_RESP);
            msg.setSrcDesc(JcCoreService.getSelfDesc());

            PublishMsg pm = new PublishMsg();
            pm.setOperationType(PublishMsg.OPER_TYPE_SUBSCR_STAT_RESP);
            pm.setFilterName(fName);
            pm.setTransCount(selfFilterValues.getTransCount());

            resp.setData(pm);
            sendManagedMessage(resp);
        }
    }

    public void validate() {
        for (JcClientManagedConnection mc : managedConnectionList) {
            mc.validate();
        }
//        managedConnection.validate();

        validateSubscription();
        validateTimeout();
        validateInboundConnectionCount();
        validateOutboundConnectionCount();

    }

    public boolean hasMngConWithId(String mngConId) {
        for (JcClientManagedConnection mc : managedConnectionList) {
            if (mc.getConnId().equals(mngConId)) {
                return true;
            }
        }
        return false;
    }

    protected void onManagedConClose(String reason, JcClientManagedConnection mc) {
        boolean remove = managedConnectionList.remove(mc);
        if (!remove) {
            //debug here   
            LOG.trace("Can not remove connection in manage connection list! {}", mc);
        }
        if (!active || !managedConnectionList.isEmpty()) {
            return;
        }
        try {

            synchronized (this) {
                active = false;
                for (JcClientConnection conn : outboundList) {
                    conn.destroy(reason);
                }
                for (JcClientConnection conn : inboundList) {
                    conn.destroy(reason);
                }
                outboundList.clear();
                inboundList.clear();
            }
        } catch (Exception e) {
            LOG.error(null, e);
        }
        JcCoreService.getInstance().onMemberRemove(this, reason);
    }

    public int removeAllConnection(String reason) {
        int count = outboundList.size() + inboundList.size();
        synchronized (this) {
            for (JcClientConnection conn : outboundList) {
                conn.destroy(reason);
            }
            for (JcClientConnection conn : inboundList) {
                conn.destroy(reason);
            }
            outboundList.clear();
            inboundList.clear();
        }
        return count;
    }

    protected boolean removeConnection(JcClientConnection conn, String reason) {
        synchronized (this) {

            if (conn != null && conn.getType() != null) {

                if (conn.getType() == JcConnectionTypeEnum.OUTBOUND) {
                    outboundList.remove(conn);
                } else {
                    inboundList.remove(conn);
                }

                conn.destroy(reason);
                return true;
            }
            return false;
        }
    }

    protected boolean addConnection(JcClientConnection conn) {
        synchronized (this) {
            if (conn != null && conn.getType() != null) {

                if (conn.getType() == JcConnectionTypeEnum.OUTBOUND) {
                    outboundList.add(conn);
                } else {
                    inboundList.add(conn);
                }
                return true;
            }
            return false;
        }
    }

    public boolean isOutboundAvailable() {
        return !outboundList.isEmpty();
    }

    public Object send(JcProxyMethod proxyMethod, Object[] args) throws JcIOException {
        JcClientConnection conn = outboundList.getNext();
        if (conn == null) {
            conRequested = true;
            throw new JcIOException("No outbound connections for: " + this.toString());
        }

        Map<String, MethodExecMetric> execMetricMap = metrics.getOutbound().getMethodExecMap();
        String[] split = proxyMethod.getClassName().split("\\.");
        String className = split[split.length - 1];
        MethodExecMetric execMetric = execMetricMap.get(className + "." + proxyMethod.getMethodSignature());
        if (execMetric == null) {
            execMetric = new MethodExecMetric();
            execMetricMap.put(className + "." + proxyMethod.getMethodSignature(), execMetric);
        }

        try {
            JcMessage msg = new JcMessage(proxyMethod.getMethodSignature(), proxyMethod.getClassName(), args);

            long start = System.currentTimeMillis();
            Object result = conn.send(msg, proxyMethod.getTimeout()).getData();

            execMetric.setLastExecTime(System.currentTimeMillis() - start);

            return result;
        } catch (NotSerializableException ex) {
            throw new JcRuntimeException("Not serializable class: " + ex.getMessage());

        } catch (IOException ex) {
            LOG.warn("Removing connection {} because of {}", conn.getConnId(), ex.getMessage());
            removeConnection(conn, "Send IO exception " + ex.getMessage());
            throw new JcIOException(ex.getMessage());
        }
    }

    protected JcClientManagedConnection getManagedConnection() {
//        return managedConnection;
        return managedConnectionList.getNext();
    }

    public boolean isOnDemandConnection() {
        return onDemandConnection;
    }

    public void setOnDemandConnection(boolean onDemandConnection) {
        this.onDemandConnection = onDemandConnection;
    }

    public void setOutboundRequired() {
        this.outboundRequired = true;
    }

    public boolean isActive() {
        return active;
    }

    public Map<String, RemMembFilter> getFilterMap() {
        return filterMap;
    }

    public void onShutdown() {
        for (JcClientManagedConnection mc : managedConnectionList) {

            try {

                JcDistMsg msg = new JcDistMsg(JcDistMsgType.LEAVE);
                msg.setSrcDesc(core.selfDesc);
                msg.setData("Shutdown");
                sendManagedMessage(msg, mc);
            } catch (Exception e) {
            }
        }

        for (JcClientConnection cc : outboundList) {
            cc.destroy("Server shutdown");
        }
        for (JcClientConnection cc : inboundList) {
            cc.destroy("Server shutdown");
        }

    }

}
