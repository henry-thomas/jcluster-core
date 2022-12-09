/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.cluster.hzUtils;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.cp.CPSubsystemConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import java.util.Map;
import org.jcluster.core.bean.JcAppDescriptor;
import org.jcluster.core.config.JcAppConfig;

/**
 *
 * @author henry
 */
public class HzController {

//    private String appId;
    private final IMap<String, JcAppDescriptor> map;
    private final Config hzConfig = new Config();
    private final HazelcastInstance hz;
    private static HzController INSTANCE = null;

    private HzController() {
        hzConfig.setClusterName("hz-jc-cluster");
        setDiscoveryConfig();
        
//        hzConfig.getCPSubsystemConfig().setCPMemberCount(3);

        hz = Hazelcast.newHazelcastInstance(hzConfig);

        map = hz.getMap("jc-app-map");

        map.addEntryListener(new ConnectionCallback(), true);
    }

    private void setDiscoveryConfig() {
        JoinConfig join = new JoinConfig();
        join.getMulticastConfig().setEnabled(false);
        join.getTcpIpConfig().setEnabled(true);

        join.getTcpIpConfig().getMembers().add(JcAppConfig.getINSTANCE().getJcHzPrimaryMember());
        hzConfig.getNetworkConfig().setJoin(join);
    }

    public static HzController getInstance() {

        if (INSTANCE == null) {
            INSTANCE = new HzController();
        }

        return INSTANCE;
    }

    public IMap<String, JcAppDescriptor> getMap() {
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
