package org.jcluster.core;

///*
// * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
// * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
// */
//package org.jcluster.core.cluster;
//
//import java.io.Serializable;
//import static java.lang.System.currentTimeMillis;
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import org.jcluster.core.bean.JcAppDescriptor;
//import org.jcluster.core.bean.JcConnectionMetrics;
//import org.jcluster.core.bean.jcCollections.RingConcurentList;
//import org.jcluster.core.cluster.ConnectionType;
//import org.jcluster.core.cluster.JcClientConnection;
//import org.jcluster.core.config.JcAppConfig;
//
///**
// *
// * @autor Henry Thomas
// */
//public class JcInstance implements Serializable {
//
//    private final String serialVersionUID = "-1455291844074901991";
//    private static final Logger LOG = Logger.getLogger(JcInstance.class.getName());
//
//    //this map holds all connection to different APPs with key instanceId which is unique and value 
//    //this maps must be accessable only from this class to avoid complex synchronizations
//    private final JcAppDescriptor appDesc = new JcAppDescriptor();
//
//  
//
//    private int totalReconnects = 0;
//
//  
//
//
//    protected JcAppDescriptor getAppDesc() {
//        return appDesc;
//    }
//
//}
