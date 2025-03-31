/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.proxy;

import ch.qos.logback.classic.Logger;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.jcluster.core.JcCoreService;
import org.jcluster.core.JcManager;
//import org.jcluster.core.config.JcAppConfig;
import org.slf4j.LoggerFactory;

/**
 *
 * @autor Henry Thomas
 */
public class JcRemoteInvocationHandler implements InvocationHandler, Serializable {

    private static final ch.qos.logback.classic.Logger LOG = (Logger) LoggerFactory.getLogger(JcCoreService.class);

    private final Map<String, JcProxyMethod> methodCache = new HashMap<>();

    private final Class iClazz;

    public JcRemoteInvocationHandler(Class iClazz) {
        this.iClazz = iClazz;
    }

    //caches different ways to call this method
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        JcProxyMethod proxyMethod = methodCache.get(method.getName());//contains info to send to correct App/Instance if specified
        if (proxyMethod == null) {
            proxyMethod = JcProxyMethod.initProxyMethod(method, args, iClazz);
            methodCache.put(method.getName(), proxyMethod);
        }
        long now = System.currentTimeMillis();

        Object response = JcManager.send(proxyMethod, args);

//        if (JcAppConfig.getINSTANCE().isDebug()) {
//            LOG.debug("exec [{0}] in [{1} ms]", new Object[]{method.getName(), System.currentTimeMillis() - now});
//        }
        if (response instanceof Exception) {
            LOG.error(((Exception) response).getMessage());
            throw ((Exception) response);
        }
        if (response instanceof Throwable) {
            LOG.error(((Throwable) response).getMessage());
            throw ((Throwable) response);
        }

        return response;

    }

}
