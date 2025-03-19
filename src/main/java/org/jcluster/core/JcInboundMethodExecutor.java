/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import org.jcluster.core.messages.JcMessage;
import org.jcluster.core.messages.JcMsgResponse;
import org.jcluster.core.monitor.MethodExecMetric;

/**
 *
 * @autor Henry Thomas
 */
public class JcInboundMethodExecutor implements Runnable {

    private final JcClientConnection clientConn;
    private final JcMessage request;
    private final boolean enterprise;
    private static final Logger LOG = Logger.getLogger(JcInboundMethodExecutor.class.getName());

    public JcInboundMethodExecutor(JcMessage msg, JcClientConnection clientConn) {
        this(msg, clientConn, false);
    }

    public JcInboundMethodExecutor(JcMessage msg, JcClientConnection clientConn, boolean enterprise) {
        this.request = msg;
        this.clientConn = clientConn;
        this.enterprise = enterprise;
    }

    public void sendResponse(JcMsgResponse msg) {
        try {
            clientConn.writeAndFlushToOOS(msg);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Attempt to send response for: {0} to [{1}] FAILED", new Object[]{request.getMethodSignature(), clientConn.getConnId()});
            Logger.getLogger(JcClientConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        String jndiName;
        try {

            jndiName = request.getJndiName();
            Object service;

            if (enterprise) {
                service = ServiceLookup.getServiceEnterprise(jndiName, request.getClassName());
            } else {
                service = ServiceLookup.getService(jndiName, request.getClassName());
            }

            Method method = ServiceLookup.getINSTANCE().getMethod(service, request.getMethodSignature());

            Map<String, MethodExecMetric> execMetricMap = clientConn.getMetrics().getInbound().getMethodExecMap();
//            JcMemberMetricsInOut inbound = JcCoreService.getInstance().getAllMetrics().getSelfMetrics().getInbound().getMethodExecMap();
            MethodExecMetric execMetric = execMetricMap.get(method.getClass().getSimpleName() + "." + request.getMethodSignature());
            if (execMetric == null) {
                execMetric = new MethodExecMetric();
                execMetricMap.put(method.getDeclaringClass().getSimpleName() + "." + request.getMethodSignature(), execMetric);
            }

            long start = System.currentTimeMillis();

            //Do work, then assign response here
            Object result = method.invoke(service, request.getArgs()); //if method return type is void then result will be null,
            execMetric.setLastExecTime(System.currentTimeMillis() - start);

            //send back result or null for ACK
            JcMsgResponse response = JcMsgResponse.createResponseMsg(request, result);
            sendResponse(response);

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
