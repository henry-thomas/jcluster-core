/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypower24.jcclustertest.tableModel;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.model.LevelModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author henry
 */
public class LoggerDescBean {

    private final String name;
    private final Integer levelInt;
    private final String levelStr;

    public static String[] getAllLevelsStr() {
        return new String[]{
            Level.ALL.levelStr,
            Level.TRACE.levelStr,
            Level.DEBUG.levelStr,
            Level.INFO.levelStr,
            Level.WARN.levelStr,
            Level.ERROR.levelStr,
            Level.OFF.levelStr,};
    }

    public LoggerDescBean(String name, Integer levelInt) {
        this.name = name;
        this.levelInt = levelInt;

        Level toLevel = Level.toLevel(levelInt);
        this.levelStr = toLevel.levelStr;
    }

    public String getName() {
        return name;
    }

    public Integer getLevelInt() {
        return levelInt;
    }

    public String getLevelStr() {
        return levelStr;
    }

    public static List<LoggerDescBean> fromMap(Map<String, Integer> map) {

        List<LoggerDescBean> list = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            String name = entry.getKey();
            Integer val = entry.getValue();

            list.add(new LoggerDescBean(name, val));
        }

        return list;
    }

}
