/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.bean;

import java.util.Objects;
import org.jcluster.core.JcManager;

/**
 *
 * @author platar86
 */
public class MemSimpleDesc {

    private String ip;
    private int port;
    private String secret;

    public MemSimpleDesc() {
    }

    public MemSimpleDesc(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public MemSimpleDesc(String ip, int port, String secret) {
        this.ip = ip;
        this.port = port;
        this.secret = secret;
    }

    public static MemSimpleDesc createFromString(String template) {
        String[] split = template.split(":"); 
        if (split.length == 0) {
            return null;
        }
      
        MemSimpleDesc desc = new MemSimpleDesc();
        desc.ip = split[0];
        if (split.length >= 2) {
            desc.port = Integer.parseInt(split[1]);
        } else {
            desc.port = JcManager.DEFAULT_IPPORT;
        }

        if (split.length >= 3) {
            desc.secret = split[2];
        }
        return desc;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public boolean sameAs(JcAppDescriptor jad) {
        return Objects.equals(jad.getIpAddress(), ip) && Objects.equals(jad.getIpPort(), port);
    }

    public boolean sameAs(String ipAdd, int ipPort) {
        return Objects.equals(ipPort, port) && Objects.equals(ipAdd, ip);
    }

    public String getSecret() {
        return secret;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Objects.hashCode(this.ip);
        hash = 41 * hash + this.port;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MemSimpleDesc other = (MemSimpleDesc) obj;
        if (this.port != other.port) {
            return false;
        }
        return Objects.equals(this.ip, other.ip);
    }

}
