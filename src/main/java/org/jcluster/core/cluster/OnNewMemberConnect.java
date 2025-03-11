/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.cluster;

import org.jcluster.core.JcFactory;
import org.jcluster.core.JcManager;
import org.jcluster.core.bean.JcAppDescriptor;
import org.jcluster.core.bean.JcMemberEvent;
import org.jcluster.core.bean.JcMemerEventTypeEnum;
import org.jcluster.core.bean.jcCollections.jcmap.EntryAddedEvent;
import org.jcluster.core.bean.jcCollections.jcmap.EntryAddedListener;

/**
 *
 * @author henry
 */
public class OnNewMemberConnect implements EntryAddedListener<String, JcAppDescriptor> {

    @Override
    public void onEntryAdded(EntryAddedEvent<String, JcAppDescriptor> ev) {
        JcAppDescriptor value = (JcAppDescriptor) ev.getEntry().getValue();
        System.out.println("onEntryAdded" + value.getAppName());
        JcFactory.getManager().onNewMemberEvent(new JcMemberEvent(ev.getValue(), JcMemerEventTypeEnum.MEMBER_ADD));
    }

}
