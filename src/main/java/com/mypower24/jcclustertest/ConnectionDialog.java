/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.mypower24.jcclustertest;

import com.mypower24.jcclustertest.controller.SystemPropManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.jcluster.core.JcCoreService;
import org.jcluster.core.bean.JcAppDescriptor;

/**
 *
 * @author henry
 */
public class ConnectionDialog extends javax.swing.JFrame {

    /**
     * Creates new form ConnectionDialog
     */
    SystemPropManager prop = SystemPropManager.getINSTANCE();
    private final JcTestWindow mainWindow;

    public ConnectionDialog(JcTestWindow mainWindow) {
        this.mainWindow = mainWindow;
        initComponents();

    }

    protected void connect() {
        Map<String, Object> config = new HashMap<>();

        config.put("primaryMembers", getList(primaryMember.getText()));
        config.put("appName", appName.getText());
        config.put("selfIpAddress", txtSelfIp.getText());
        config.put("tcpListenPort", getPortList(tcpPort.getText()));

        config.put("start", "");
        try {
            JcCoreService.getInstance().start(config);
            JcAppDescriptor selfDesc = JcCoreService.getSelfDesc();

            mainWindow.updateTitle("JC-Core App name:[" + selfDesc.getAppName() + "]   ID:[" + selfDesc.getInstanceId() + "]");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex, ex.getMessage(), JOptionPane.WARNING_MESSAGE);
        }
        this.setVisible(false);
    }

    private ArrayList<Integer> getPortList(String txt) {
        ArrayList<Integer> strList = new ArrayList<>();

        String[] split = txt.split(",");
        for (int i = 0; i < split.length; i++) {
            String splitStr = split[i].trim();
            if (splitStr.contains("-")) {
                String[] rangeSplitArr = splitStr.split("-");
                if (rangeSplitArr.length == 2) {
                    Integer begin = Integer.valueOf(rangeSplitArr[0]);
                    Integer end = Integer.valueOf(rangeSplitArr[1]);
                    if (begin < end) {
                        for (int j = begin; j <= end; j++) {
                            strList.add(j);
                        }
                    }
                }
            } else {
                strList.add(Integer.valueOf(splitStr));
            }
        }
        return strList;
    }

    private ArrayList<String> getList(String txt) {
        String[] split = txt.split(",");
        ArrayList<String> strList = new ArrayList<>();

        for (int i = 0; i < split.length; i++) {
            String splitStr = split[i].trim();
            strList.add(splitStr);
        }
        return strList;
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel3 = new javax.swing.JPanel();
        jCheckBox1 = new javax.swing.JCheckBox();
        appName = new javax.swing.JTextField();
        tcpPort = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jCheckBox2 = new javax.swing.JCheckBox();
        jButton3 = new javax.swing.JButton();
        primaryMember = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        txtSelfIp = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();

        jCheckBox1.setText("Isolated");
        jCheckBox1.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox1ActionPerformed(evt);
            }
        });

        appName.setText(prop.loadParamAsString("appName", "jcMissionControl"));
        appName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                appNameActionPerformed(evt);
            }
        });

        tcpPort.setText(prop.loadParamAsString("tcpPort", "8380"));
        tcpPort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tcpPortActionPerformed(evt);
            }
        });

        jLabel1.setText("App Name");

        jLabel3.setText("Port");

        jCheckBox2.setText("Primary");
        jCheckBox2.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);

        jButton3.setText("Okay");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        primaryMember.setText(prop.loadParamAsString("primaryMember", "192.168.24.34:8381"));
        primaryMember.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                primaryMemberActionPerformed(evt);
            }
        });

        jLabel4.setText("Primary");

        jLabel5.setText("Self");

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        txtSelfIp.setText(prop.loadParamAsString("txtSelfIp","192.168.24.34"));

        jLabel2.setText("Self IP");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(appName, javax.swing.GroupLayout.DEFAULT_SIZE, 137, Short.MAX_VALUE)
                            .addComponent(tcpPort)))
                    .addComponent(jLabel5)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(15, 15, 15)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jCheckBox2)
                            .addComponent(jCheckBox1)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtSelfIp, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(27, 27, 27)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 11, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 94, Short.MAX_VALUE)
                        .addComponent(jButton3))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(7, 7, 7)
                        .addComponent(jLabel4)
                        .addGap(0, 118, Short.MAX_VALUE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(primaryMember, javax.swing.GroupLayout.DEFAULT_SIZE, 160, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4)
                            .addComponent(jLabel5))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, 18, Short.MAX_VALUE)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(primaryMember, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(103, 103, 103)
                                .addComponent(jButton3))
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(appName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel1))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(tcpPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel3))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jCheckBox1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jCheckBox2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(txtSelfIp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel2))))
                        .addContainerGap(12, Short.MAX_VALUE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jSeparator1)
                        .addContainerGap())))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jCheckBox1ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        // Variables declaration - do not modify                     

        prop.saveParam("appName", appName.getText());
        prop.saveParam("primaryMember", primaryMember.getText());
        prop.saveParam("txtSelfIp", txtSelfIp.getText());
        prop.saveParam("tcpPort", tcpPort.getText());

        try {
            JcCoreService.getInstance().stop();
        } catch (Exception ex) {
            Logger.getLogger(ConnectionDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
        connect();
    }//GEN-LAST:event_jButton3ActionPerformed


    private void tcpPortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tcpPortActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_tcpPortActionPerformed

    private void primaryMemberActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_primaryMemberActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_primaryMemberActionPerformed

    private void appNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_appNameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_appNameActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField appName;
    private javax.swing.JButton jButton3;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTextField primaryMember;
    private javax.swing.JTextField tcpPort;
    private javax.swing.JTextField txtSelfIp;
    // End of variables declaration//GEN-END:variables
}
