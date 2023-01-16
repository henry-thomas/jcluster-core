/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import org.jcluster.core.ServiceLookup;
import org.jcluster.core.messages.JcMessage;
import org.jcluster.core.messages.JcMsgResponse;

/**
 *
 * @autor Henry Thomas
 */
public class JcInboundMethodExecutor implements Runnable {

    private final JcClientConnection clientConn;
    private final JcMessage request;
    private static final Logger LOG = Logger.getLogger(JcInboundMethodExecutor.class.getName());

    public JcInboundMethodExecutor(JcMessage msg, JcClientConnection clientConn) {
        this.request = msg;
        this.clientConn = clientConn;
    }

    public void sendResponse(JcMsgResponse msg) {
        try {
            clientConn.writeAndFlushToOOS(msg);
        } catch (IOException ex) {
            Logger.getLogger(JcClientConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        String jndiName;
        try {
            switch (request.getMethodSignature()) {
                case "ping":
                    onPingRequest();
                    break;

                default:
                    jndiName = request.getJndiName();
                    Object service = ServiceLookup.getService(jndiName);

                    Method method = ServiceLookup.getINSTANCE().getMethod(service, request.getMethodSignature());

                    //Do work, then assign response here
                    Object result = method.invoke(service, request.getArgs()); //if method return type is void then result will be null,

                    //send back result or null for ACK
                    JcMsgResponse response = JcMsgResponse.createResponseMsg(request, result);
                    sendResponse(response);
            }
        } catch (NamingException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            LOG.log(Level.SEVERE, null, ex.getCause());
            JcMsgResponse response = JcMsgResponse.createResponseMsg(request, ex.getCause());
            sendResponse(response);
        }
    }

    private void onPingRequest() {
        JcMsgResponse response = JcMsgResponse.createResponseMsg(request, "pong");
        sendResponse(response);

//        LOG.log(Level.INFO, "Sending pong msgId: {0}", response.getRequestId());
    }

}
