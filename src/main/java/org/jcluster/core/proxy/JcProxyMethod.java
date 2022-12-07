/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.proxy;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import org.jcluster.annotation.JcInstanceFilter;
import org.jcluster.annotation.JcRemote;

/**
 *
 * @author henry
 */
public class JcProxyMethod {

    private final String appName;
    private final String className;
    private final String methodName;
    private boolean instanceFilter;
    private final Map<String, Integer> paramNameIdxMap = new HashMap<>(); //<>
    private final Class<?> returnType;

    private JcProxyMethod(String appName, String remoteJndiName, String methodName, Class<?> returnType) {
        this.appName = appName;
        this.className = remoteJndiName;
        this.methodName = methodName;
        this.returnType = returnType;
    }

    public String getAppName() {
        return appName;
    }

    public boolean isInstanceFilter() {
        return instanceFilter;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public void addInstanceFilterParam(String paramName, Integer idx) {
        paramNameIdxMap.put(paramName, idx);
        instanceFilter = true;
    }

    public Map<String, Integer> getParamNameIdxMap() {
        return paramNameIdxMap;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public static JcProxyMethod initProxyMethod(Method method, Object[] args) {

        Class<?> returnType = method.getReturnType();
//        proxyMethod.returnType = returnType;

        JcRemote jcRemoteAnn = method.getDeclaringClass().getAnnotation(JcRemote.class);
        String appName = "unknown";
        if (jcRemoteAnn != null) {
            appName = jcRemoteAnn.appName();
        }

        String className = method.getDeclaringClass().getName();

        JcProxyMethod proxyMethod = new JcProxyMethod(appName, className, method.getName(), returnType);

        Parameter[] parameters = method.getParameters();
        JcInstanceFilter instanceFilter = null;

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            instanceFilter = param.getAnnotation(JcInstanceFilter.class);

            if (instanceFilter != null) {
                proxyMethod.addInstanceFilterParam(instanceFilter.filterName(), i);
            }
        }

        return proxyMethod;
    }

}
