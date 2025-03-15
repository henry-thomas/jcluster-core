/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.messages;

import java.io.Serializable;

/**
 *
 * @author platar86
 */
public class JcMsgFragmentData implements Serializable{

    private final String frgId;
    private final int seq;
    private final int totalFragments;
    private final byte data[];

    public JcMsgFragmentData(String frgId, int seq, int totalSeg, byte[] data) {
        this.frgId = frgId;
        this.seq = seq;
        this.totalFragments = totalSeg;
        this.data = data;
    }

    public String getFrgId() {
        return frgId;
    }

    public int getSeq() {
        return seq;
    }

    public byte[] getData() {
        return data;
    }

    public int getTotalFragments() {
        return totalFragments;
    }

    @Override
    public String toString() {
        return "JcMsgFragmentData{" + "frgId=" + frgId + ", seq=" + seq + ", totalFragments=" + totalFragments + '}';
    }

}
