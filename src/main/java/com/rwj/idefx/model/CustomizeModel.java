package com.rwj.idefx.model;

import java.io.Serializable;

public class CustomizeModel implements Serializable {
    private static final long serialVersionUID = 114514L;

    private FileModel currentFile;

    public CustomizeModel(FileModel currentFile){
        this.currentFile = currentFile;
    }

    public void setCurrentFile(FileModel currentFile){
        this.currentFile = currentFile;
    }


}