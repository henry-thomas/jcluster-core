/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.messages;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author henry
 */
public class JcMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long requestId;
    private final String methodSignature;
    private final String className;
    private final Object[] args; //arguments for method execution
    private JcMsgResponse response;
//    private final Object lock = new Object(); //Sync on lock

    private static final AtomicLong MSG_ID_INCR = new AtomicLong();

    public JcMessage(String methodSignature, String className, Object[] args) {
        this.methodSignature = methodSignature;
        this.className = className;
        this.args = args;
        this.requestId = MSG_ID_INCR.getAndIncrement();
    }

    public JcMsgResponse getResponse() {
        return response;
    }

    public void setResponse(JcMsgResponse response) {
        this.response = response;
    }

//    public static long getSerialVersionUID() {
//        return serialVersionUID;
//    }
    public long getRequestId() {
        return requestId;
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

//    public Object getLock() {
//        return lock;
//    }
}
