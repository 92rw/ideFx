package com.rwj.idefx.controller;

import com.rwj.idefx.model.AppConfig;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileController {
    private static FileController INSTANCE;
    private static AppConfig appConfig;

    private static final String configPath = "files/config";

    private FileController(){}
    public static AppConfig loadAppConfig() {
        File file = new File(configPath);
        if(!file.exists()) {
            try {
                if(file.createNewFile()) {
                    appConfig = new AppConfig();
                    saveConfigure();
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
        INSTANCE = new FileController();
        return appConfig;
    }

    public static void saveConfigure(){
        try (ObjectOutputStream stream = new ObjectOutputStream(Files.newOutputStream(Paths.get(configPath)))){
            stream.writeObject(appConfig);
            stream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

