/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Exception.java to edit this template
 */
package org.jcluster.core.exception.sockets;

import org.jcluster.core.exception.JcRuntimeException;
import org.jcluster.core.messages.JcMessage;

/**
 *
 * @author henry
 */
public class JcResponseTimeoutException extends JcRuntimeException {

    private JcMessage jcMessage;

    /**
     * Creates a new instance of <code>JcResponseTimeoutException</code> without
     * detail message.
     *
     * @param msg
     * @param jcMsg
     */
    public JcResponseTimeoutException(String msg, JcMessage jcMsg) {
        super(msg);
        this.jcMessage = jcMsg;
    }

    /**
     * Constructs an instance of <code>JcResponseTimeoutException</code> with
     * the specified detail message.
     *
     * @param msg the detail message.
     */
    public JcResponseTimeoutException(String msg) {
        super(msg);
    }

    public JcMessage getJcMessage() {
        return jcMessage;
    }

}
