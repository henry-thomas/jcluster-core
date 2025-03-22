/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

import ch.qos.logback.classic.Level;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import org.jcluster.core.bean.JcAppDescriptor;
import org.jcluster.core.messages.JcDistMsg;
import ch.qos.logback.classic.Logger;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jcluster.core.bean.FilterDescBean;
import org.jcluster.core.exception.fragmentation.JcFragmentationException;
import org.jcluster.core.messages.JcDistMsgType;
import org.jcluster.core.messages.JcMsgFragment;
import static org.jcluster.core.messages.JcMsgFragment.FRAGMENT_DATA_MAX_SIZE;
import org.jcluster.core.messages.JcMsgFragmentData;
import org.jcluster.core.messages.JcMsgFrgResendReq;
import org.jcluster.core.messages.PublishMsg;
import org.jcluster.core.monitor.JcMemberMetrics;
import org.slf4j.LoggerFactory;

/**
 *
 * @author henry
 */
public class JcMember {

    private static final Logger LOG = (Logger) LoggerFactory.getLogger(JcMember.class);

    private JcAppDescriptor desc;
    private DatagramSocket socket;
    private long lastSeen;
    private String id;
    private final JcCoreService core;

    //container to store send fragments in case of requested segment resend
    private final Map<String, JcMsgFragment> txFragList = new HashMap<>();
    private final Map<String, JcMsgFragment> rxFragList = new HashMap<>();

    private final JcRemoteInstanceConnectionBean conector;

    //this is for verification and keep track if we are subscribe or not to a filter 
    //at this specific member
    //value is filterName
    private final Set<String> subscribtionSet = new HashSet<>();

    private final JcMemberMetrics metrics;

    private final Map<String, RemMembFilter> filterMap = new HashMap<>();

    public JcMember(JcAppDescriptor desc, JcCoreService core, JcMemberMetrics metrics) {
        LOG.setLevel(Level.ALL);
        this.desc = desc;
        this.core = core;
        conector = new JcRemoteInstanceConnectionBean(metrics);
        if (desc != null) {
            conector.setDesc(desc);
            id = desc.getIpStrPortStr();
        }
        this.metrics = metrics;
    }

    public JcRemoteInstanceConnectionBean getConector() {
        return conector;
    }

    void verifyRxFrag() {
        long now = System.currentTimeMillis();
        for (Iterator<JcMsgFragment> iterator = rxFragList.values().iterator(); iterator.hasNext();) {
            JcMsgFragment fr = iterator.next();

            if (now - fr.getTimestamp() > 500) {
                if (fr.getRecoverCount() >= 3) {
                    rxFragList.remove(fr.getFrgId());
                    LOG.info("Fragment [{}] could not resend.", fr.getFrgId());
                } else {
                    LOG.info("Sending FrgResend request [{}]", fr.getFrgId());

                    List<Integer> missingFragmenList = fr.getMissingFragmenList(); //this will increment missing counter
                    JcMsgFrgResendReq resendReq = new JcMsgFrgResendReq(fr.getFrgId(), missingFragmenList);

                    JcDistMsg jcDistMsg = new JcDistMsg(JcDistMsgType.FRG_RESEND);
                    jcDistMsg.setSrc(core.selfDesc);
                    jcDistMsg.setData(resendReq);

                    try {
                        sendMessage(jcDistMsg);
                    } catch (IOException ex) {
                        java.util.logging.Logger.getLogger(JcMember.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

    void verifyFilterIntegrity() {

        filterMap.values().forEach((filter) -> {
            if (!filter.checkIntegrity()) {
                filter.onSubsciptionRequest();

                JcDistMsg jcDistMsg = new JcDistMsg(JcDistMsgType.SUBSCRIBE);
                jcDistMsg.setSrc(core.selfDesc);
                jcDistMsg.setData(filter.getFilterName());

                try {
                    LOG.info("Sending new unconsistent filter request filter:[{}] to {}",
                            filter.getFilterName(), getId());

                    sendMessage(jcDistMsg);
                    core.requestMsgMap.put(jcDistMsg.getMsgId(), jcDistMsg);
                } catch (IOException ex) {
                    LOG.warn(null, ex);
                }
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

    void onFrgMsgAck(JcDistMsg msg) {
        if (msg.getData() == null || !(msg.getData() instanceof String)) {
            LOG.warn("Invalid Fragmented ACK msg received!");
            return;
        }

        JcMsgFragment get = txFragList.remove(msg.getData());
        if (get == null) {
            LOG.warn("Receive FragACK FrId[{}] without valid fragment in buffer", msg.getData());
        } else {
            LOG.trace("Receive FragACK FrId[{}]r", msg.getData());
        }

    }

    void onFrgMsgResend(JcDistMsg msg) {
        if (msg.getData() == null || !(msg.getData() instanceof JcMsgFrgResendReq)) {
            LOG.warn("Invalid Fragmented Resend msg received!");
            return;
        }
        JcMsgFrgResendReq resendReq = (JcMsgFrgResendReq) msg.getData();

        JcMsgFragment fr = txFragList.get(resendReq.getFrgId());
        if (fr == null) {
            LOG.warn("Fragmented Resend msg received not in buffer frId[{}]", resendReq.getFrgId());
            return;
        }

        LOG.trace("Receive Resend request Fragment {}", resendReq.getFrgId());
        for (Integer frIdx : resendReq.getResendFrIdx()) {

            JcMsgFragmentData frData = fr.getFragments()[frIdx];
            JcDistMsg frDataMsg = new JcDistMsg(JcDistMsgType.FRG_DATA);
            frDataMsg.setSrc(core.selfDesc);
            frDataMsg.setData(frData);
            try {
                LOG.trace("Resend FragmentData {}", frData);
                sendMessage(frDataMsg);
            } catch (IOException ex) {
            }
        }

    }

    protected void onFrgMsgReceived(JcDistMsg msg) {
        if (msg.getData() == null || !(msg.getData() instanceof JcMsgFragmentData)) {
            LOG.warn("Invalid Fragmented msg received!");
            return;
        }
        JcMsgFragmentData frData = (JcMsgFragmentData) msg.getData();

        JcMsgFragment rxFr = rxFragList.get(frData.getFrgId());
        if (rxFr == null) {
            rxFr = JcMsgFragment.createRxFragmentMsg(frData);
            rxFragList.put(frData.getFrgId(), rxFr);
            LOG.trace("Receive new Fragmented message: {}", rxFr);
        } else {
            LOG.trace("Receive Fragmented data message: {}", rxFr);
        }
        try {
            boolean completed = rxFr.addFragmentReceivedData(frData);

            if (completed) {
                rxFragList.remove(frData.getFrgId());
                byte[] rxData = rxFr.getRxData();
                try {
                    core.onFragmentedDataRx(rxData);
                } catch (Exception e) {
                    LOG.error(null, e);
                }

                JcDistMsg frAckMsg = new JcDistMsg(JcDistMsgType.FRG_ACK);
                frAckMsg.setSrc(core.selfDesc);
                frAckMsg.setData(frData.getFrgId());
                try {
                    sendMessage(frAckMsg);
                } catch (IOException ex) {
                }
            }

        } catch (JcFragmentationException ex) {
            rxFragList.remove(frData.getFrgId());
            LOG.error(null, ex);
        }
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

    public void close() {
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception e) {
            }
        }
        conector.destroy();
    }

    private void sendMessage(JcDistMsg msg, String ip, int port) throws IOException {

        if (msg.hasTTLExpire()) {
            return;
        }

        if (socket == null) {
            socket = new DatagramSocket();
            socket.setSendBufferSize(FRAGMENT_DATA_MAX_SIZE * 1000);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = new ObjectOutputStream(bos);
        out.writeObject(msg);

        byte[] data = bos.toByteArray();

        if (data.length > JcCoreService.UDP_FRAME_FRAGMENTATION_SIZE) {
            JcMsgFragment fr = JcMsgFragment.createTxFragmentMsg(data);

            List<JcDistMsg> toSend = JcDistMsg.createFragmentMessages(msg, fr);
            for (int i = 0; i < toSend.size(); i++) {

                sendMessage(toSend.get(i), ip, port);
            }

            txFragList.put(fr.getFrgId(), fr);
            LOG.trace("Sending Fragmented message: {}", fr);
        } else {
            DatagramPacket p = new DatagramPacket(data, data.length, InetAddress.getByName(ip), port);
            socket.send(p);

//            try {
//                Thread.sleep(1);
//            } catch (InterruptedException ex) {
//                java.util.logging.Logger.getLogger(JcMember.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//            }
        }
    }

    public void sendMessage(JcDistMsg msg) throws IOException {

        sendMessage(msg, desc.getIpAddress(), desc.getIpPortListenUDP());

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
        if (!rf.containsFilterValue(fValue)) {
            return false;
        }

        return true;
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

}
