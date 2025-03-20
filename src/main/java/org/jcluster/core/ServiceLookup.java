/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jcluster.core.exception.JcException;
import org.jcluster.lib.annotation.JcFilter;
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
//    private final Set<String> failSet = new HashSet<>();

    private static final ServiceLookup INSTANCE = new ServiceLookup();

    public static ServiceLookup getINSTANCE() {
        return INSTANCE;
    }

    protected final void registerLocalClassImplementation(Class clazz) throws JcException {
        try {
            Class iFaceClass = null;
            Class[] interfaces = clazz.getInterfaces();
             JcRemote annotation = null;
            for (Class aInterface : interfaces) {
                 annotation = (JcRemote) aInterface.getAnnotation(JcRemote.class);
                if (annotation != null) {
                    iFaceClass = aInterface;
                    break;
                }
            }
            if (iFaceClass == null) {
                throw new JcException("Can not find suitable interface for class: " + clazz.getName());
            }
            
            if(!annotation.topic().isEmpty()){
                JcCoreService.getInstance().getSelfDesc().getTopicList().add(annotation.topic());
            }
            
            Constructor<?>[] declaredConstructors = clazz.getDeclaredConstructors();
            for (Constructor<?> constr : declaredConstructors) {
                Parameter[] parameters = constr.getParameters();
                if (parameters.length == 0) {
                    Object ob = constr.newInstance();
                    localInterfaceInstanceMap.put(iFaceClass.getName(), ob);

                    //subscribe here if has not been done yet
//                    scanAnnotationFilters(iFaceClass);
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
                serviceObj = INSTANCE.localInterfaceInstanceMap.get(className);
                if (serviceObj == null) {
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

    protected void scanProxyAnnotationFilters(Class iClazz) {
        HashMap<String, Set<String>> hashMap = new HashMap<String, Set<String>>();
        
        JcRemote classAnnot = (JcRemote) iClazz.getAnnotation(JcRemote.class);

        for (Method method : iClazz.getMethods()) {

//            method.getParameters()
            for (Parameter param : method.getParameters()) {
                JcFilter parAnn = param.getAnnotation(JcFilter.class);
                if (parAnn != null) {
                    if (!classAnnot.appName().isEmpty()) {
                        JcCoreService.getInstance().subscribeToAppFilter(classAnnot.appName(), parAnn.filterName());
                    }
                    if (!classAnnot.topic().isEmpty()) {
                        JcCoreService.getInstance().subscribeToTopicFilter(classAnnot.topic(), parAnn.filterName());
                    }
                }
            }

        }
    }

    public static List<Class<?>> findImplementations(String packageName, Class<?> interfaceClass) {
        //com.mypower24.jcclustertest.FilterTestIFace
        List<Class<?>> implementations = new ArrayList<>();
        String path = packageName.replace('.', '/');

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL resource = classLoader.getResource(path);
            if (resource == null) {
                return implementations;
            }

            File directory = new File(resource.toURI());
            if (directory.exists()) {
                if (directory.getName().endsWith(".class")) {
                    String className = packageName.replace(".class", "");
                    Class<?> clazz = Class.forName(className);
                    if (interfaceClass.isAssignableFrom(clazz) && !clazz.isInterface()) {
                        implementations.add(clazz);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return implementations;
    }

}
