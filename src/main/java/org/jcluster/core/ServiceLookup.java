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
import org.jcluster.core.exception.JcException;
import org.jcluster.core.monitor.AppMetricMonitorInterface;
import org.jcluster.core.monitor.AppMetricsMonitor;
import org.jcluster.lib.annotation.JcRemote;

/**
 *
 * @autor Henry Thomas
 */
public class ServiceLookup {

    //<serviceName, <methodName, method>>
    private final Map<String, Object> localInterfaceInstanceMap = new HashMap<>();

    private final Map<String, Map<String, Method>> serviceMethodsMap = new HashMap<>();

    private final Map<String, Object> jndiLookupMap = new HashMap<>();
    private final Set<String> failSet = new HashSet<>();

    private static final ServiceLookup INSTANCE = new ServiceLookup();

    public static ServiceLookup getINSTANCE() {
        return INSTANCE;
    }

    protected final void registerLocalClassImplementation(Class clazz) throws JcException {
        try {
            Class iFaceClass = null;
            Class[] interfaces = clazz.getInterfaces();
            for (Class aInterface : interfaces) {
                if (aInterface.getAnnotation(JcRemote.class) != null) {
                    iFaceClass = aInterface;
                    break;
                }
            }
            if (iFaceClass == null) {
                throw new JcException("Can not find suitable interface for class: " + clazz.getName());
            }
            Constructor<?>[] declaredConstructors = clazz.getDeclaredConstructors();
            for (Constructor<?> constr : declaredConstructors) {
                Parameter[] parameters = constr.getParameters();
                if (parameters.length == 0) {
                    Object ob = constr.newInstance();
                    localInterfaceInstanceMap.put(iFaceClass.getName(), ob);
                    return;
                }
            }
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new JcException(ex.getMessage());
        }
        throw new JcException("Can not find suitable constructor for class: " + clazz.getName());
    }

    public static Object getService(String jndiName, String className) throws NamingException {
        Object ob = INSTANCE.localInterfaceInstanceMap.get(className);
        if (ob == null) {
            throw new NamingException("ServiceLookup can not find class instance [" + className + "]  Did you forget to load it?");
        }
        return ob;
    }

    public static Object getServiceEnterprise(String jndiName, String className) throws NamingException {
//        Object serviceObj = null;

        Object serviceObj = INSTANCE.jndiLookupMap.get(jndiName);

        if (serviceObj == null) {
            InitialContext ctx = new InitialContext();

            try {
                serviceObj = ctx.lookup(jndiName);
            } catch (NamingException e) {
                throw e;
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
