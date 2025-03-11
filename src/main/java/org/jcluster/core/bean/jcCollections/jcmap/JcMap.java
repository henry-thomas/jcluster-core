/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.bean.jcCollections.jcmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author henry
 */
public class JcMap<K, V> extends HashMap<K, V> {

    private static final long serialVersionUID = 7722387578958564823L;

    private final List<EntryAddedListener> entryAddedListenerList = new ArrayList<>();

    public void addEntryListener(EntryAddedListener listener) {
        entryAddedListenerList.add(listener);
    }

    @Override
    public V put(K key, V value) {
        V put = super.put(key, value);

//        if (!put.equals(value)) {
        EntryAddedEvent<K, V> ev = new EntryAddedEvent(Map.entry(key, value), EntryEventType.ADDED, put);
        for (EntryAddedListener entryAddedListener : entryAddedListenerList) {
            entryAddedListener.onEntryAdded(ev);
//            }
        }
        return put;
    }

}
