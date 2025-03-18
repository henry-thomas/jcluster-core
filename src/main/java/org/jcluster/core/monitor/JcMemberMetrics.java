/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.monitor;

import java.io.Serializable;
import java.util.Map;

/**
 *
 * @author henry
 */
public class JcMemberMetrics implements Serializable {

    private static final long serialVersionUID = 3468516841763326041L;

    private Integer filterSize;
    private Map<String, Map<String, JcConnectionMetrics>> connMetrics;

    public Integer getFilterSize() {
        return filterSize;
    }

    public void setFilterSize(Integer filterSize) {
        this.filterSize = filterSize;
    }

    public Map<String, Map<String, JcConnectionMetrics>> getConnMetrics() {
        return connMetrics;
    }

    public void setConnMetrics(Map<String, Map<String, JcConnectionMetrics>> connMetrics) {
        this.connMetrics = connMetrics;
    }

}
