/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypower24.jcclustertest.tableModel;

import org.jcluster.core.bean.JcAppDescriptor;

/**
 *
 * @author platar86
 */
public class JTableModelJcAppDesc extends GenericBeanTableModel<JcAppDescriptor> {

    public JTableModelJcAppDesc() {
        super(
                new JTableModelDescription("App name", "appName"),
                new JTableModelDescription("Title", "title")
        );
    }

}
