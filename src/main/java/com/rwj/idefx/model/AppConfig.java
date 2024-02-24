package com.rwj.idefx.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class AppConfig implements Serializable {
    private static final long serialVersionUID = 114514L;

    private final List<FileModel> projectList;
    private ThemeInfo theme;

    public AppConfig(){
        projectList = new ArrayList<>();
        theme = ThemeInfo.THEME4;
    }

    public ThemeInfo getTheme() {
        return theme;
    }

    public void setTheme(ThemeInfo theme) {
        this.theme = theme;
        theme.setTheme();
    }

    public void addProjectToList(FileModel project){
        projectList.add(project);
    }
    public void moveProjectUp(int index) {
        if (index > 0 && index < projectList.size()) {
            Collections.swap(projectList, index, index - 1);
        }
    }

    public void moveProjectDown(int index) {
        if (index >= 0 && index < projectList.size() - 1) {
            Collections.swap(projectList, index, index + 1);
        }
    }


    public void removeProjectIf(Predicate<FileModel> predicate){
        projectList.removeIf(predicate);
    }

    public List<FileModel> getProjectList(){
        return projectList;
    }
}
