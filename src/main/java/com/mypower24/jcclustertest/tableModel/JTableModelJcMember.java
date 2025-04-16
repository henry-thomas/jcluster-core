/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypower24.jcclustertest.tableModel;

import org.jcluster.core.JcMember;

/**
 *
 * @author platar86
 */
public class JTableModelJcMember extends GenericBeanTableModel<JcMember> {

    private final String columnNameArr[] = new String[]{"ID", "App", "Title"};

    public JTableModelJcMember() {
//        super();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        JcMember mem = getListItem(rowIndex);
        switch (columnNameArr[columnIndex]) {
            case "ID": {
                return mem.getDesc().getInstanceId();
            }
            case "App": {
                return mem.getDesc().getAppName();
            }
            case "Title": {
                return mem.getDesc().getTitle();
            }
            default:
                return "Error";
        }
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnNameArr[columnIndex];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public int getColumnCount() {
        return columnNameArr.length;
    }

}
