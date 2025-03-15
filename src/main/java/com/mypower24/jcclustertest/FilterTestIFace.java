/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mypower24.jcclustertest;

import org.jcluster.lib.annotation.JcFilter;
import org.jcluster.lib.annotation.JcRemote;

/**
 *
 * @author henry
 */
@JcRemote(appName = "jcAppNameDefault")
public interface FilterTestIFace {

    public static String FILTER_TEST_1_FILTERNAME = "filter1";
    public static String FILTER_TEST_2_FILTERNAME = "filter2";

    public String getStringFilter1(@JcFilter(filterName = FILTER_TEST_1_FILTERNAME) String filter1);

    public String getStringFilter2(@JcFilter(filterName = FILTER_TEST_2_FILTERNAME) Integer filter2);
}
