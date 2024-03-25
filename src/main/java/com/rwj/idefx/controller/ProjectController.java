package com.rwj.idefx.controller;

import com.rwj.idefx.model.CustomizeModel;
import com.rwj.idefx.model.FileModel;
import com.rwj.idefx.model.ProjectConfig;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.awt.Desktop;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;

public class ProjectController {

    private static String configFile;
    private static CustomizeModel customizeModel;

    public static void copyFile(File path) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();

        content.putFiles(Collections.singletonList(path));

        clipboard.setContent(content);
    }

    public static String getProjectPath() {
        return new File(configFile).getParent();
    }
    public static FileModel loadCurrentFile(FileModel model) {
        if (configFile == null) {
            configFile = model.filePath() + File.separator + ".idefx";
            try (FileInputStream fileIn = new FileInputStream(configFile);
                 ObjectInputStream in = new ObjectInputStream(fileIn)) {
                customizeModel = (CustomizeModel) in.readObject();
            } catch (ClassNotFoundException | IOException e) {
                throw new RuntimeException(e);
            }
        }
        return customizeModel.getCurrentFile();
    }


    private ProjectController(){}
    public static void setCurrentFile(File file) {
        customizeModel.setCurrentFile(new FileModel(file.getName(), file.getAbsolutePath()));
    }
    public static void showinExplorer(File file) throws IOException {
        if (file.isDirectory()) {
            Desktop.getDesktop().open(file);
        } else {
            Desktop.getDesktop().open(file.getParentFile());
        }
    }

    public static FileModel createProject(ProjectConfig configure) throws IOException {
        File dir = new File(configure.rootPath());
        if(dir.exists() || dir.mkdirs()) {
            configFile = dir.getAbsolutePath() + File.separator + ".idefx";
            File src = new File(dir, "src");
            if (!src.mkdir()) System.out.println("This project already exists source folder");
            FileModel currentFile = new FileModel(src.getName(), src.getAbsolutePath());
            if (!configure.currentFile().equals("")) {
                File defaultCodeFile = new File(src, "Main.java");
                currentFile = new FileModel(defaultCodeFile.getName(), defaultCodeFile.getAbsolutePath());
                try (FileWriter writer = new FileWriter(defaultCodeFile)) {
                    writer.write(defaultMainCode());
                }
            }
            saveCustomizeModel(currentFile);
        } else {
            throw new IOException("Fail to create project path");
        }
        return new FileModel(configure.name(), dir.getAbsolutePath());
    }

    public static void saveCustomizeModel(FileModel file) throws IOException {
        if (file != null) customizeModel = new CustomizeModel(file);
        try (FileOutputStream fileOut = new FileOutputStream(configFile);
             ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(customizeModel);
        }
    }

    public static void saveContext(String content, Path filePath) throws IOException {
        if (filePath == null) throw new IOException("Can only save java files");
        Files.write(filePath, content.getBytes());
    }
    public static List<File> getFilesFromClipboard() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard.hasFiles()) {
            List<File> files = clipboard.getFiles();
            return files;
        }
        return null;
    }

    public static void pasteFiles(File targetDirectory) throws IOException {
        List<File> filesToPaste = getFilesFromClipboard();
        if (filesToPaste != null && targetDirectory.isDirectory()) {
            for (File file : filesToPaste) {
                File destFile = new File(targetDirectory, file.getName());
                // 处理重名文件
                int count = 1;
                String fileName = file.getName();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                String extension = fileName.substring(fileName.lastIndexOf('.'));
                while (destFile.exists()) {
                    String newFileName = baseName + "_" + count + extension;
                    destFile = new File(targetDirectory, newFileName);
                    count++;
                }
                Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
    private static String defaultMainCode(){
        return "public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"Hello World!\");\n" +
                "    }\n" +
                "}";
    }
    public static boolean deleteFiles(File directory) {
        if (!directory.isDirectory()) {
            return directory.delete();
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFiles(file);
                } else {
                    file.delete();
                }
            }
        }

        // 删除文件夹本身
        return directory.delete();
    }

}

