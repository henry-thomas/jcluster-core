///*
// * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
// * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
// */
//package org.jcluster.core;
//
//import ch.qos.logback.classic.Logger;
//import java.io.IOException;
//import java.io.NotSerializableException;
//import java.io.ObjectInputStream;
//import java.io.ObjectOutputStream;
//import static java.lang.System.currentTimeMillis;
//import java.net.InetSocketAddress;
//import java.net.Socket;
//import java.net.SocketAddress;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.FutureTask;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.TimeoutException;
//import org.jcluster.core.bean.JcAppDescriptor;
//import org.jcluster.core.bean.JcHandhsakeFrame;
//import org.jcluster.core.bean.jcCollections.RingConcurentList;
//import org.jcluster.core.exception.JcRuntimeException;
////import org.jcluster.core.config.JcAppConfig;
//import org.jcluster.core.exception.cluster.JcIOException;
//import org.jcluster.core.messages.JcMessage;
//import org.jcluster.core.messages.JcMsgResponse;
//import org.jcluster.core.monitor.JcMemberMetrics;
//import org.jcluster.core.monitor.MethodExecMetric;
//import org.jcluster.core.proxy.JcProxyMethod;
//import org.slf4j.LoggerFactory;
//
///**
// *
// * @author platar86
// *
// * Bean that handles connections with another app. Uses the appDescriptor to
// * figure out how to manage that connection.
// */
//public class JcRemoteInstanceConnectionBean {
//
//
//    private static final Logger LOG = (Logger) LoggerFactory.getLogger(JcRemoteInstanceConnectionBean.class);
//
//
//
//    private final JcMemberMetrics metrics;
//    private final JcMember member;
//
//    private JcAppDescriptor desc = null;
//
//    public JcRemoteInstanceConnectionBean(JcMember mem) {
//        this.member = mem;
//        this.metrics = mem.getMetrics();
//    }
//
//    public JcMemberMetrics getMetrics() {
//        return metrics;
//    }
//
//    public JcMember getMember() {
//        return member;
//    }
//
//    public boolean isOnDemandConnection() {
//        return onDemandConnection;
//    }
//
//    public void setOnDemandConnection(boolean onDemandConnection) {
//        this.onDemandConnection = onDemandConnection;
//    }
//
//    public void setDesc(JcAppDescriptor desc) {
//        this.desc = desc;
//    }
//
//   
//    @Override
//    public String toString() {
//        return "JcRemoteInstanceConnectionBean{" + "appName="
//                + desc.getAppName() + ", instanceId="
//                + desc.getInstanceId() + " address="
//                + desc.getIpAddress() + '}';
//    }
//
//}
