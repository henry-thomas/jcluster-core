/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.proxy;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcluster.core.JcFactory;
import org.jcluster.core.config.JcAppConfig;

/**
 *
 * @autor Henry Thomas
 */
public class JcRemoteExecutionHandler implements InvocationHandler, Serializable {

    private static final Logger LOG = Logger.getLogger(JcRemoteExecutionHandler.class.getName());

    private final Map<String, JcProxyMethod> methodCache = new HashMap<>();

    public JcRemoteExecutionHandler() {

    }

    //caches different ways to call this method
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        JcProxyMethod proxyMethod = methodCache.get(method.getName());//contains info to send to correct App/Instance if specified
        if (proxyMethod == null) {
            proxyMethod = JcProxyMethod.initProxyMethod(method, args);
            methodCache.put(method.getName(), proxyMethod);
        }
        long now = System.currentTimeMillis();

        Object response = JcFactory.getManager().send(proxyMethod, args);

        if (JcAppConfig.getINSTANCE().isDebug()) {
            LOG.log(Level.INFO, "exec [{0}] in [{1} ms]", new Object[]{method.getName(), System.currentTimeMillis() - now});
        }

        if (response instanceof Exception) {
            LOG.severe(((Exception) response).getMessage());
            throw ((Exception) response);
        }

        return response;

    }

}
