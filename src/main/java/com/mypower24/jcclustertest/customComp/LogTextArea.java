/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypower24.jcclustertest.customComp;

import java.text.SimpleDateFormat;
import java.time.LocalTime;
import javax.swing.JTextArea;

/**
 *
 * @author platar86
 */
public class LogTextArea extends JTextArea implements JLogInterface{

    private long line = 0;

    @Override
    public void warn(String template, Object... msg) {
        log("WARN", template, msg);
    }

    
    @Override
    public void info(String template, Object... msg) {
        log("INFO", template, msg);
    }

    private String formatMsg(String template, Object... msg) {
        for (int i = 0; i < msg.length; i++) {
            Object object = msg[i];
            if (object == null) {
                object = "null";
            }

            template = template.replaceFirst("\\{\\}", object.toString());
        }
        return template;
    }

    private void log(String level, String template, Object... msg) {
        String msgStr = formatMsg(template, msg);
        LocalTime now = LocalTime.now();
        line++;
        String sb = "[" + level + "]@[" + now + " " + msgStr + "\n" + getText();
        setText(sb);

    }
}
