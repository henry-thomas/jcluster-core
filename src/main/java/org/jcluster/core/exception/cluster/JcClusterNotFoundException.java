/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Exception.java to edit this template
 */
package org.jcluster.core.exception.cluster;

import org.jcluster.core.exception.JcRuntimeException;

/**
 *
 * @author henry
 */
public class JcClusterNotFoundException extends JcRuntimeException{

    /**
     * Creates a new instance of <code>JcClusterNotFoundException</code> without
     * detail message.
     */
    public JcClusterNotFoundException() {
    }

    /**
     * Constructs an instance of <code>JcClusterNotFoundException</code> with
     * the specified detail message.
     *
     * @param msg the detail message.
     */
    public JcClusterNotFoundException(String msg) {
        super(msg);
    }
}
