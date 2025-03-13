/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private final Set<String> failSet = new HashSet<>();

    private static final ServiceLookup INSTANCE = new ServiceLookup();

    public static ServiceLookup getINSTANCE() {
        return INSTANCE;
    }

    public static Object getService(String jndiName, String className) throws NamingException {
//        Object serviceObj = null;

        //Figure it out
        return serviceObj;
    }

    public static Object getServiceEnterprise(String jndiName, String className) throws NamingException {
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

    private Object createClassWithReflection(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?>[] declaredConstructors = clazz.getDeclaredConstructors();
            for (Constructor<?> constr : declaredConstructors) {
                Parameter[] parameters = constr.getParameters();
                if (parameters.length == 0) {
                    Object unit = constr.newInstance();
                    return unit;
                }
            }
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassNotFoundException ex) {
            Logger.getLogger(ServiceLookup.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
