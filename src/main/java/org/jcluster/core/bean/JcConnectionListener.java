/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package org.jcluster.core.bean;

import org.jcluster.core.JcClientConnection;

/**
 *
 * @author platar86
 */
public interface JcConnectionListener {

    public void onConnectionEvent(JcClientConnection con);

}
