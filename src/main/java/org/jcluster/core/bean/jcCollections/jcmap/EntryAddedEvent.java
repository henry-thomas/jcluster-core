/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.bean.jcCollections.jcmap;

import java.util.Map;

/**
 *
 * @author henry
 */
public class EntryAddedEvent<K, V> {

    private final Map.Entry<K, V> entry;
    private final V previousValue;
    private final V value;
    private final EntryEventType type;

    public EntryAddedEvent(Map.Entry<K, V> entry, EntryEventType type, V previousValue) {
        this.entry = entry;
        this.value = entry.getValue();
        this.type = type;
        this.previousValue = previousValue;
    }

    public V getPreviousValue() {
        return previousValue;
    }

    public EntryEventType getType() {
        return type;
    }

    public Map.Entry<K, V> getEntry() {
        return entry;
    }

    public V getValue() {
        return value;
    }

}
