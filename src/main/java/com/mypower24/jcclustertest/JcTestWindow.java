/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.mypower24.jcclustertest;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.mypower24.jcclustertest.customComp.JLogInterface;
import com.mypower24.jcclustertest.customComp.LogTextArea;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import org.jcluster.core.JcCoreService;
import org.jcluster.core.JcManager;
import org.jcluster.core.JcMember;
import org.jcluster.core.MemberEvent;
import org.jcluster.core.RemMembFilter;
import org.jcluster.core.monitor.AppMetricMonitorInterface;

/**
 *
 * @author henry
 */
public class JcTestWindow extends javax.swing.JFrame {

    /**
     * Creates new form JcTestWindow
     */
    private volatile boolean running = false;
    AppMetricMonitorInterface metricsMonitor = JcManager.generateProxy(AppMetricMonitorInterface.class);
    FilterTestIFace filterTestIFace = JcManager.generateProxy(FilterTestIFace.class);
    private final Set<JcMember> memberList = new HashSet<>();
    private JcMember selectedMember = null;
    private String selectedFilter = null;

    private final JLogInterface log;

    public JcTestWindow() {
        initComponents();
        log = (LogTextArea) logContainer;

        JcCoreService.getInstance().addMemberEventListener(this::onMemberEvent);

        ConnectionDialog connDlg = new ConnectionDialog();

        tblMembers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblFilters.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//        connDlg.setVisible(true);
    }

    private void onMemSelChange() {
        int rowIdx = tblMembers.getSelectedRow();
        if (rowIdx == -1) {
            DefaultTableModel dtm = (DefaultTableModel) tblFilters.getModel();
            dtm.getDataVector().clear();
            dtm = (DefaultTableModel) tblFilterValues.getModel();
            dtm.getDataVector().clear();
            return;
        }

        String memId = tblMembers.getValueAt(rowIdx, 0).toString();
        log.info("onMemberSelect {}", memId);
        selectedMember = JcCoreService.getMemberMap().get(memId);
        if (selectedMember == null) {
            log.info("Selected member is null: {}", memId);
            return;
        }
        updateFilters();
    }

    private void onSelectedFilterChange() {
        int rowIdx = tblFilters.getSelectedRow();
        if (rowIdx == -1) {
            DefaultTableModel dtmFilterValues = (DefaultTableModel) tblFilterValues.getModel();
            dtmFilterValues.getDataVector().clear();
            return;
        }
        String filterName = tblFilters.getValueAt(rowIdx, 0).toString();
        selectedFilter = filterName;
        log.info("onSelFilterChange {}", filterName);
        updateSelectedFilterValues();
    }

    private void updateSelectedFilterValues() {
        if (selectedFilter != null) {
            DefaultTableModel dtmFilterValues = (DefaultTableModel) tblFilterValues.getModel();
            dtmFilterValues.getDataVector().clear();
            Set<Object> filters = metricsMonitor.getFilterValues(selectedMember.getDesc().getInstanceId(), selectedFilter);
            for (Object filter : filters) {
                dtmFilterValues.addRow(new Object[]{filter});
            }
            dtmFilterValues.fireTableDataChanged();
        }
    }

    private void updateFilters() {

        DefaultTableModel dtm = (DefaultTableModel) tblFilters.getModel();

        dtm.getDataVector().clear();

        Map<String, Integer> filterList = metricsMonitor.getFilterList(selectedMember.getDesc().getInstanceId());

        for (Map.Entry<String, Integer> entry : filterList.entrySet()) {
            String fName = entry.getKey();
            Integer size = entry.getValue();

            dtm.addRow(new Object[]{fName.isBlank() ? "No Filter name" : fName, size});
        }

    }

    private void onMemberEvent(MemberEvent ev) {
        DefaultTableModel dtm = (DefaultTableModel) tblMembers.getModel();
        if (ev.getType() == MemberEvent.TYPE_ADD) {
//            JcMember m = ev.getMember();
            memberList.add(ev.getMember());
        } else if (ev.getType() == MemberEvent.TYPE_REMOVE) {
            memberList.remove(ev.getMember());

        }
        dtm.getDataVector().clear();
        for (JcMember m : memberList) {
            dtm.addRow(new Object[]{m.getId(), m.getDesc().getAppName(), m.getDesc().getTopicList()});
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    private void onAddRemoveOperation(boolean isAdd) {
        Integer itteration = (Integer) testFilterARFIteration.getValue();
        Integer itterationDelay = (Integer) testFilterARFIterationDelay.getValue();
        new Thread(() -> {
            String v = testFilterARFIterVal.getText();
            if (v.contains("{randData=")) {
                String randDataSubStr = v.substring(v.indexOf("{randData=") + "{randData=".length());
                String randDataLenStr = randDataSubStr.substring(0, randDataSubStr.indexOf("}"));
                try {
                    int randDataLen = Integer.parseInt(randDataLenStr);
                    byte randData[] = new byte[randDataLen];
                    char c = 'A';
                    for (int i = 0; i < randDataLen; i++) {
                        randData[i] = (byte) c++;
                        if (c >= 'Z') {
                            c = 'A';
                        }
                    }
                    String randDataStr = new String(randData);
                    v = v.replaceFirst("\\{randData=\\d+\\}", randDataStr);

                } catch (NumberFormatException e) {
                }

            }
            long duration = System.currentTimeMillis();
            for (int i = 0; i < itteration; i++) {
                String itVal = v.replaceAll("\\{i\\}", String.valueOf(i));

                if (isAdd) {
                    JcManager.addFilter(testFilterARFIterName.getText(), itVal);
                } else {
                    JcManager.removeFilter(testFilterARFIterName.getText(), itVal);
                }
                if (itterationDelay > 0) {
                    try {
                        Thread.sleep(itterationDelay);
                    } catch (InterruptedException ex) {
                    }
                }
            }
            duration = System.currentTimeMillis() - duration;
            log.info("Filter {} operation complete in {} ms", isAdd ? "add" : "remove", duration);
            //log Separatly
            if (testFilterARFIterationLog.isSelected()) {
                for (int i = 0; i < itteration; i++) {
                    String itVal = v.replaceAll("\\{i\\}", String.valueOf(i));
                    if (isAdd) {
                        log.info("Add filter Value:[{}] iteration[{}]", itVal, i);
                    } else {
                        log.info("Removing filter Value:[{}] iteration[{}]", itVal, i);
                    }
                }
            }
        }).start();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton3 = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        tblFilters = new javax.swing.JTable();
        jScrollPane5 = new javax.swing.JScrollPane();
        tblFilterValues = new javax.swing.JTable();
        txtFilterValues = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        btnRefreshFilters = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        testFilterARFIterVal = new javax.swing.JTextField();
        addFilterValue1 = new javax.swing.JButton();
        removeFilterValue1 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        testFilterARFIterName = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        testFilterARFIteration = new javax.swing.JSpinner();
        testFilterARFIterationDelay = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        testFilterARFIterationLog = new javax.swing.JCheckBox();
        addFilterValue2 = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        filterExec = new javax.swing.JTextField();
        btnTestFilterString = new javax.swing.JButton();
        btnTestFilterNumver = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblMembers = new javax.swing.JTable();
        jButton2 = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        logContainer = new LogTextArea();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        jMenu3 = new javax.swing.JMenu();
        darkModeRadio = new javax.swing.JRadioButtonMenuItem();

        jButton3.setText("jButton3");

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane2.setViewportView(jTextArea1);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jTabbedPane1.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
        jTabbedPane1.setTabPlacement(javax.swing.JTabbedPane.LEFT);

        tblFilters.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String [] {
                "Filter Name", "Size"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblFilters.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tblFiltersMouseReleased(evt);
            }
        });
        jScrollPane4.setViewportView(tblFilters);

        tblFilterValues.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null},
                {null},
                {null},
                {null}
            },
            new String [] {
                "Value"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane5.setViewportView(tblFilterValues);

        txtFilterValues.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtFilterValuesKeyReleased(evt);
            }
        });

        jLabel5.setText("Filter:");

        btnRefreshFilters.setText("Refresh");
        btnRefreshFilters.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefreshFiltersActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(btnRefreshFilters)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 50, Short.MAX_VALUE)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtFilterValues, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addGap(7, 7, 7))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtFilterValues, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(btnRefreshFilters))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 399, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Status", jPanel1);

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Test Filter"));

        testFilterARFIterVal.setText("val1_{i}_{randData=600}");
        testFilterARFIterVal.setToolTipText("Use {i} to be replaced with itteration number");

        addFilterValue1.setText("ADD");
        addFilterValue1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addFilterValue1ActionPerformed(evt);
            }
        });

        removeFilterValue1.setText("REMOVE");
        removeFilterValue1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeFilterValue1ActionPerformed(evt);
            }
        });

        jLabel1.setText("Filter Value:");

        jLabel2.setText("Filter Name:");

        testFilterARFIterName.setText("filter1");

        jLabel3.setText("Iterations:");

        testFilterARFIteration.setModel(new javax.swing.SpinnerNumberModel(500, 1, 100000, 1));
        testFilterARFIteration.setToolTipText("cxvbn cvb n");
        testFilterARFIteration.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        testFilterARFIteration.setEditor(new javax.swing.JSpinner.NumberEditor(testFilterARFIteration, ""));

        testFilterARFIterationDelay.setModel(new javax.swing.SpinnerNumberModel(0, 0, 500, 1));
        testFilterARFIterationDelay.setToolTipText("cxvbn cvb n");
        testFilterARFIterationDelay.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        testFilterARFIterationDelay.setEditor(new javax.swing.JSpinner.NumberEditor(testFilterARFIterationDelay, ""));

        jLabel4.setText("Iteration Delay:");

        testFilterARFIterationLog.setText("LOG Operation");

        addFilterValue2.setText(" SKIP");
        addFilterValue2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addFilterValue2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addContainerGap(45, Short.MAX_VALUE)
                        .addComponent(addFilterValue2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(addFilterValue1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(removeFilterValue1))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(testFilterARFIterationDelay, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(testFilterARFIteration, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(testFilterARFIterVal, javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(testFilterARFIterName, javax.swing.GroupLayout.Alignment.LEADING))
                                .addGap(5, 5, 5))))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(testFilterARFIterationLog)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(testFilterARFIterName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(testFilterARFIterVal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(testFilterARFIteration, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(testFilterARFIterationDelay, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addFilterValue1)
                    .addComponent(removeFilterValue1)
                    .addComponent(addFilterValue2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(testFilterARFIterationLog)
                .addContainerGap())
        );

        testFilterARFIteration.getAccessibleContext().setAccessibleDescription("");

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Execute Filter"));

        filterExec.setText("val1");

        btnTestFilterString.setText("Filter String");
        btnTestFilterString.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnTestFilterStringActionPerformed(evt);
            }
        });

        btnTestFilterNumver.setText("Filter Number");
        btnTestFilterNumver.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnTestFilterNumverActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(filterExec)
                .addContainerGap())
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnTestFilterString)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnTestFilterNumver)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(filterExec, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 28, Short.MAX_VALUE)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnTestFilterString)
                    .addComponent(btnTestFilterNumver))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 250, Short.MAX_VALUE)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jPanel3.getAccessibleContext().setAccessibleName("Add Remove");
        jPanel4.getAccessibleContext().setAccessibleName("Execute");

        jTabbedPane1.addTab("Test", jPanel2);

        jButton1.setText("Connect");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        tblMembers.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Member ID", "App Name", "Topics"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblMembers.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tblMembersMouseReleased(evt);
            }
        });
        tblMembers.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                tblMembersPropertyChange(evt);
            }
        });
        jScrollPane1.setViewportView(tblMembers);

        jButton2.setText("Call Remote");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jPanel5.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        logContainer.setColumns(20);
        logContainer.setRows(5);
        jScrollPane3.setViewportView(logContainer);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3)
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jMenu1.setText("File");
        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");
        jMenuBar1.add(jMenu2);

        jMenu3.setText("View");

        darkModeRadio.setSelected(true);
        darkModeRadio.setText("Dark Mode");
        darkModeRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                darkModeRadioActionPerformed(evt);
            }
        });
        jMenu3.add(darkModeRadio);

        jMenuBar1.add(jMenu3);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButton1)
                            .addComponent(jButton2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 110, Short.MAX_VALUE)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 316, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 366, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .addComponent(jPanel5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButton1)
                        .addGap(18, 18, 18)
                        .addComponent(jButton2)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1)
                        .addGap(14, 14, 14))
                    .addComponent(jTabbedPane1))
                .addGap(12, 12, 12)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        try {
            ConnectionDialog connDlg = new ConnectionDialog();
            connDlg.setVisible(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e, e.getMessage(), JOptionPane.WARNING_MESSAGE);
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void darkModeRadioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_darkModeRadioActionPerformed
        // TODO add your handling code here:
        try {
            if (darkModeRadio.isSelected()) {
                System.out.println("Dark");
                javax.swing.UIManager.setLookAndFeel(new FlatDarculaLaf());
            } else {
                System.out.println("Ligth");
                javax.swing.UIManager.setLookAndFeel(new FlatLightLaf());
            }
            SwingUtilities.updateComponentTreeUI(this);
        } catch (UnsupportedLookAndFeelException ex) {
            Logger.getLogger(JcTestWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_darkModeRadioActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        Map<String, JcMember> memberMap = JcCoreService.getInstance().getMemberMap();
        for (Map.Entry<String, JcMember> entry : memberMap.entrySet()) {
            String key = entry.getKey();
            JcMember mem = entry.getValue();
            String msg = metricsMonitor.testReq(mem.getDesc().getInstanceId());
            info(msg);
            return;
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void btnTestFilterStringActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTestFilterStringActionPerformed
        info(filterTestIFace.getStringFilter1(filterExec.getText()));
    }//GEN-LAST:event_btnTestFilterStringActionPerformed

    private void btnTestFilterNumverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTestFilterNumverActionPerformed
        info(filterTestIFace.getStringFilter2(Integer.valueOf(filterExec.getText())));
    }//GEN-LAST:event_btnTestFilterNumverActionPerformed


    private void removeFilterValue1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeFilterValue1ActionPerformed
        onAddRemoveOperation(false);
    }//GEN-LAST:event_removeFilterValue1ActionPerformed

    private void addFilterValue1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addFilterValue1ActionPerformed
        onAddRemoveOperation(true);
    }//GEN-LAST:event_addFilterValue1ActionPerformed

    private void addFilterValue2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addFilterValue2ActionPerformed
        JcManager.addFilter(testFilterARFIterName.getText(), "skip");
    }//GEN-LAST:event_addFilterValue2ActionPerformed

    private void btnRefreshFiltersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshFiltersActionPerformed
        updateFilters();
        updateSelectedFilterValues();
    }//GEN-LAST:event_btnRefreshFiltersActionPerformed

    private void txtFilterValuesKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtFilterValuesKeyReleased
        updateSelectedFilterValues();
    }//GEN-LAST:event_txtFilterValuesKeyReleased

    private void tblFiltersMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblFiltersMouseReleased
        onSelectedFilterChange();
    }//GEN-LAST:event_tblFiltersMouseReleased

    private void tblMembersPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_tblMembersPropertyChange
        System.out.println("tblMembersPropertyChange: " + evt.getPropertyName());
    }//GEN-LAST:event_tblMembersPropertyChange

    private void tblMembersMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblMembersMouseReleased
        onMemSelChange();
    }//GEN-LAST:event_tblMembersMouseReleased

    private void info(String msg) {
        JOptionPane.showMessageDialog(this, msg);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        FlatLightLaf.setup();
        try {
            javax.swing.UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(JcTestWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                JcTestWindow jcTestWindow = new JcTestWindow();
                jcTestWindow.setVisible(true);

            }
        });

    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addFilterValue1;
    private javax.swing.JButton addFilterValue2;
    private javax.swing.JButton btnRefreshFilters;
    private javax.swing.JButton btnTestFilterNumver;
    private javax.swing.JButton btnTestFilterString;
    private javax.swing.JRadioButtonMenuItem darkModeRadio;
    private javax.swing.JTextField filterExec;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea logContainer;
    private javax.swing.JButton removeFilterValue1;
    private javax.swing.JTable tblFilterValues;
    private javax.swing.JTable tblFilters;
    private javax.swing.JTable tblMembers;
    private javax.swing.JTextField testFilterARFIterName;
    private javax.swing.JTextField testFilterARFIterVal;
    private javax.swing.JSpinner testFilterARFIteration;
    private javax.swing.JSpinner testFilterARFIterationDelay;
    private javax.swing.JCheckBox testFilterARFIterationLog;
    private javax.swing.JTextField txtFilterValues;
    // End of variables declaration//GEN-END:variables
}
