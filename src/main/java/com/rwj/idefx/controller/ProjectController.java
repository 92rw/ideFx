package com.rwj.idefx.controller;

import com.rwj.idefx.model.ProjectConfig;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;

public class ProjectController {


    public static void copyFile(File path) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();

        content.putFiles(Collections.singletonList(path));

        clipboard.setContent(content);
    }
    public static boolean deleteFile(File path) throws RuntimeException{
        return false;
    }

    public static void showinExplorer(File file) throws IOException {
        Desktop.getDesktop().open(file.getParentFile());
    }
    public static boolean createProject(ProjectConfig configure){
//        File dir = new File(configure.getPath() + "/" +configure.getName());
//        if(dir.exists() || dir.mkdirs()) {
//            File src = new File(dir.getAbsolutePath() + "/src");
//            createProjectConfigure(configure);
//            ProjectManager.createProject(configure.getName(), configure.getPath());
//            if(!src.mkdir()) return false;
//            if(configure.hasDefaultCode()) {
//                File defaultCodeFile = new File(dir.getAbsolutePath() + "/src/Main.java");
//                try(FileWriter writer = new FileWriter(defaultCodeFile)) {
//                    writer.write(defaultMainCode());
//                    return true;
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    return false;
//                }
//            }
//            return true;
//        } else {
//            return false;
//        }
        System.out.println(configure.toString());
        return true;
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

}
