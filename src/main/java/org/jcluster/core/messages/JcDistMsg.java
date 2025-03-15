/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.messages;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.jcluster.core.bean.JcAppDescriptor;

/**
 *
 * @author henry
 */
public class JcDistMsg implements Serializable {

    private final JcDistMsgType type;

    private final String msgId;
    private int ttl;
    private JcAppDescriptor src;
    private Object data;
    private String srcIpAddr;

    private final long timestamp = System.currentTimeMillis();

    public JcDistMsg(JcDistMsgType type) {
        this.type = type;
        this.msgId = RandomStringUtils.random(10, true, true);
        this.ttl = 5;
    }

    public JcDistMsg(JcDistMsgType type, String msgId, int ttl) {
        this.type = type;
        this.msgId = msgId;
        this.ttl = ttl;
    }

    public static List<JcDistMsg> createFragmentMessages(JcDistMsg src, JcMsgFragment fr) {
        List<JcDistMsg> frList = new ArrayList<>();
        for (int i = 0; i < fr.getFragments().length; i++) {
            JcMsgFragmentData frData = fr.getFragments()[i];
            JcDistMsg frDataMsg = new JcDistMsg(JcDistMsgType.FAG_DATA);
            frDataMsg.setSrc(src.getSrc());
            frDataMsg.setSrcIpAddr(src.getSrcIpAddr());
            frDataMsg.setData(frData);

            frList.add(frDataMsg);
        }
        return frList;
    }

    public static JcDistMsg generateJoinResponse(JcDistMsg request, JcAppDescriptor src) {
        JcDistMsg resp = new JcDistMsg(JcDistMsgType.JOIN_RESP, request.msgId, request.getTtl() - 1);
        resp.data = request.src;
        resp.src = src;

        return resp;

    }

    public boolean hasTTLExpire() {
        ttl--;
        return ttl <= 0;
    }

    public JcDistMsgType getType() {
        return type;
    }

    public String getMsgId() {
        return msgId;
    }

    public int getTtl() {
        return ttl;
    }

    public JcAppDescriptor getSrc() {
        return src;
    }

    public void setSrc(JcAppDescriptor src) {
        this.src = src;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public String getSrcIpAddr() {
        return srcIpAddr;
    }

    public void setSrcIpAddr(String srcIpAddr) {
        this.srcIpAddr = srcIpAddr;
    }

    @Override
    public String toString() {
        return "JcDistMsg{" + "type=" + type + ", msgId=" + msgId + ", ttl=" + ttl + ", data=" + (data == null ? "null" : data.getClass().getSimpleName()) + '}';
    }

}
