/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.bean.jcCollections.jcmap;

import org.jcluster.core.bean.JcAppDescriptor;

/**
 *
 * @author henry
 */
public interface EntryAddedListener<K, V> {

    public void onEntryAdded(EntryAddedEvent<K, V> ev);
}
