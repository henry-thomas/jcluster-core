/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.messages;

import java.io.Serializable;
import java.util.List;

/**
 *
 * @author platar86
 */
public class JcMsgFrgResendReq implements Serializable {

    private final String frgId;
    private final List<Integer> resendFrIdx;

    public JcMsgFrgResendReq(String frgId, List<Integer> resendFrIdx) {
        this.frgId = frgId;
        this.resendFrIdx = resendFrIdx;
    }

    public String getFrgId() {
        return frgId;
    }

    public List<Integer> getResendFrIdx() {
        return resendFrIdx;
    }

}
