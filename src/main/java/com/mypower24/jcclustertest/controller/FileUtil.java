/*
 * Copyright (c) 2018, platar86
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.mypower24.jcclustertest.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.Properties;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.LoggerFactory;

/**
 *
 * @author platar86
 */
public class FileUtil {

    public static final String FILE_BACKUP_SUFIX = "_backup";

    private static final Logger LOG = (Logger) LoggerFactory.getLogger(FileUtil.class);

    public static boolean deleteFile(String fileName) {
        File file = new File(fileName);
        return file.delete();
    }

    public static void appendStrToFile(String fileName, String str) throws IOException {
        // Open given file in append mode. 
        if (!Files.isReadable(new File(fileName).toPath())) {
            Files.createDirectories(Paths.get(fileName).getParent());
        }
        BufferedWriter out = new BufferedWriter(new FileWriter(fileName, true));
        out.write(str);
        out.close();

    }

    public static void writeStrToFile(String fileName, String str) throws IOException {
        // Open given file in append mode. 
        if (!Files.isReadable(new File(fileName).toPath())) {
            Files.createDirectories(Paths.get(fileName).getParent());
        }
        BufferedWriter out = new BufferedWriter(new FileWriter(fileName, false));
        out.write(str);
        out.close();

    }

    public static String readFirstLine(String fd) {
        try (Stream<String> lines = Files.lines(new File(fd).toPath());) {
            List<String> collect = lines.limit(1).collect(Collectors.toList());
            if (!collect.isEmpty()) {
                return collect.get(0);
            }
        } catch (Throwable ex) {
            LOG.error(null, ex);
        }
        return null;

    }

    public static List<String> readFileFirstLine(String fd) {
        try (Stream<String> lines = Files.lines(new File(fd).toPath());) {
            List<String> collect = lines.limit(1).collect(Collectors.toList());
            return collect;
        } catch (Throwable ex) {
            LOG.error(null, ex);
        }
        return null;
    }

    public static List<String> readFileLines(String fd) {
        if (Files.isReadable(new File(fd).toPath())) {
            try (Stream<String> lines = Files.lines(new File(fd).toPath());) {
                List<String> collect = lines.collect(Collectors.toList());
                return collect;
            } catch (Throwable ex) {
                LOG.error(null, ex);
            }
        }
        return null;
    }

    public static boolean isFileExist(String fd) {
        File file = new File(fd);

        return Files.isReadable(new File(fd).toPath());
    }

    public static List<File> getSubFolderList(String folderPath) {
        List<File> foldersList = new ArrayList<>();

        File folder = new File(folderPath);
        try {
            if (folder.exists() && folder.isDirectory()) {
                File[] listOfFiles = folder.listFiles();

                for (int i = 0; i < listOfFiles.length; i++) {
                    try {
                        if (listOfFiles[i].isFile()) {
                        } else if (listOfFiles[i].isDirectory()) {
                            foldersList.add(listOfFiles[i]);
                        }
                    } catch (SecurityException e) {
                    }
                }
            }
            return foldersList;
        } catch (SecurityException e) {
        }
        return null;
    }

    public static List<File> getFileListFromFolder(String folderPath) {
        return getFileListFromFolder(folderPath, false);
    }

    public static List<File> getFileListFromFolder(String folderPath, boolean includeSpecialFiles) {
        List<File> list = new ArrayList<>();

        File folder = new File(folderPath);
        try {
            if (folder.exists() && folder.isDirectory()) {
                File[] listOfFiles = folder.listFiles();

                for (int i = 0; i < listOfFiles.length; i++) {
                    try {
                        if (includeSpecialFiles) {
                            if (!listOfFiles[i].isDirectory()) {
                                list.add(listOfFiles[i]);
                            }
                        } else if (listOfFiles[i].isFile()) {
                            list.add(listOfFiles[i]);

                        }
                    } catch (SecurityException e) {
                    }
                }
            }
            return list;
        } catch (SecurityException e) {
        }
        return null;
    }

    public static String readSingleLineFromFile(String filePath) {
        File f = new File(filePath);
        if (f.isFile()) {
            try (Scanner reader = new Scanner(f)) {
                if (reader.hasNextLine()) {
                    return reader.nextLine();
                }
            } catch (SecurityException | FileNotFoundException | NumberFormatException ex) {
                LOG.error(null, ex);
            }
        }
        return null;
    }

    public static boolean replaceLineInFile(String filePath, String contain, String replaceWith) {
        boolean success = false;
        try (
                BufferedReader br = new BufferedReader(new FileReader(filePath));
                BufferedWriter bw = new BufferedWriter(new FileWriter(filePath + "_temp", false))) {
            StringBuilder sb = new StringBuilder();
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                if (sCurrentLine.contains(contain)) {
                    bw.write(replaceWith + "\n");
                    success = true;
                } else {
                    bw.write(sCurrentLine + "\n");
                }
            }
            if (success) {
                Files.move(Paths.get(filePath + "_temp"), Paths.get(filePath), REPLACE_EXISTING);
            }
            return success;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return success;
    }

    public static String readLineFromFileContain(String filePath, String contain) {
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(filePath));
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                if (sCurrentLine.contains(contain)) {
                    return sCurrentLine;
                }

            }
        } catch (FileNotFoundException ex) {
            java.util.logging.Logger.getLogger(FileUtil.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(FileUtil.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        return null;
    }

    public static byte[] readFile(String filePath) {
        try (FileInputStream input = new FileInputStream(filePath)) {
            // load a properties file
            byte b[] = new byte[input.available()];
            input.read(b, 0, b.length);
            return b;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static String readFileAsString(String filePath) {
        return new String(readFile(filePath));
    }

    public static Properties readPropertiesFromFilePath(String filePath) {
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream(filePath)) {
            // load a properties file
            prop.load(input);
            return prop;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static void saveProperties(Properties p, String file) throws IOException {
        FileOutputStream fr = new FileOutputStream(file);
        p.store(fr, "Properties");
        fr.close();
//        System.out.println("After saving properties: " + p);
    }

    public static boolean updateObjectFromJson(String jsonStr, Object destination) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS, false);

        JsonNode rootNode = objectMapper.readTree(jsonStr.getBytes());
        objectMapper.readerForUpdating(destination).readValue(rootNode.traverse());
        return true;

    }

    public static boolean readJsonObjectFromFileIntoObject(String file, Object destination) {
        return readJsonObjectFromFileIntoObject(file, destination, null, null);
    }

    public static boolean readJsonObjectFromFileIntoObject(String file, Object destination, TypeReference typeRef) {
        return readJsonObjectFromFileIntoObject(file, destination, typeRef, null);
    }

    public static boolean readJsonObjectFromFileIntoObject(String file, Object destination, String ignoreFields[]) {
        return readJsonObjectFromFileIntoObject(file, destination, null, ignoreFields);
    }

    public static boolean readJsonObjectFromFileIntoObject(String file, Object destination, TypeReference typeRef, String ignoreFields[]) {
        if (!Files.exists(Paths.get(file)) && Files.exists(Paths.get(file + FILE_BACKUP_SUFIX))) {
            copyFile(file + FILE_BACKUP_SUFIX, file);
        }

        if (Files.exists(Paths.get(file))) {
            String jsonContent = FileUtil.readFileAsString(file);
            if (jsonContent != null) {
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
                    objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

                    objectMapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);
                    objectMapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false);
                    objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
                    objectMapper.configure(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY, false);
                    objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, false);
                    objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
                    objectMapper.configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, false);
                    objectMapper.configure(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY, false);
                    objectMapper.configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, false);
                    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    objectMapper.configure(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS, false);

                    JsonNode rootNode = objectMapper.readTree(jsonContent.getBytes());
                    if (rootNode == null) {
                        return false;
                    }
                    if (ignoreFields != null) {
                        for (String field : ignoreFields) {
                            ObjectNode oNode = (ObjectNode) rootNode;
                            oNode.remove(field);
                        }
                    }
                    if (typeRef != null) {
                        objectMapper.readerForUpdating(destination).readValue(rootNode.traverse(), typeRef);
                    } else {
                        objectMapper.readerForUpdating(destination).readValue(rootNode.traverse());
                    }
                    return true;

                } catch (IOException ex) {
                    java.util.logging.Logger.getLogger(SystemPropManager.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        try {
            saveObjectToFileAsJson(destination, file);

        } catch (JsonDecodeException ex) {
            java.util.logging.Logger.getLogger(SystemPropManager.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public static boolean saveObjectToFileAsJson(Object object, String file, boolean writeWithBackup) throws JsonDecodeException {
        return saveObjectToFileAsJson(object, file, writeWithBackup, false);
    }

    public static boolean copyFile(String src, String dst) {
        Path copied = Paths.get(dst);
        Path originalPath = Paths.get(src);
        try {
            Files.copy(originalPath, copied, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    public static boolean saveObjectToFileAsJson(Object object, String file) throws JsonDecodeException {
        return saveObjectToFileAsJson(object, file, false, false);
    }

    public static boolean saveObjectToFileAsJson(Object object, String file, boolean writeWithBackup, boolean append) throws JsonDecodeException {
        return saveObjectToFileAsJson(object, file, writeWithBackup, append, null);
    }

    public static boolean saveObjectToFileAsJson(Object object, String file, boolean writeWithBackup, String ignoreFields[]) throws JsonDecodeException {
        return saveObjectToFileAsJson(object, file, writeWithBackup, false, ignoreFields);
    }

    public static boolean saveObjectToFileAsJson(Object object, String filePathStr, boolean writeWithBackup, boolean append, String ignoreFields[]) throws JsonDecodeException {
        File f = new File(filePathStr);
        try {
            Files.createDirectories(f.getParentFile().toPath());
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(SystemPropManager.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        if (writeWithBackup) {
            if (Files.isReadable(f.toPath())) {
                copyFile(filePathStr, filePathStr + FILE_BACKUP_SUFIX);
            }
        }

        try (FileWriter fw = new FileWriter(f, append)) {
            ObjectMapper objectMapper = new ObjectMapper();
            //for prity print
            objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            JsonNode node = objectMapper.valueToTree(object);

            if (ignoreFields != null) {
                for (String field : ignoreFields) {
                    ObjectNode oNode = (ObjectNode) node;
                    oNode.remove(field);
                }
            }
            String strJson = objectMapper.writeValueAsString(node);
//            String strJson = node.asText(); 

            fw.write(strJson);
        } catch (Exception e) {
            return false;
        }

        if (writeWithBackup) {
            copyFile(filePathStr, filePathStr + FILE_BACKUP_SUFIX);
        }

        return true;
    }

}
