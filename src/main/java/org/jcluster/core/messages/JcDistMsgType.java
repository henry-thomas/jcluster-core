/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core.messages;

/**
 *
 * @author henry
 */
public enum JcDistMsgType {
    JOIN,
    JOIN_RESP,
    NEW_MEMBER,
    PING,
    LEAVE,
    PUBLISH,
    SUBSCRIBE,
    AUTH_REQ,
    AUTH_RESP;
}
