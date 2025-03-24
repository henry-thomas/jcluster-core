/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypower24.jcclustertest.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;

/**
 *
 * @author platar86
 */
public final class SystemPropManager {

    private static final SystemPropManager INSTANCE = new SystemPropManager();
    public static final String SYSTEM_PROP_FILE_NAME = "/etc/jcMissionControl/jc.conf";

    private final Object lock = new Object();

    private final HashMap<String, String> propMap = new HashMap<>();
    private boolean windowsEnv = false;
    private boolean debug = false;

    private final HashMap<String, String> startupArgMap = new HashMap<>();

    private SystemPropManager() {

        Object get = System.getProperties().get("os.name");
        if (get != null && get.toString().contains("Windows")) {
            this.windowsEnv = true;
        }

        initAllParam();

    }

    public static SystemPropManager getINSTANCE() {
        return INSTANCE;
    }

    private void initAllParam() {
//        boolean success = readJsonObjectFromFileIntoObject(SYSTEM_PROPJSONMAP_FILE, propMap);
        boolean success = FileUtil.readJsonObjectFromFileIntoObject(SYSTEM_PROP_FILE_NAME, propMap);
        if ((!success || propMap.isEmpty())) {

            try {
                FileUtil.saveObjectToFileAsJson(propMap, SYSTEM_PROP_FILE_NAME, true);
            } catch (JsonDecodeException ex) {
                java.util.logging.Logger.getLogger(SystemPropManager.class.getName()).log(Level.SEVERE, null, ex);
            }
//        }
        }
    }

    public static SystemPropManager getInstance() {
        return INSTANCE;
    }

    public HashMap<String, String> getConfigMap() {

        return propMap;
    }

    public boolean isWindowsEnv() {
        return windowsEnv;
    }

    /**
     *
     * @param paramName Parameters name to be load
     * @return Parameter value as String or null if not found
     */
    private String readParamFromMap(String key) {
        String value = null;
        synchronized (lock) {
            value = propMap.get(key);
        }
        return value;
    }

    private void writeParamToMap(String key, String value) {
        synchronized (lock) {
            propMap.put(key, value);
        }
    }

    private void saveMapToFile() {
        try {
            FileUtil.saveObjectToFileAsJson(propMap, SYSTEM_PROP_FILE_NAME, true);

        } catch (JsonDecodeException ex) {
            java.util.logging.Logger.getLogger(SystemPropManager.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String saveParamAsJson(String key, Object value) throws JsonProcessingException {
        return saveParamAsJson(key, value, null);
    }

    public String saveParamAsJson(String key, Object value, HashMap<SerializationFeature, Boolean> serConf) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        //for prity print
        if (serConf != null) {
            serConf.entrySet().forEach((entry) -> {
                objectMapper.configure(entry.getKey(), entry.getValue());
            });

        } else {
            objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        }

        String jsonValue = objectMapper.writeValueAsString(value);

        return saveParam(key, jsonValue);
    }

    public String saveParam(String key, Object value) {
        return saveParam(key, String.valueOf(value));
    }

    public String saveParam(String key, String value) {
        synchronized (lock) {
            if (propMap.containsKey(key)) {
                String currentValue = propMap.get(key);
                if (currentValue != null && currentValue.equals(value)) {
                    return value;
                }
            }
        }

//        saveParamToDB(deviceProp);
        writeParamToMap(key, value);
        saveMapToFile();

        return value;
    }

    public boolean removeParam(String key) {

        String value = propMap.remove(key);
        if (value != null) {

//            EntityManager em = emf.createEntityManager();
//            DeviceProp singleResult = null;
//
//            try {
//                singleResult = em.createNamedQuery("DeviceProp.findByPropKey", DeviceProp.class
//                )
//                        .setParameter("propKey", key)
//                        .getSingleResult();
//
//                if (singleResult != null) {
//                    em.getTransaction().begin();
//                    em.remove(singleResult);
//                    em.getTransaction().commit();
//                }
//                return true;
//            } catch (PersistenceException e) {
//            } finally {
//                em.clear();
//                em.close();
//            }
            saveMapToFile();

        }
        return false;
    }

    public HashMap<String, String> getParamAsJsonMap(String paramName) {
        String jsonContent = readParamFromMap(paramName);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS, false);
        HashMap<String, String> jsonMap = new HashMap<>();
        try {

            JsonNode rootNode = objectMapper.readTree(jsonContent.getBytes());
            if (rootNode != null) {
                rootNode.fields().forEachRemaining((t) -> {
                    jsonMap.put(t.getKey(), t.getValue().asText());
                });

            }
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(SystemPropManager.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        return jsonMap;
    }

    public boolean loadParamAsObject(String paramName, Object destination, Object def) {

        String jsonContent = readParamFromMap(paramName);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS, false);
        if (jsonContent != null) {
            try {

                JsonNode rootNode = objectMapper.readTree(jsonContent.getBytes());
                if (rootNode == null) {
                    return false;
                }
//                objectMapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
                ObjectReader readerForUpdating = objectMapper.readerForUpdating(destination);
                readerForUpdating.readValue(rootNode);
                return true;

            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(SystemPropManager.class
                        .getName()).log(Level.SEVERE, null, ex);

            }
        }
        try {
            String param = saveParamAsJson(paramName, def);

        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(SystemPropManager.class
                    .getName()).log(Level.SEVERE, null, ex);
        }

        return false;
    }

    public String loadParamAsString(String paramName, String defaultValue) {
        String param = readParamFromMap(paramName);
        if (param != null) {
            return param;
        }
        saveParam(paramName, defaultValue);
        return defaultValue;
    }

    public String loadParamAsString(String paramName) throws Exception {
        String param = readParamFromMap(paramName);
        if (param != null) {
            return param;
        }
        throw new Exception("Param " + paramName + " does not exist");
    }

    public boolean isConfigExist(String confKey) {
        synchronized (lock) {
            return propMap.containsKey(confKey);
        }
    }

    public long loadParamAsLong(String paramName, long defaultValue) {
        String param = readParamFromMap(paramName);
        try {
            return Long.valueOf(param);
        } catch (NumberFormatException | NullPointerException e) {
        }
        saveParam(paramName, String.valueOf(defaultValue));
        return defaultValue;
    }

    public int loadParamAsInteger(String paramName, Integer defaultValue) {
        String param = readParamFromMap(paramName);
        try {
            return Integer.valueOf(param);
        } catch (NumberFormatException | NullPointerException e) {
        }
        if (defaultValue != null) {
            saveParam(paramName, String.valueOf(defaultValue));
            return defaultValue;
        }
        return -1;
    }

    public Double loadParamAsDouble(String paramName, Double defaultValue) {
        String param = readParamFromMap(paramName);
        try {
            return Double.valueOf(param);
        } catch (NumberFormatException | NullPointerException e) {
        }
        if (defaultValue != null) {
            saveParam(paramName, String.valueOf(defaultValue));
            return defaultValue;
        }
        return -1d;
    }

    /**
     * Load some parameters as Integer from prop file. If property is not found
     * and default value is null return -1 if property is not found and default
     * value is != null new property will be created and the default value will
     * be return.
     *
     * @param paramName
     * @param defaultValue if prop not found return and create default value, if
     * default value is null return -1
     * @return
     */
    public boolean loadParamAsBoolean(String paramName, Boolean defaultValue) {
        String param = readParamFromMap(paramName);
        try {
            if (param != null) {
                return Boolean.valueOf(param);
            }
        } catch (NumberFormatException e) {
        }
        saveParam(paramName, String.valueOf(defaultValue.toString()));
        return defaultValue;
    }

    public Date loadParamAsDate(String paramName, Date defaultValue) {
        String param = readParamFromMap(paramName);
        try {
            if (param != null) {
                Long time = Long.valueOf(param);
                return new Date(time);
            }
        } catch (NumberFormatException e) {
        }
        saveParam(paramName, String.valueOf(defaultValue.getTime()));
        return defaultValue;
    }

    public boolean isDebug() {
        return debug;
    }

    public String getStartupArg(String key) {
        return startupArgMap.get(key);
    }

    public boolean containsStartupArg(String key) {
        return startupArgMap.containsKey(key);
    }

    public void initStartupArgs(HashMap<String, String> args) {
        startupArgMap.putAll(args);

        debug = startupArgMap.containsKey("debug");
    }

}
