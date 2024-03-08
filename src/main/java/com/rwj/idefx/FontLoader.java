package com.rwj.idefx;

import javafx.scene.text.Font;

public class FontLoader {

    public static void load() {
        Font.loadFont(FontLoader.class.getResourceAsStream("/fonts/jetbrains/JetBrainsMono-Medium.ttf"), 13);
    }

}
