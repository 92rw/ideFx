package com.rwj.idefx.model;

import java.io.Serializable;

public record ProjectConfig(String name, String rootPath, String currentFile, String buildSystem, String language)
        implements Serializable {private static final long serialVersionUID = 1L;}