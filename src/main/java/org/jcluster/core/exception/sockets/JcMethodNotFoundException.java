/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Exception.java to edit this template
 */
package org.jcluster.core.exception.sockets;

/**
 *
 * @author henry
 */
public class JcMethodNotFoundException extends JcNamingException{

    /**
     * Creates a new instance of <code>JcMethodNotFoundException</code> without
     * detail message.
     */
    public JcMethodNotFoundException() {
    }

    /**
     * Constructs an instance of <code>JcMethodNotFoundException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public JcMethodNotFoundException(String msg) {
        super(msg);
    }
}
