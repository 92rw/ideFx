module com.rwj.idefx {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.feather;
    requires atlantafx.base;
    requires org.fxmisc.richtext;
    requires org.fxmisc.flowless;
    requires reactfx;
    requires org.kordamp.ikonli.core;

    opens com.rwj.idefx to javafx.fxml;
    exports com.rwj.idefx.controller;
    exports com.rwj.idefx.model;
    exports com.rwj.idefx.view;
    exports com.rwj.idefx;
}