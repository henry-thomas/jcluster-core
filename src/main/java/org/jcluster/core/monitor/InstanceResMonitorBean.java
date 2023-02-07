/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.monitor;

import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcluster.core.bean.JcInstanceResMetrics;

/**
 *
 * @author henry
 */
public class InstanceResMonitorBean implements Serializable {

    private static final Logger LOG = Logger.getLogger(InstanceResMonitorBean.class.getName());

    private final String serverName;
    private static final String NET_FILE_LOCATION = "/sys/class/net/enp1s0f1/statistics/enp1s0f1/";
    private long prevNetRxCount = 0l;
    private long prevNetTxCount = 0l;
    private long prevNetUpdate = 0l;

    private final JcInstanceResMetrics metrics = new JcInstanceResMetrics();

    public InstanceResMonitorBean(String serverName) {
        this.serverName = serverName;
    }

    public void updateServerStatus() {

        double systemLoadAverage = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        long heapMemoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();

        metrics.setCpuUsage(systemLoadAverage);
        metrics.setMemUsage(heapMemoryUsage / 1024 / 1024);

        long currRxCount = 0l;
        long currTxCount = 0l;

        Object osName = System.getProperties().get("os.name");
        if (!osName.toString().contains("Windows")) {
            if (Files.exists(Paths.get(NET_FILE_LOCATION + "tx_bytes"), LinkOption.NOFOLLOW_LINKS)
                    && Files.exists(Paths.get(NET_FILE_LOCATION + "rx_bytes"), LinkOption.NOFOLLOW_LINKS)) {

                try {
                    List<String> currRxStringList = Files.readAllLines(Paths.get(NET_FILE_LOCATION + "rx_bytes"));
                    currRxCount = Long.parseLong(currRxStringList.get(0));

                    List<String> currTxStringList = Files.readAllLines(Paths.get(NET_FILE_LOCATION + "tx_bytes"));
                    currTxCount = Long.parseLong(currTxStringList.get(0));

                    long now = System.currentTimeMillis();
                    //On first iteration, set the values for next iteration but do not 
                    //set on dist map. To avoid strange values when prev update is = to current time.; 
                    if (prevNetUpdate == 0l) {
                        prevNetUpdate = now;
                        prevNetRxCount = currRxCount;
                        prevNetTxCount = currTxCount;
                        return;
                    }

                    long timeDiff = (now - prevNetUpdate);
                    long rx = (currRxCount - prevNetRxCount) / (timeDiff * 1024);
                    long tx = (currTxCount - prevNetTxCount) / (timeDiff * 1024);

                    metrics.setNetRxTraffic(rx);
                    metrics.setNetTxTraffic(tx);

                    prevNetRxCount = currRxCount;
                    prevNetTxCount = currTxCount;
                    prevNetUpdate = System.currentTimeMillis();
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            }

        }

    }

    public JcInstanceResMetrics getMetrics() {
        return metrics;
    }

}
