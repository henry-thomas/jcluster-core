/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.cluster;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryRemovedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;
import java.util.logging.Logger;
import org.jcluster.core.bean.JcAppDescriptor;
import org.jcluster.core.bean.JcMeberEvent;
import org.jcluster.core.bean.JcMemerEventTypeEnum;

/**
 *
 * @autor Henry Thomas
 */
public class JcHzConnectionListener implements EntryAddedListener<String, JcAppDescriptor>, EntryRemovedListener<String, JcAppDescriptor>, EntryUpdatedListener<String, JcAppDescriptor> {

    private static final Logger LOG = Logger.getLogger(JcHzConnectionListener.class.getName());

    @Override
    public void entryAdded(EntryEvent<String, JcAppDescriptor> event) {
        JcFactory.getManager().onNewMemberEvent(new JcMeberEvent(event.getValue(), JcMemerEventTypeEnum.MEMBER_ADD));
//        LOG.log(Level.INFO, "ConnectionCallback entryAdded() {0}", event.getKey());
    }

    @Override
    public void entryRemoved(EntryEvent<String, JcAppDescriptor> event) {
        JcFactory.getManager().onNewMemberEvent(new JcMeberEvent(event.getValue(), JcMemerEventTypeEnum.MEMBER_REMOVE));
//        LOG.log(Level.INFO, "ConnectionCallback entryRemoved() {0}", event.getKey());
    }

    @Override
    public void entryUpdated(EntryEvent<String, JcAppDescriptor> event) {
//          JcFactory.getManager().onNewMemberEvent(new JcMeberEvent(event.getValue(), JcMemerEventEnum.MEMBER_UPDATE));
//        LOG.log(Level.INFO, "ConnectionCallback entryUpdated() {0}", event.getKey());
    }

}
