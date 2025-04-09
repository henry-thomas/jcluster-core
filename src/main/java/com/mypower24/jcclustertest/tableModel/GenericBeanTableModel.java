/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypower24.jcclustertest.tableModel;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author platar86
 * @param <T>
 */
public abstract class GenericBeanTableModel<T> extends AbstractTableModel {

    private List<T> items;
    private List<JTableModelDescription> dataDesc = new ArrayList<>();

    public GenericBeanTableModel(JTableModelDescription... model) {
        this(new ArrayList<>(), model);
    }

    public GenericBeanTableModel(List<T> items, JTableModelDescription... model) {
        this.items = items;
        for (int i = 0; i < model.length; i++) {
            dataDesc.add(model[i]);
        }
        initGetters();
    }

    private void initGetters() {
        Class<? extends Object> genericClass = getGenericClass();
        for (JTableModelDescription desc : dataDesc) {
            desc.initGetter(genericClass);
        }
    }

    private Class getGenericClass() {
        ParameterizedType unitClassParameterized = (ParameterizedType) this.getClass().getGenericSuperclass();
        Type uClass = unitClassParameterized.getActualTypeArguments()[0];
        return (Class) uClass;
    }

    @Override
    public int getRowCount() {
        return items.size();
    }

    @Override
    public int getColumnCount() {
        return dataDesc.size();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        JTableModelDescription desc = dataDesc.get(columnIndex);
        return desc.getReturnType();
    }

    @Override
    public String getColumnName(int columnIndex) {
        JTableModelDescription desc = dataDesc.get(columnIndex);
        return desc.getColTitle();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        T item = items.get(rowIndex);
        JTableModelDescription desc = dataDesc.get(columnIndex);
        return desc.getValue(item);
    }

    public void add(T item) {
        items.add(item);
        int row = items.indexOf(item);
        fireTableRowsInserted(row, row);
    }

    public void remove(T item) {
        if (items.contains(item)) {
            int row = items.indexOf(item);
            items.remove(row);
            fireTableRowsDeleted(row, row);
        }
    }

}
