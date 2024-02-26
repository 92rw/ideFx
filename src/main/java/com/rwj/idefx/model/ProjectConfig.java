package com.rwj.idefx.model;

import java.io.Serializable;

public record ProjectConfig(String name, String rootPath, String currentFile, String buildSystem, String language) implements Serializable {
    public ProjectConfig(String name, String rootPath) {
        this(name, rootPath, "","IntelliJ", "Java");
    }
}