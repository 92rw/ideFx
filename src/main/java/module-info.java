module com.rwj.idefx {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.feather;
    requires atlantafx.base;

    opens com.rwj.idefx to javafx.fxml;
//    exports com.rwj.idefx.controller;
//    exports com.rwj.idefx.model;
    exports com.rwj.idefx;
}