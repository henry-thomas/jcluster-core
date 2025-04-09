/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypower24.jcclustertest.tableModel.reflectionUtils;

/**
 *
 * @author platar86
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.lang.reflect.Field;

import java.util.List;

/**
 *
 * @author platar86
 */
public class ReflectionUtils {

    public static String getMethodName(String fieldName, String mthPrefix) {
        if (fieldName.length() > 1 && Character.isUpperCase(fieldName.charAt(1))) {
            return mthPrefix + fieldName;
        }
        return mthPrefix + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

    public static String getGetterMethodName(Field field) {
        String fieldName = field.getName();
        String prefix = (field.getType() == Boolean.class || field.getType() == boolean.class) ? "is" : "get";
        if (fieldName.length() > 1 && Character.isUpperCase(fieldName.charAt(1))) {
            return prefix + fieldName;
        }
        return prefix + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

    public static String getSetterMethodName(Field field) {
        String fieldName = field.getName();
        if (fieldName.length() > 1 && Character.isUpperCase(fieldName.charAt(1))) {
            return "set" + fieldName;
        }
        return "set" + fieldName.charAt(0) + fieldName.substring(1);
    }

    public static Method getReflectGetterMethod(String fieldName, Class clz) {
        Field field = findField(fieldName, clz);
        

        String getterMethodName = (field.getType() == Boolean.class || field.getType() == boolean.class) ? "is" : "get";
        getterMethodName += field.getName().substring(0, 1).toUpperCase();
        getterMethodName += field.getName().substring(1);

        try {
            Method getter = clz.getMethod(getterMethodName);
            return getter;
        } catch (NoSuchMethodException | SecurityException ex) {
            Logger.getLogger(ReflectionUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static Method getReflectGetterMethod(Field field, Object pojo) {
        String getterMethodName = (field.getType() == Boolean.class || field.getType() == boolean.class) ? "is" : "get";
        getterMethodName += field.getName().substring(0, 1).toUpperCase();
        getterMethodName += field.getName().substring(1);

        try {
            Method getter = pojo.getClass().getMethod(getterMethodName);
            return getter;
        } catch (NoSuchMethodException | SecurityException ex) {
            Logger.getLogger(ReflectionUtils.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    public static Object getPogoValue(String fieldName, Object pojo) throws ReflectiveOperationException {
        return getPogoValue(fieldName, pojo, false);
    }

    public static Object getPogoValue(String fieldName, Object pojo, boolean checkSuperClasses) throws ReflectiveOperationException {

        String getterName = getMethodName(fieldName, "get");
        Method getMethod = null;
        try {
            if (checkSuperClasses) {
                getMethod = pojo.getClass().getMethod(getterName);
            } else {
                getMethod = pojo.getClass().getDeclaredMethod(getterName);
            }
        } catch (NoSuchMethodException e) {
            getterName = getMethodName(fieldName, "is");
            if (checkSuperClasses) {
                getMethod = pojo.getClass().getMethod(getterName);
            } else {
                getMethod = pojo.getClass().getDeclaredMethod(getterName);
            }
        }

        return getMethod.invoke(pojo);
    }

    public static GetterMethod getPojoGetterMethod(Field field, Object pojo) {
        String getterMethodName = (field.getType() == Boolean.class) ? "is" : "get";
        getterMethodName += field.getName().substring(0, 1).toUpperCase();
        getterMethodName += field.getName().substring(1);

        try {
//            Method getter = pojo.getClass().getMethod(getterMethodName);
            return new GetterMethod(field, pojo);
        } catch (SecurityException ex) {
            Logger.getLogger(ReflectionUtils.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    public static Field[] getAllFields(Object ob) {
        List<Field> allFields = new ArrayList<>();
        findFields(allFields, ob.getClass());
        return allFields.toArray(new Field[allFields.size()]);
    }

    private static Field findField(String fieldName, Class clz) {
        Field[] fieldArr = clz.getDeclaredFields();
        for (Field field : fieldArr) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }

        if (clz.getSuperclass() != null) {
            return findField(fieldName, clz.getSuperclass());
        }
        throw new RuntimeException("Can not find field: [" + fieldName + "] in class: [" + clz.getName() + "]");
    }

    private static void findFields(List<Field> fList, Class ob) {
        Field[] fieldArr = ob.getDeclaredFields();
        for (Field field : fieldArr) {
            fList.add(field);
        }

        if (ob.getSuperclass() != null) {
            findFields(fList, ob.getSuperclass());
        }
    }

}
