package com.rwj.idefx.controller;

import com.rwj.idefx.model.CustomizeModel;
import com.rwj.idefx.model.FileModel;
import com.rwj.idefx.model.ProjectConfig;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.awt.Desktop;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;

public class ProjectController {

    private static String configFile;

    private static FileModel currentFile;

    public static void copyFile(File path) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();

        content.putFiles(Collections.singletonList(path));

        clipboard.setContent(content);
    }

    public static String loadCurrentFile(FileModel model) {
        if (configFile == null) {
            configFile = model.filePath() + File.separator + ".idefx";
        }
        try (FileInputStream fileIn = new FileInputStream(configFile);
             ObjectInputStream in = new ObjectInputStream(fileIn)) {
            currentFile = (FileModel) in.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return currentFile.fileName();
    }


    private ProjectController(){}
    public static void setCurrentFile(File file) {
        currentFile = new FileModel(file.getName(), file.getAbsolutePath());
    }
    public static void showinExplorer(File file) throws IOException {
        Desktop.getDesktop().open(file.getParentFile());
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
            saveCurrentFile(currentFile);
        } else {
            throw new IOException("Fail to create project path");
        }
        return new FileModel(configure.name(), dir.getAbsolutePath());
    }
    public static CustomizeModel initProjectInfo(String rootPath) {
        File file = new File(rootPath, ".idefx");
        //TODO 完善初始化逻辑
        return null;
    }


    public static void saveCurrentFile(FileModel currentFile) throws IOException {
        try (FileOutputStream fileOut = new FileOutputStream(configFile);
             ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(currentFile);
        }
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

        return directory.delete();
    }

}
