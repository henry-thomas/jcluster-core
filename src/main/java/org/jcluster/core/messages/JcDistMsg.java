/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.messages;

import java.io.Serializable;
import org.apache.commons.lang3.RandomStringUtils;
import org.jcluster.core.bean.JcAppDescriptor;

/**
 *
 * @author henry
 */
public class JcDistMsg implements Serializable {

    private final JcDistMsgType type;
    public static final int TTL_DEFAULT = 5;

    private final String msgId;
    private int ttl;
    private JcAppDescriptor srcDesc;
    private Object data;
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

    public JcAppDescriptor getSrcDesc() {
        return srcDesc;
    }

    public void setSrcDesc(JcAppDescriptor srcDesc) {
        this.srcDesc = srcDesc;
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

    @Override
    public String toString() {
        return "JcDistMsg{" + "type=" + type + ", msgId=" + msgId + ", ttl=" + ttl + ", data=" + (data == null ? "null" : data.getClass().getSimpleName()) + '}';
    }

}
