package com.rwj.idefx.view;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

public class ContextMenuBuilder {
    private final ContextMenu contextMenu;

    private ContextMenuBuilder() {
        this.contextMenu = new ContextMenu();
    }

    public static ContextMenuBuilder create() {
        return new ContextMenuBuilder();
    }

    public ContextMenuBuilder addMenuItem(String text, Runnable action) {
        MenuItem menuItem = new MenuItem(text);
        menuItem.setOnAction(event -> action.run());
        contextMenu.getItems().add(menuItem);
        return this;
    }

    public ContextMenu build() {
        return contextMenu;
    }
}