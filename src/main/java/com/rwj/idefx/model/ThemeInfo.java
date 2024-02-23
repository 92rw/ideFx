package com.rwj.idefx.model;

import atlantafx.base.theme.*;
import javafx.application.Application;

import java.util.ArrayList;
import java.util.List;

public enum ThemeInfo {
    THEME1("Primer Light",() -> {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());}),
    THEME2("Primer Dark",() -> {Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());}),
    THEME3("Nord Light",() -> {Application.setUserAgentStylesheet(new NordLight().getUserAgentStylesheet());}),
    THEME4("Nord Dark",() -> {Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());}),
    THEME5("Cupertino Light",() -> {Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());}),
    THEME6("Cupertino Dark",() -> {Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet());}),
    THEME7("Dracula",() -> {Application.setUserAgentStylesheet(new Dracula().getUserAgentStylesheet());});
    private final String displayName;
    private final Runnable action;
    ThemeInfo(String displayName, Runnable action) {
        this.displayName = displayName;
        this.action = action;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setTheme() {
        action.run();
    }
    public static List<String> getThemeNames() {
        List<String> themeNames = new ArrayList<>();
        for(ThemeInfo theme: values()) {
            themeNames.add(theme.getDisplayName());
        }
        return themeNames;
    }
}
