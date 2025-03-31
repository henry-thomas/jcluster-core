/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.test;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author henry
 */
public class JcTestImpl implements JcTestIFace {

    @Override
    public Object blockExec(String instanceId, int blockTime) {
        try {
            Thread.sleep(blockTime);
        } catch (InterruptedException ex) {
            Logger.getLogger(JcTestImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "Done";
    }

    @Override
    public byte[] getCustomDataSize(String instanceId, int size) {
        return ByteBuffer.allocate(size).array();
    }

    @Override
    public void throwEx(String instanceId, Throwable e) throws Throwable {
        throw e;
    }

    @Override
    public void throwRuntimeEx(String instanceId, RuntimeException e) {
        throw e;
    }

}
