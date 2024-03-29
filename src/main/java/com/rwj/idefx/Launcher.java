package com.rwj.idefx;

import com.rwj.idefx.controller.ApplicationController;
import com.rwj.idefx.view.StartView;
import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        ApplicationController.loadAppConfig();
        FontLoader.load();
        Application.launch(StartView.class, args);
    }
}
