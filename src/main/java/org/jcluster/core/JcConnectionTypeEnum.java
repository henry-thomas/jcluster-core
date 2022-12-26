/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package org.jcluster.core;

/**
 *
 * @autor Henry Thomas
 */
public enum JcConnectionTypeEnum {
    INBOUND,
    OUTBOUND;

    public JcConnectionTypeEnum getOpposite() {
        if (this == INBOUND) {
            return OUTBOUND;
        }
        return INBOUND;
    }
}
