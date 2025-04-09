/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypower24.jcclustertest.tableModel;

import com.mypower24.jcclustertest.tableModel.reflectionUtils.GetterMethod;
import com.mypower24.jcclustertest.tableModel.reflectionUtils.ReflectionUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author platar86
 */
public class JTableModelDescription {

    private String colTitle;
    private String fieldName;
    private Method getter;
    private Class<?> returnType;

    public JTableModelDescription(String colTitle, String fieldName) {
        this.colTitle = colTitle;
        this.fieldName = fieldName;

    }

    public String getColTitle() {
        return colTitle;
    }

    public void setColTitle(String colTitle) {
        this.colTitle = colTitle;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public Object getValue(Object instance) {
        try {
            return getter.invoke(instance);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(JTableModelDescription.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public void initGetter(Class clz) {
        this.getter = ReflectionUtils.getReflectGetterMethod(fieldName, clz);
        returnType = getter.getReturnType();
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    
}
