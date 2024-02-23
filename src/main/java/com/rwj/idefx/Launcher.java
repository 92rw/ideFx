package com.rwj.idefx;

import com.rwj.idefx.controller.FileController;
import com.rwj.idefx.model.AppConfig;
import com.rwj.idefx.view.StartView;
import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        AppConfig appConfig = FileController.loadAppConfig();
        StartView.setAppConfig(appConfig);
        Application.launch(StartView.class, args);
    }
}
