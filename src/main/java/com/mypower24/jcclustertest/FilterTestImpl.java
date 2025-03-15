/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypower24.jcclustertest;

/**
 *
 * @author henry
 */
public class FilterTestImpl implements FilterTestIFace {

    @Override
    public String getStringFilter1(String filter1) {
        return "Response for request: " + filter1;
    }

    @Override
    public String getStringFilter2(Integer filter2) {
        return String.valueOf(filter2 * 1000);
    }

}
