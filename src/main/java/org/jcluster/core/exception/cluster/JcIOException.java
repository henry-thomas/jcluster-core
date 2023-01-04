/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Exception.java to edit this template
 */
package org.jcluster.core.exception.cluster;

import org.jcluster.core.exception.JcRuntimeException;

/**
 *
 * @autor Henry Thomas
 */
public class JcIOException extends JcRuntimeException {

    /**
     * Creates a new instance of <code>JcInstanceNotFoundException</code>
     * without detail message.
     */
    public JcIOException() {
    }

    /**
     * Constructs an instance of <code>JcInstanceNotFoundException</code> with
     * the specified detail message.
     *
     * @param msg the detail message.
     */
    public JcIOException(String msg) {
        super(msg);
    }
}
