/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Exception.java to edit this template
 */
package org.jcluster.core.exception;

/**
 *
 * @autor Henry Thomas
 */
public class JcRuntimeException extends RuntimeException{

    /**
     * Creates a new instance of <code>JcRuntimeException</code> without detail
     * message.
     */
    public JcRuntimeException() {
    }

    /**
     * Constructs an instance of <code>JcRuntimeException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public JcRuntimeException(String msg) {
        super(msg);
    }
    public JcRuntimeException(Throwable e) {
        super(e.getMessage());
        setStackTrace(e.getStackTrace());
    }
}
