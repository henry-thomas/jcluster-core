/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.messages;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @autor Henry Thomas
 */
public class JcMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long requestId;
    private final String methodSignature;
    private final String className;
    private final Object[] args; //arguments for method execution
    private JcMsgResponse response;

    private static final AtomicLong MSG_ID_INCR = new AtomicLong();

    public JcMessage(String methodSignature, Object[] args) {
        this(methodSignature, null, args);
    }

    public JcMessage(String methodSignature, String className, Object[] args) {
        this.methodSignature = methodSignature;
        this.className = className;
        this.args = args;
        this.requestId = MSG_ID_INCR.getAndIncrement();
    }

    public static JcMessage createPingMsg() {
        return new JcMessage("ping", null, null);
    }

    public JcMsgResponse getResponse() {
        return response;
    }

    public void setResponse(JcMsgResponse response) {
        this.response = response;
    }

    public long getRequestId() {
        return requestId;
    }

    public String getJndiName() {
        return className + "#" + className;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public String getClassName() {
        return className;
    }

    public Object[] getArgs() {
        return args;
    }

}
