/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.LocalBean;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import org.jcluster.core.config.JcAppConfig;
import org.jcluster.lib.annotation.JcRemote;
import org.jcluster.core.proxy.JcRemoteExecutionHandler;

/**
 *
 * @author henry
 */
@LocalBean //required for glassfish
public class JcBootstrap implements Extension {

    private static final Logger LOG = Logger.getLogger(JcBootstrap.class.getName());

    private List<Class> getAllJcRemoteClasses(String pkg) {
        List<Class> classList = new ArrayList<>();
        try {
            ClassPath classPath = ClassPath.from(JcBootstrap.class.getClassLoader());
            Set<ClassInfo> classes = classPath.getAllClasses();
            for (ClassInfo c : classes) {
                if (c.getName().startsWith(pkg)) {
                    try {
                        Class<?> forName = Class.forName(c.getName());
                        if (forName.isInterface() && forName.getAnnotation(JcRemote.class) != null) {
//                        System.out.println("JcRemote Class: " + forName.getName() + " implementation loaded");
                            classList.add(forName);
                        }

                    } catch (Throwable ex) {
//                    LOG.severe(ex.getMessage());
                    }
                }
            }

        } catch (IOException ex) {
        }
        return classList;
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager manager) {
        LOG.info("JcBootstrap afterBeanDiscovery()");

        String scanPackageName = JcAppConfig.getINSTANCE().getScanPackageName();
        
        long scanStart = System.currentTimeMillis();
        
        
        List<Class> jcRemoteInterfaceList = getAllJcRemoteClasses(scanPackageName);

        long scanEnd = System.currentTimeMillis();
        LOG.log(Level.INFO, "JcBootstrap Scan for JcRemote annotation in {0}ms, found: {1}", new Object[]{scanEnd - scanStart, jcRemoteInterfaceList.size()});

        for (Class jcRClass : jcRemoteInterfaceList) {
            Object newProxyInstance = Proxy.newProxyInstance(JcRemote.class.getClassLoader(), new Class[]{jcRClass}, new JcRemoteExecutionHandler());
            event.addBean().types(jcRClass).createWith(e -> newProxyInstance);
            LOG.log(Level.INFO, "JcBootstrap add Remote interface implementation for: [{0}]", new Object[]{jcRClass.getName()});
        }

    }

}
