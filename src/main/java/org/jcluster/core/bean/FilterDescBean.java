/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.bean;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author platar86
 */
public class FilterDescBean {

    private final String filterName; // String LOGGERCONNECTION_LWS_FILTER = "lvCon";
    private long transCount = 0l;
    private final Set<Object> valueSet = new HashSet<>();
    private final Set<String> subscriberList = new HashSet<>(); //MemberId

    public FilterDescBean(String filterName) {
        this.filterName = filterName;
    }

    public boolean addSubscirber(String memId) {
        return subscriberList.add(memId);
    }

    public boolean removeSubscirber(String memId) {
        return subscriberList.remove(memId);
    }

    public boolean addFilterValue(Object value) {
        transCount++;
        return valueSet.add(value);
    }

    public boolean removeFilterValue(Object value) {
        transCount++;
        return valueSet.remove(value);
    }

    public Set<Object> getValueSet() {
        return valueSet;
    }

    public long getTransCount() {
        return transCount;
    }

}
