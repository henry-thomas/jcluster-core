/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Exception.java to edit this template
 */
package org.jcluster.core.exception;

/**
 *
 * @author henry
 */
public class JcException extends Exception{

    /**
     * Creates a new instance of <code>JcException</code> without detail
     * message.
     */
    public JcException() {
    }

    /**
     * Constructs an instance of <code>JcException</code> with the specified
     * detail message.
     *
     * @param msg the detail message.
     */
    public JcException(String msg) {
        super(msg);
    }
}
