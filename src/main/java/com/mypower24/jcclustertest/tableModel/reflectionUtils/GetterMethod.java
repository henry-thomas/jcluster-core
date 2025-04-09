/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypower24.jcclustertest.tableModel.reflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *
 * @author platar86
 */
public class GetterMethod {

    public final Method method;
    public final Field field;

    public GetterMethod(Field field, Object ob) {

        this.field = field;

        this.method = ReflectionUtils.getReflectGetterMethod(field, ob);
        if (this.method == null) {
            throw new RuntimeException("Can not find getterMethod for Object: " + ob.getClass().getName());
        }
    }

    public Method getMethod() {
        return method;
    }

    public String getFieldName() {
        return field.getName();
    }

    public Field getField() {
        return field;
    }

    public Object invoke(Object data) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return method.invoke(data, (Object[]) null);
    }

}
