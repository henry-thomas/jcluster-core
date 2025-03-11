/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.messages;

/**
 *
 * @author henry
 */
public class JcDistMsgMetrics {

    private long rxPub;
    private long txPub;
    private long rxJoin;
    private long txJoin;
    private long txNewMember;
    private long rxNewMember;

    public long addRxPub() {
        return rxPub++;
    }

    public long addTxPub() {
        return txPub++;
    }

    public long addRxJoin() {
        return rxJoin++;
    }

    public long addTxJoin() {
        return txJoin++;
    }

    public long addTxNewMember() {
        return txNewMember++;
    }

    public long addRxNewMember() {
        return rxNewMember++;
    }

    public long getRxPub() {
        return rxPub;
    }

    public long getTxPub() {
        return txPub;
    }

    public long getRxJoin() {
        return rxJoin;
    }

    public long getTxJoin() {
        return txJoin;
    }

    public long getTxNewMember() {
        return txNewMember;
    }

    public long getRxNewMember() {
        return rxNewMember;
    }

}
