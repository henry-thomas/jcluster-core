/*
 * ===================================================================
 * Licensed to henry under one or more contributor license agreements.
 * See the LICENSE file distributed with this work for additional information.
 * You may not use this file except in compliance with the License.
 *
 * Created by: henry on 2025-03-29
 * ===================================================================
 * 
 * Disclaimer:
 * This software is provided "as is," without any warranties of any kind,
 * either express or implied, including but not limited to the implied 
 * warranties of merchantability and fitness for a particular purpose.
 * In no event shall the author or contributors be liable for any direct, 
 * indirect, incidental, special, exemplary, or consequential damages 
 * arising in any way out of the use of this software.
 * 
 * Description:
 * 
 * 
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change the license.
 */
// Template: Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
package com.mypower24.jcclustertest.remote;

/**
 *
 * @author henry
 */
public class BroadcastImpl implements BroadcastIFace {

    @Override
    public void onTopicMessage(String appIdFilter, String message) {
        System.out.println("Rec Topic value from: " + appIdFilter + " Message: " + message);
    }

    @Override
    public void onBcMessage(String message) {
        System.out.println("Rec BC message: " + message);
    }

}
