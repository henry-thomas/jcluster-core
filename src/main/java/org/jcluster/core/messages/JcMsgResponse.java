/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.messages;

import java.io.Serializable;

/**
 *
 * @autor Henry Thomas
 */
public class JcMsgResponse implements Serializable {

    private final long requestId;
    private final Object data;

    public static JcMsgResponse createResponseMsg(JcMessage request, Object data) {
        JcMsgResponse response = new JcMsgResponse(request.getRequestId(), data);
        request.setResponse(response);
        return response;
    }

    public JcMsgResponse(long requestId, Object data) {
        this.requestId = requestId;
        this.data = data;
    }

    public long getRequestId() {
        return requestId;
    }

    public Object getData() {
        return data;
    }

}
