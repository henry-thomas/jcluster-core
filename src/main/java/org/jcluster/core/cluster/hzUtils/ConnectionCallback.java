/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.cluster.hzUtils;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryRemovedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcluster.core.bean.JcAppDescriptor;
import org.jcluster.core.cluster.JcFactory;

/**
 *
 * @author henry
 */
public class ConnectionCallback implements EntryAddedListener<String, JcAppDescriptor>, EntryRemovedListener<String, JcAppDescriptor>, EntryUpdatedListener<String, JcAppDescriptor> {

    private static final Logger LOG = Logger.getLogger(ConnectionCallback.class.getName());

    @Override
    public void entryAdded(EntryEvent<String, JcAppDescriptor> event) {
        JcFactory.getManager().onNewMemberJoin(event.getValue());
//        LOG.log(Level.INFO, "ConnectionCallback entryAdded() {0}", event.getKey());
    }

    @Override
    public void entryRemoved(EntryEvent<String, JcAppDescriptor> event) {
        JcFactory.getManager().onMemberLeave(event.getOldValue());
//        LOG.log(Level.INFO, "ConnectionCallback entryRemoved() {0}", event.getKey());
    }

    @Override
    public void entryUpdated(EntryEvent<String, JcAppDescriptor> event) {
//        LOG.log(Level.INFO, "ConnectionCallback entryUpdated() {0}", event.getKey());
    }

}
