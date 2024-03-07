package com.rwj.idefx;

import javafx.scene.text.Font;

public class FontsManager {

    public static Font JET_BRAINS_MONO_MEDIUM = load("/fonts/jetbrains/JetBrainsMono-Medium.ttf");
    public static Font JET_BRAINS_MONO_MEDIUM_ITALIC = load("/fonts/jetbrains/JetBrainsMono-MediumItalic.ttf");

    private static Font load(String path) {
        return Font.loadFont(FontsManager.class.getResourceAsStream(path), 13);
    }

}
