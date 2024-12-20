/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.hzUtils;

import org.jcluster.core.JcHzConnectionListener;
import com.hazelcast.config.Config;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.spi.properties.ClusterProperty;
import java.util.Map;
import org.jcluster.core.bean.JcAppDescriptor;
import org.jcluster.core.config.JcAppConfig;

/**
 *
 * @autor Henry Thomas
 */
public class HzController {

//    private String appId;
    private final IMap<String, JcAppDescriptor> map;
    private final Config hzConfig = new Config();
    private final HazelcastInstance hz;
    private static HzController INSTANCE = null;

    private HzController() {

        boolean userClusterName = JcAppConfig.getINSTANCE().readProp("HZ_USECLUSTERNAME", false);
        if (userClusterName) {
            hzConfig.setClusterName("hz-jc-cluster");
        }

        String primaryMemAddress = JcAppConfig.getINSTANCE().getJcHzPrimaryMember();

        JoinConfig join = new JoinConfig();

        join.getMulticastConfig()
                .setEnabled(false);

        join.getTcpIpConfig()
                .setEnabled(true)
                .setConnectionTimeoutSeconds(5)
                .getMembers().add(primaryMemAddress);

        hzConfig.getNetworkConfig()
                .setPortAutoIncrement(true)
                .setPortCount(100)
                .setJoin(join);
//        hzConfig.setProperty("hazelcast.backpressure.enabled", "true");
        
        

        hz = Hazelcast.newHazelcastInstance(hzConfig);
//        ClusterProperty.BACKPRESSURE_ENABLED.setSystemProperty("true");
//        ClusterProperty.BACKPRESSURE_ENABLED;

        MapConfig noBackupsMap = new MapConfig("*")
                .setBackupCount(0)
                .setAsyncBackupCount(1)
                .setInMemoryFormat(InMemoryFormat.OBJECT)
                .setStatisticsEnabled(true);
        hz.getConfig().addMapConfig(noBackupsMap);

        map = hz.getMap("jc-app-map");

        map.addEntryListener(new JcHzConnectionListener(), true);
    }

    public synchronized static HzController getInstance() {

        if (INSTANCE == null) {
            INSTANCE = new HzController();
        }

        return INSTANCE;
    }

    public <K, V> IMap<K, V> getMap(String mapName) {
        return hz.getMap(mapName);
    }

    public IMap<String, JcAppDescriptor> getInstanceDescriptorMap() {
        return map;
    }

    public void destroy() {
        hz.shutdown();
    }

    public void showConnected() {
        for (Map.Entry<String, JcAppDescriptor> entry : map.entrySet()) {
            String appId = entry.getKey();
            String appName = entry.getValue().getAppName();

            System.out.println(appId + " is online as type: " + appName);
        }
    }

}
