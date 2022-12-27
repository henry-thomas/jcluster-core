/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
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
 * @autor Henry Thomas
 */
@LocalBean //required for glassfish
public class JcBootstrap implements Extension {

    private static final Logger LOG = Logger.getLogger(JcBootstrap.class.getName());

    public static final Set<String> appNameList = new HashSet<>();

    private Class<?> filterPackageClass(String className, List<String> pkgList) {
        for (String pkg : pkgList) {

            if (className.startsWith(pkg)) {
                try {
                    Class<?> forName = Class.forName(className);
                    if (forName.isInterface() && forName.getAnnotation(JcRemote.class) != null) {
                        return forName;
                    }

                } catch (Throwable ex) {
//                    LOG.severe(ex.getMessage());
                }
            }
        }
        return null;
    }

    private List<Class> getAllJcRemoteClasses(List<String> pkgList) {
        List<Class> classList = new ArrayList<>();
        try {
            ClassPath classPath = ClassPath.from(JcBootstrap.class.getClassLoader());
            Set<ClassInfo> classes = classPath.getAllClasses();
            for (ClassInfo c : classes) {
                Class<?> clazz = filterPackageClass(c.getName(), pkgList);
                if (clazz != null) {
                    classList.add(clazz);
                }

            }

        } catch (IOException ex) {
        }
        return classList;
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager manager) {
        LOG.info("JcBootstrap afterBeanDiscovery()");

        List<String> pkgFilterList = JcAppConfig.getINSTANCE().getPkgFilterList();

        long scanStart = System.currentTimeMillis();

        List<Class> jcRemoteInterfaceList = getAllJcRemoteClasses(pkgFilterList);

        long scanEnd = System.currentTimeMillis();
        LOG.log(Level.INFO, "JcBootstrap Scan for JcRemote annotation in {0}ms, found: {1}", new Object[]{scanEnd - scanStart, jcRemoteInterfaceList.size()});

        appNameList.clear();
        for (Class jcRClass : jcRemoteInterfaceList) {
            JcRemote jcRemoteAnn = (JcRemote) jcRClass.getAnnotation(JcRemote.class);
            if (jcRemoteAnn != null) {
                appNameList.add(jcRemoteAnn.appName());
            }

            Object newProxyInstance = Proxy.newProxyInstance(JcRemote.class.getClassLoader(), new Class[]{jcRClass}, new JcRemoteExecutionHandler());
            event.addBean().types(jcRClass).createWith(e -> newProxyInstance);
            LOG.log(Level.INFO, "JcBootstrap add Remote interface implementation for: [{0}]", new Object[]{jcRClass.getName()});
        }
        String allApps = "";
        for (String appName : appNameList) {
            allApps += appName + " ";
        }
        LOG.log(Level.INFO, "JcBootstrap Found Total Required APP: {0} =>  [ {1}]", new Object[]{appNameList.size(), allApps});

    }

}
