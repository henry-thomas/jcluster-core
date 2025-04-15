/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.messages;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author platar86
 */
public class PublishMsg implements Serializable {

    public static final int OPER_TYPE_UNKNOWN = 0;
    public static final int OPER_TYPE_ADD = 1;
    public static final int OPER_TYPE_REMOVE = 2;
    public static final int OPER_TYPE_ADDBULK = 3;
    public static final int OPER_TYPE_REMOVEBULK = 4;
    public static final int OPER_TYPE_SUBSCR_STAT_RESP = 5;
    public static final int OPER_TYPE_INVALID_FNAME = -1;

    private String filterName;
    public int operationType;

    private long transCount = 0l;
    private final Set<Object> valueSet = new HashSet<>();
    private Object value;

    public Set<Object> getValueSet() {
        return valueSet;
    }

    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    public int getOperationType() {
        return operationType;
    }

    public void setOperationType(int operationType) {
        this.operationType = operationType;
    }

    public long getTransCount() {
        return transCount;
    }

    public void setTransCount(long transCount) {
        this.transCount = transCount;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "PublishMsg{" + "filterName=" + filterName + ", transCount=" + transCount + ", valueSet=" + valueSet + ", value=" + value + '}';
    }

}
