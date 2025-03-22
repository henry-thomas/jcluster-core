/* Copyright (C) Solar MD (Pty) ltd - All Rights Reserved
 * 
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  
 *  Written by platar86, 2021
 *  
 *  For more information http://www.solarmd.co.za/ 
 *  email: info@solarmd.co.za
 *  7 Alternator Ave, Montague Gardens, Cape Town, 7441 Sout Africa
 *  Phone: 021 555 2181
 *  
 */
package com.mypower24.jcclustertest.controller;

/**
 *
 * @author platar86
 */
public class JsonDecodeException extends Exception {

    /**
     * Creates a new instance of <code>JsonDecodeException</code> without detail
     * message.
     */
    public JsonDecodeException() {
    }

    /**
     * Constructs an instance of <code>JsonDecodeException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public JsonDecodeException(String msg) {
        super(msg);
    }
}
