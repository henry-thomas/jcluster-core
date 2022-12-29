/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.config;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @autor Henry Thomas
 *
 * These properties should be stored in your server configuration. E.G. in
 * Payara, in DAC, in server-config -> System Properties Add them there. If
 * someone can think of a better way, please mention.
 *
 * They are read by JcAppConfig, so all accessible from there throughout the
 * project.
 *
 */
public class JcAppConfig {

    private static final Logger LOG = Logger.getLogger(JcAppConfig.class.getName());

    private final String jcHzPrimaryMember;
    private final Integer port;
    private final Integer minConnections;
    private final String hostName;
    private final boolean isolated;
    private final String appName;
    private final List<String> pkgFilterList = new ArrayList<>();
    private final Long jcLastSendMaxTimeout;

    private static final JcAppConfig INSTANCE = new JcAppConfig();

    private JcAppConfig() {
        this.jcHzPrimaryMember = readProp("JC_HZ_PRIMARY_MEMBER", "127.0.0.1");
        this.port = Integer.valueOf(readProp("JC_PORT", "2200"));
        this.minConnections = Integer.valueOf(readProp("JC_MIN_CONNECTIONS", "2"));
        this.hostName = readProp("JC_HOSTNAME", "127.0.0.1");
        this.appName = readProp("JC_APP_NAME", "jcAppNameDefault");
        this.isolated = readProp("JC_ISOLATED", false);

        this.jcLastSendMaxTimeout = Long.valueOf(readProp("JC_LAST_SEND_MAX_TIMEOUT", "5000"));

        initPkgFilterList();
    }

    private void initPkgFilterList() {
        String pkgFilter = readProp("JC_SCAN_PKG_NAME", "");
        if (pkgFilter.equals("")) {
            pkgFilterList.add("");
            LOG.warning("JCluster set to scan the entire package, this can slow down application startup. Please set correct package to scan in domain.xml -> configs -> server-config example: <system-property name=\"JC_SCAN_PKG_NAME\" value=\"com.myPower24.commonLib\"></system-property>");
        } else {
            String[] split;
            if (pkgFilter.contains(";")) {
                split = pkgFilter.split(";");
            } else {
                split = pkgFilter.split(",");
            }

            for (String pkg : split) {
                pkgFilterList.add(pkg.trim());
            }
        }
    }

    public String readProp(String propName) {
        return readProp(propName, null);
    }

    public boolean readProp(String propName, boolean defaultValue) {
        String readProp = readProp(propName, String.valueOf(defaultValue));
        return Boolean.parseBoolean(readProp);
    }

    public String readProp(String propName, String defaultValue) {
        String prop = System.getProperty(propName);
        if (prop == null) {
            LOG.log(Level.SEVERE, "{0} property not set!", propName);
            return defaultValue;
        }
        return prop;
    }

    public Integer getMinConnections() {
        return minConnections;
    }

    public boolean isIsolated() {
        return isolated;
    }

    public static JcAppConfig getINSTANCE() {
        return INSTANCE;
    }

    public String getJcHzPrimaryMember() {
        return jcHzPrimaryMember;
    }

    public Integer getPort() {
        return port;
    }

    public String getHostName() {
        return hostName;
    }

    public String getAppName() {
        return appName;
    }

    public static Long getConnMaxTimeout() {
        return INSTANCE.jcLastSendMaxTimeout;
    }

    public Long getJcLastSendMaxTimeout() {
        return jcLastSendMaxTimeout;
    }

    public List<String> getPkgFilterList() {
        return pkgFilterList;
    }

}
