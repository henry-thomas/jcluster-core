/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.jcluster.core;

/**
 *
 * @author henry
 */
public class MemberEvent {

    public static final int TYPE_ADD = 1;
    public static final int TYPE_REMOVE = 2;

    private final JcMember member;
    private final int type;

    public MemberEvent(JcMember member, int type) {
        this.member = member;
        this.type = type;
    }

    public JcMember getMember() {
        return member;
    }

    public int getType() {
        return type;
    }

}
