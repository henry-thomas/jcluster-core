/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.sockets;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import org.jcluster.core.ServiceLookup;
import org.jcluster.core.exception.sockets.JcMethodNotFoundException;
import org.jcluster.core.messages.JcMessage;
import org.jcluster.core.messages.JcMsgResponse;

/**
 *
 * @author henry
 */
public class MethodExecutor implements Runnable {

    private final ObjectOutputStream oos;
    private final JcMessage request;
    private static final Logger LOG = Logger.getLogger(MethodExecutor.class.getName());

    public MethodExecutor(ObjectOutputStream oos, JcMessage msg) {
        this.oos = oos;
        this.request = msg;
    }

    public void sendAck(JcMessage msg) {
        try {

            synchronized (oos) {
                oos.writeObject(msg);
            }

        } catch (IOException ex) {
            Logger.getLogger(JcClientConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        JcMsgResponse response;
        String jndiName;
        try {
            if (request.getMethodSignature().equals("ping")) {
                handlePing();
                return;
            }

            jndiName = request.getClassName() + "#" + request.getClassName();
            Object service;

            try {
                service = ServiceLookup.getService(jndiName);
            } catch (NamingException ex) {
                response = new JcMsgResponse(request.getRequestId(), (JcMethodNotFoundException) ex);
                request.setResponse(response);
                sendAck(request);
                Logger.getLogger(JcClientConnection.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }

            Method method = ServiceLookup.getINSTANCE().getMethod(service, request.getMethodSignature());
            Object result = method.invoke(service, request.getArgs());

            //Do work, then assign response here
            response = new JcMsgResponse(request.getRequestId(), result);
            request.setResponse(response);
//            LOG.info("Sending response...");
            sendAck(request);

        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(JcClientConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void handlePing() {
        JcMsgResponse response = new JcMsgResponse(request.getRequestId(), "pong");
        request.setResponse(response);
        sendAck(request);
    }

}
