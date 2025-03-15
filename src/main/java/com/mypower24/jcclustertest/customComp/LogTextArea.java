/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypower24.jcclustertest.customComp;

import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.regex.Pattern;
import javax.swing.JTextArea;
import org.apache.commons.lang3.RegExUtils;

/**
 *
 * @author platar86
 */
public class LogTextArea extends JTextArea {

    private final SimpleDateFormat formatter = new SimpleDateFormat("mm-ss-yyyy");
    private long line = 0;

    public void logWarn(String template, Object... msg) {
        log("WARN", template, msg);
    }

    public void logInfo(String template, Object... msg) {
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
