package com.rwj.idefx.view;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCombination;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

public class ContextMenuBuilder {
    private final ContextMenu contextMenu;

    private ContextMenuBuilder() {
        this.contextMenu = new ContextMenu();
    }

    public static ContextMenuBuilder create() {
        return new ContextMenuBuilder();
    }

    public ContextMenuBuilder addSeperate() {
        contextMenu.getItems().add(new SeparatorMenuItem());
        return this;
    }


    public ContextMenuBuilder addMenuItem(String text, Runnable action) {
        MenuItem menuItem = new MenuItem(text);
        menuItem.setOnAction(event -> action.run());
        contextMenu.getItems().add(menuItem);
        return this;
    }

    public ContextMenuBuilder addMenuItem(String text, Ikon graphic, KeyCombination accelerator, Boolean quickAction, Runnable action) {
        MenuItem item = new MenuItem(text);
        if (graphic != null) {
            item.setGraphic(new FontIcon(graphic));
        }

        if (accelerator != null) {
            item.setAccelerator(accelerator);
        }

        if (null != quickAction && quickAction) {
            item.setMnemonicParsing(true);
        }

        item.setOnAction(event -> action.run());
        contextMenu.getItems().add(item);
        return this;
    }

    public ContextMenu build() {
        return contextMenu;
    }
}