/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jcluster.core.monitor.AppMetricMonitorInterface;
import org.jcluster.core.monitor.AppMetricsMonitor;

/**
 *
 * @autor Henry Thomas
 */
public class ServiceLookup {

    //<serviceName, <methodName, method>>
    private final Map<String, Map<String, Method>> serviceMethodsMap = new HashMap<>();

    private final Map<String, Object> jndiLookupMap = new HashMap<>();

    private static final ServiceLookup INSTANCE = new ServiceLookup();

    public static ServiceLookup getINSTANCE() {
        return INSTANCE;
    }

    public static Object getService(String jndiName) throws NamingException {
        return getService(jndiName, null);
    }

    public static Object getService(String jndiName, String className) throws NamingException {
//        Object serviceObj = null;

        Object serviceObj = INSTANCE.jndiLookupMap.get(jndiName);

        if (serviceObj == null) {
            InitialContext ctx = new InitialContext();

            try {
                serviceObj = ctx.lookup(jndiName);
            } catch (NamingException e) {
                if (className.equals(AppMetricMonitorInterface.class.getName())) {
                    serviceObj = AppMetricsMonitor.getInstance();
                } else {
                    throw e;
                }
            }

            INSTANCE.jndiLookupMap.put(jndiName, serviceObj);
        }

        return serviceObj;
    }

    public Method getMethod(Object service, String methodSignature) {
        Map<String, Method> methodMap = serviceMethodsMap.get(service.getClass().getName());

        if (methodMap == null) {
            methodMap = new HashMap<>();

            for (Method method : service.getClass().getMethods()) {

                String ms = method.getName();
                for (Parameter parameter : method.getParameters()) {
                    ms += "," + parameter.getType().getSimpleName();
                }
                methodMap.put(ms, method);
            }

            serviceMethodsMap.put(service.getClass().getName(), methodMap);
        }
        return methodMap.get(methodSignature);
    }

}
