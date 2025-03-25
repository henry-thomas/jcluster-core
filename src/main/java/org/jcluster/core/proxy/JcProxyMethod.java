/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.proxy;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jcluster.core.exception.JcRuntimeException;
import org.jcluster.lib.annotation.JcBroadcast;
import org.jcluster.lib.annotation.JcRemote;
import org.jcluster.lib.annotation.JcTimeout;
import org.jcluster.lib.annotation.JcFilter;

/**
 *
 * @autor Henry Thomas
 */
public class JcProxyMethod {

    private final String appName;
    private final String topicName;
    private final String className;
    private final String methodSignature;
    private boolean instanceFilter;
    private boolean global;
    private Integer timeout = null;
    private final boolean broadcast;
    private final Map<String, Integer> paramNameIdxMap = new HashMap<>(); //<>
    private final Class<?> returnType;

//    private static final List<String> appList = new ArrayList<>();
    private JcProxyMethod(String appName, Method method, boolean broadcast) {
        this(appName, method, broadcast, null);
    }

    private JcProxyMethod(String appName, Method method, boolean broadcast, String topicName) {
        this(appName, method, broadcast, topicName, method.getDeclaringClass().getName());
    }

    private JcProxyMethod(String appName, Method method, boolean broadcast, String topicName, String className) {
        this.appName = appName;
        this.className = className;
        String ms = method.getName();
        for (Parameter parameter : method.getParameters()) {
            ms += "," + parameter.getType().getSimpleName();
        }
        this.methodSignature = ms;
        this.returnType = method.getReturnType();
        this.broadcast = broadcast;
        this.topicName = topicName;
        this.global = (topicName == null && appName == null);
    }

    public String getAppName() {
        return appName;
    }

    public boolean isInstanceFilter() {
        return instanceFilter;
    }

    public boolean isTopic() {
        return topicName != null;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    private void addInstanceFilterParam(String paramName, Integer idx) {
        paramNameIdxMap.put(paramName, idx);
        instanceFilter = true;
    }

    public Map<String, Integer> getParamNameIdxMap() {
        return paramNameIdxMap;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public boolean isBroadcast() {
        return broadcast;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public String getTopicName() {
        return topicName;
    }

    public boolean isGlobal() {
        return global;
    }

    public String printFilters(Object args[]) {
        StringBuilder sb = new StringBuilder("Filters[ ");
        for (Map.Entry<String, Integer> entry : paramNameIdxMap.entrySet()) {
            sb
                    .append("{")
                    .append(entry.getKey())
                    .append("=")
                    .append(args[entry.getValue()])
                    .append("} ");
        }
        sb.append("]");
        return sb.toString();
    }

    public static JcProxyMethod initProxyMethod(Method method, Object[] args) {
        return initProxyMethod(method, args, null);
    }

    public static JcProxyMethod initProxyMethod(Method method, Object[] args, Class iClazz) {

        boolean broadcast = false;
        if (method.getAnnotation(JcBroadcast.class) != null) {
            broadcast = true;
        }

        JcRemote jcRemoteAnn = method.getDeclaringClass().getAnnotation(JcRemote.class);
        String className = method.getDeclaringClass().getName();
        if (jcRemoteAnn == null && iClazz != null) {
            jcRemoteAnn = (JcRemote) iClazz.getAnnotation(JcRemote.class);
            className = iClazz.getName();
        }
        if (jcRemoteAnn == null) {
            throw new JcRuntimeException("Can not find anotated interface for method " + method.getName());
        }

        String appName = jcRemoteAnn.appName();
        String topicName = jcRemoteAnn.topic();

        JcProxyMethod proxyMethod;
        if (!appName.isEmpty()) {
            proxyMethod = new JcProxyMethod(appName, method, broadcast, null, className);
        } else if (!topicName.isEmpty()) {
            proxyMethod = new JcProxyMethod(null, method, broadcast, topicName, className);
        } else {
            proxyMethod = new JcProxyMethod(null, method, broadcast, null, className);
        }

        Parameter[] parameters = method.getParameters();
        JcFilter instanceFilter = null;

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];

            instanceFilter = param.getAnnotation(JcFilter.class);
            //allow to use filtername from args if anotation does not have value
            if (instanceFilter != null) {
                proxyMethod.addInstanceFilterParam(instanceFilter.filterName(), i);
            }
        }

        JcTimeout jcTimeout = method.getAnnotation(JcTimeout.class);

        if (jcTimeout != null) {
            proxyMethod.timeout = jcTimeout.timeout();
        }

        return proxyMethod;
    }

    @Override
    public String toString() {
        return "JcProxyMethod{" + "appName=" + appName + ", topicName=" + topicName + ", className=" + className + ", methodSignature=" + methodSignature + ", timeout=" + timeout + '}';
    }

}
