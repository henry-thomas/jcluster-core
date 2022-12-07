/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.bean;

/**
 *
 * @author henry
 */
public class JcConnectionMetrics {

    private final String connId;
    private int txCount = 0;
    private int rxCount = 0;
    private int errCount = 0;
    private int timeoutCount = 0;

    public JcConnectionMetrics(String connId) {
        this.connId = connId;
    }

    public void incTxCount() {
        ++txCount;
    }

    public void incRxCount() {
        ++rxCount;
    }

    public void incErrCount() {
        ++errCount;
    }

    public void incTimeoutCount() {
        ++timeoutCount;
    }

    public int getTxCount() {
        return txCount;
    }

    public int getRxCount() {
        return rxCount;
    }

    public int getErrCount() {
        return errCount;
    }

    public int getTimeoutCount() {
        return timeoutCount;
    }

    public String getConnId() {
        return connId;
    }

}
