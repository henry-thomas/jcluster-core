/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Exception.java to edit this template
 */
package org.jcluster.core.exception.fragmentation;

import org.jcluster.core.exception.JcException;

/**
 *
 * @author platar86
 */
public class JcFragmentationException extends JcException {

    /**
     * Creates a new instance of <code>JcFragmentationException</code> without
     * detail message.
     */
    public JcFragmentationException() {
    }

    /**
     * Constructs an instance of <code>JcFragmentationException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public JcFragmentationException(String msg) {
        super(msg);
    }
}
