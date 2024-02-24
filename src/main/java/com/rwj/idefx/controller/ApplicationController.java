package com.rwj.idefx.controller;

import com.rwj.idefx.model.AppConfig;
import com.rwj.idefx.model.ThemeInfo;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ApplicationController {
    private static ApplicationController INSTANCE;
    private static AppConfig appConfig;

    private static final String configPath = "files/config";

    private ApplicationController(){}
    public static AppConfig loadAppConfig() {
        File file = new File(configPath);
        if(!file.exists()) {
            try {
                if(file.createNewFile()) {
                    appConfig = new AppConfig();
                    saveConfigure(appConfig);
                } else {
                    throw new RuntimeException("无法创建配置文件！");
                }
            } catch (IOException e) {
                throw new RuntimeException("创建配置文件失败", e);
            }
        }
        else {
            try (ObjectInputStream stream = new ObjectInputStream(Files.newInputStream(file.toPath()))) {
                appConfig = (AppConfig) stream.readObject();
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException("加载配置文件失败", e);
            }
        }
        INSTANCE = new ApplicationController();
        return appConfig;
    }

    public static void saveConfigure(AppConfig newConfig){
        try (ObjectOutputStream stream = new ObjectOutputStream(Files.newOutputStream(Paths.get(configPath)))){
            appConfig = newConfig;
            stream.writeObject(appConfig);
            stream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ThemeInfo getThemeByName(String themeName) {
        for (ThemeInfo value : ThemeInfo.values()) {
            if (value.getDisplayName().equals(themeName)) {
                return value;
            }
        }
        return null;
    }

    public static boolean validString(String input) {
        return null != input && !input.trim().isEmpty();
    }
}
