/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Exception.java to edit this template
 */
package org.jcluster.core.exception.sockets;

import javax.naming.NamingException;

/**
 *
 * @author henry
 */
public class JcNamingException extends NamingException{

    /**
     * Creates a new instance of <code>JcMethodNotFoundException</code> without
     * detail message.
     */
    public JcNamingException() {
    }

    /**
     * Constructs an instance of <code>JcMethodNotFoundException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public JcNamingException(String msg) {
        super(msg);
    }
}
