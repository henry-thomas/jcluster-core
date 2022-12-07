/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 *
 * @author henry
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
//        Object serviceObj = null;

        Object serviceObj = INSTANCE.jndiLookupMap.get(jndiName);

        if (serviceObj == null) {
            InitialContext ctx = new InitialContext();
            serviceObj = ctx.lookup(jndiName);

            INSTANCE.jndiLookupMap.put(jndiName, serviceObj);
        }

        return serviceObj;
    }

    public Method getMethod(Object service, String methodName) {
        Map<String, Method> methodMap = serviceMethodsMap.get(service.getClass().getName());

        if (methodMap == null) {
            methodMap = new HashMap<>();
            for (Method method : service.getClass().getMethods()) {
                methodMap.put(method.getName(), method);
            }
            serviceMethodsMap.put(service.getClass().getName(), methodMap);
        }
        return methodMap.get(methodName);
    }

}
