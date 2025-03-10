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
public class EntryAddedEvent {

    private Map.Entry entry;
    private Object previousValue;
    private EntryEventType type;

    public EntryAddedEvent(Map.Entry entry, EntryEventType type, Object previousValue) {
        this.entry = entry;
        this.type = type;
        this.previousValue = previousValue;
    }

    public Object getPreviousValue() {
        return previousValue;
    }

    public EntryEventType getType() {
        return type;
    }

    public Map.Entry getEntry() {
        return entry;
    }

}
