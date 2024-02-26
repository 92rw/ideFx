package com.rwj.idefx.controller;

import com.rwj.idefx.model.AppConfig;
import com.rwj.idefx.model.FileModel;
import com.rwj.idefx.model.ThemeInfo;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ApplicationController {
    private static ApplicationController INSTANCE;
    private static AppConfig appConfig;

    private static final String configPath = "files/config";

    private ApplicationController(){}

    public static AppConfig loadAppConfig() {
        File configFile = new File(configPath);
        File parentDir = configFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs(); // 创建所有不存在的父目录
        }

        try {
            if (!configFile.exists()) {
                if (!configFile.createNewFile()) {
                    throw new RuntimeException("Fail to create configuration file: " + configFile.getAbsolutePath());
                }
                appConfig = new AppConfig();
                saveConfigure();
            } else {
                try (ObjectInputStream stream = new ObjectInputStream(Files.newInputStream(configFile.toPath()))) {
                    appConfig = (AppConfig) stream.readObject();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Fail to cast AppConfig class when loading configuration file");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Fail to read configuration file: " + configFile.getAbsolutePath(), e);
        }
        INSTANCE = new ApplicationController();
        return appConfig;
    }

    public static ApplicationController getINSTANCE() {
        return INSTANCE;
    }

    public static void addProject(FileModel project){
        appConfig.removeProjectIf(proj -> proj.equals(project));
        appConfig.addProjectToList(project);
        saveConfigure();
    }

    public static String currentTheme() {
        return appConfig.getTheme().getDisplayName();
    }

    public static List<FileModel> currentProjects() {
        return appConfig.getProjectList();
    }
    public static void saveConfigure(){
        try (ObjectOutputStream stream = new ObjectOutputStream(Files.newOutputStream(Paths.get(configPath)))){
            stream.writeObject(appConfig);
            stream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void delete(FileModel selectedProject){
        appConfig.removeProjectIf(project -> project.equals(selectedProject));
    }

    public static void moveUp(int projectIndex){
        appConfig.moveProjectUp(projectIndex);
    }

    public static void moveDown(int projectIndex){
        appConfig.moveProjectDown(projectIndex);
    }
    public static void setThemeByName(String themeName) {
        for (ThemeInfo value : ThemeInfo.values()) {
            if (value.getDisplayName().equals(themeName)) {
                appConfig.setTheme(value);
                setTheme();
            }
        }
    }

    public static void setTheme() {
        appConfig.getTheme().setTheme();
    }
    public static boolean validString(String input) {
        return null != input && !input.trim().isEmpty();
    }
}
