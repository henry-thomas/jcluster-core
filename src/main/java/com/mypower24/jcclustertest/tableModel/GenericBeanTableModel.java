/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypower24.jcclustertest.tableModel;

import ch.qos.logback.classic.Logger;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.slf4j.LoggerFactory;

/**
 *
 * @author platar86
 * @param <T>
 */
public abstract class GenericBeanTableModel<T> extends AbstractTableModel {

    private List<T> items;
    private List<JTableModelDescription> dataDesc = new ArrayList<>();
    protected Logger LOG = (Logger) LoggerFactory.getLogger(this.getClass().getName());

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
        try {

            Class<? extends Object> genericClass = getGenericClass();
            for (JTableModelDescription desc : dataDesc) {
                desc.initGetter(genericClass);
            }
        } catch (Exception e) {
            LOG.warn(null, e);
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

    public T getListItem(int idx) {
        return items.get(idx);
    }

    public void add(T item) {
        items.add(item);
        int row = items.indexOf(item);
        fireTableRowsInserted(row, row);
    }

    public void add(List<T> itemList) {
        for (T item : itemList) {
            items.add(item);
            int row = items.indexOf(item);
            fireTableRowsInserted(row, row);
        }
    }

    public void clear() {
        int size = items.size();
        items.clear();
        fireTableRowsDeleted(0, size);
    }

    public void remove(T item) {

        if (items.contains(item)) {
            int row = items.indexOf(item);
            items.remove(row);
            fireTableRowsDeleted(row, row);
        }
    }

    public void createRowFilter() {

//        //RowFilter has two formal type parameters that allow you to create a RowFilter for a specific model. For example, the following assumes a specific model that is wrapping objects of type Person. Only Persons with an age over 20 will be shown:
//        RowFilter<T, String> ageFilter = new RowFilter<T, String>() {
//            public boolean include(Entry<? extends T, ? extends String> entry) {
//                T personModel = entry.getModel();
//                Person person = personModel.getPerson(entry.getIdentifier());
//              
//                // Age is <= 20, don't show it.
//                return false;
//            }
//        };
//      
//        TableRowSorter<T> sorter = new TableRowSorter<T>(this);
//        sorter.setRowFilter(ageFilter);

    }

}
