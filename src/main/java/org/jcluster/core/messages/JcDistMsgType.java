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
//    JOIN,
//    JOIN_RESP,
    NEW_MEMBER,
    PING,
    LEAVE,
    PUBLISH_FILTER,
    SUBSCRIBE,
    SUBSCRIBE_RESP,
    AUTH_REQ,
    AUTH_RESP,
    CREATE_IO,
    SUBSCRIBE_STATE_REQ,
    SUBSCRIBE_STATE_RESP,
//    FRG_DATA,//
//    FRG_ACK,//
//    FRG_RESEND,//
    ;
}
