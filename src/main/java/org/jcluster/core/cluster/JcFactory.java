/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.cluster;

/**
 *
 * @autor Henry Thomas
 */
public class JcFactory {

    public static JcManager initManager() {
        return JcManager.getInstance().startManager();
    }

    public static JcManager getManager() {
        return JcManager.getInstance();
    }

    public static void destroyManager() {
        JcManager.getInstance().destroy();
    }
}
