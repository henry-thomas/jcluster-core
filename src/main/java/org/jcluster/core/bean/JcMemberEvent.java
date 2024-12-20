/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.bean;

/**
 *
 * @author platar86
 */
public class JcMemberEvent {

    private final JcAppDescriptor appDescriptor;
    private final JcMemerEventTypeEnum eventType;

    public JcMemberEvent(JcAppDescriptor appDescriptor, JcMemerEventTypeEnum eventType) {
        this.appDescriptor = appDescriptor;
        this.eventType = eventType;
    }

    public JcAppDescriptor getAppDescriptor() {
        return appDescriptor;
    }

    public JcMemerEventTypeEnum getEventType() {
        return eventType;
    }

}
