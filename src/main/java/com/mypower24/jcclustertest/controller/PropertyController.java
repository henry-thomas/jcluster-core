/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypower24.jcclustertest.controller;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author platar86
 */
public class PropertyController {

    private static final PropertyController INSTANCE = new PropertyController();
    private Properties appProps;

    private PropertyController() {

        appProps = new Properties();
        try {
            appProps.load(new FileInputStream("app.conf"));
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(PropertyController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            appProps = new Properties();

        }
    }

    private void save() {
        try {
            appProps.store(new FileWriter("app.conf"), "store to properties file");
        } catch (IOException ex) {
            Logger.getLogger(PropertyController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
