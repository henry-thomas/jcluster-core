/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.bean.jcCollections;

import java.util.ArrayList;

/**
 *
 * @author platar86
 */
public class RingConcurentList<T> extends ArrayList<T> {

    private int ringIdx = 0;

    public T getNext() {
        if (isEmpty()) {
            return null;
        }
        synchronized (this) {
            if (ringIdx >= size()) {
                ringIdx = 0;
            }
            return get(ringIdx++);
        }
    }

    @Override
    public boolean add(T e) {
        synchronized (this) {
            return super.add(e); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
        }
    }

    @Override
    public T get(int index) {
        return super.get(index); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

}
