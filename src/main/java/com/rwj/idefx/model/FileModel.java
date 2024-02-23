package com.rwj.idefx.model;

import java.io.Serializable;

public record FileModel(String fileName, String filePath) implements Serializable {private static final long serialVersionUID = 1L;}