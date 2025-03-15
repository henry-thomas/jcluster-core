/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.messages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.RandomStringUtils;
import org.jcluster.core.exception.fragmentation.JcFragmentationException;

/**
 *
 * @author platar86
 */
public class JcMsgFragment {

    public static final int FRAGMENT_DATA_MAX_SIZE = 60_000; //FROM max ~650000 we need to remove some overhead from the container message 

    private String frgId;
    private long timestamp;
    private int totalFrag;
    private int rxFragCount = 0;
    private int recoverCount = 0;

    private JcMsgFragmentData fragments[] = null;

    private byte rxData[] = null;

    private JcMsgFragment() {
    }

    public static JcMsgFragment createRxFragmentMsg(JcMsgFragmentData frData) {
        JcMsgFragment fr = new JcMsgFragment();
        fr.frgId = frData.getFrgId();
        fr.fragments = new JcMsgFragmentData[frData.getTotalFragments()];
        fr.timestamp = System.currentTimeMillis();
        fr.totalFrag = frData.getTotalFragments();
        return fr;
    }

    public static JcMsgFragment createTxFragmentMsg(byte[] data) {
        JcMsgFragment fr = new JcMsgFragment();
        fr.frgId = RandomStringUtils.random(16, true, true);
        fr.timestamp = System.currentTimeMillis();

        //create all fragments
        int requredFr = data.length / FRAGMENT_DATA_MAX_SIZE + (data.length % FRAGMENT_DATA_MAX_SIZE != 0 ? 1 : 0);
        fr.fragments = new JcMsgFragmentData[requredFr];

        for (int i = 0; i < requredFr; i++) {
            int startPos = i * FRAGMENT_DATA_MAX_SIZE;
            int end = startPos + FRAGMENT_DATA_MAX_SIZE;
            if (end > data.length) {
                end = data.length;
            }

            byte[] fragData = Arrays.copyOfRange(data, startPos, end);
            fr.fragments[i] = new JcMsgFragmentData(fr.frgId, i, requredFr, fragData);
        }
        fr.totalFrag = requredFr;
        return fr;
    }

    private void assembleRxFragments() {
        int dataSize = (totalFrag - 1) * FRAGMENT_DATA_MAX_SIZE;
        dataSize += fragments[totalFrag - 1].getData().length;

        byte d[] = new byte[dataSize];

        for (int i = 0; i < fragments.length; i++) {
            JcMsgFragmentData fr = fragments[i];
            System.arraycopy(fr.getData(), 0, d, i * FRAGMENT_DATA_MAX_SIZE, fr.getData().length);
        }
        rxData = d;
    }

    public synchronized boolean addFragmentReceivedData(JcMsgFragmentData frData) throws JcFragmentationException {
        int frIdx = frData.getSeq();
        if (fragments.length <= frIdx) {
            throw new JcFragmentationException("Received fragment idx out of bound!");
        }
        fragments[frIdx] = frData;
        rxFragCount++;

        //if last fragment, can be received not last
        // 
        if (rxFragCount == totalFrag) {
            assembleRxFragments();
            return true;
        }
        return false;
    }

    public String getFrgId() {
        return frgId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getTotalFrag() {
        return totalFrag;
    }

    public JcMsgFragmentData[] getFragments() {
        return fragments;
    }

    @Override
    public String toString() {
        return "JcMsgFragment{" + "frgId=" + frgId + ", timestamp=" + timestamp + ", totalFrag=" + totalFrag + '}';
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 41 * hash + Objects.hashCode(this.frgId);
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
        final JcMsgFragment other = (JcMsgFragment) obj;
        return Objects.equals(this.frgId, other.frgId);
    }

    public byte[] getRxData() {
        return rxData;
    }

    public int getRecoverCount() {
        return recoverCount;
    }

    public List<Integer> getMissingFragmenList() {
        recoverCount++;
        timestamp = System.currentTimeMillis();
        List<Integer> missing = new ArrayList<>();
        for (int i = 0; i < fragments.length; i++) {
            if (fragments[i] == null) {
                missing.add(i);
            }
        }
        return missing;
    }
}
