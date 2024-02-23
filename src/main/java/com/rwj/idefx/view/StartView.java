package com.rwj.idefx.view;

import atlantafx.base.theme.Styles;
import com.rwj.idefx.model.AppConfig;
import com.rwj.idefx.model.FileModel;
import com.rwj.idefx.model.ThemeInfo;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class StartView extends Application {
    private static AppConfig appConfig;

    @Override
    public void start(Stage primaryStage) {
        appConfig.getTheme().setTheme();
        // 创建第一个场景
        BorderPane borderPane1 = new BorderPane();

        Text topText1 = new Text("Welcome to ideFx");
        topText1.getStyleClass().addAll(Styles.TITLE_1);
        BorderPane.setAlignment(topText1, Pos.CENTER);

        Button newProjectButton = new Button("Create Project", new FontIcon(Feather.PLUS));
        Button openProjectButton = new Button("Open Project", new FontIcon(Feather.FOLDER));
        Button settingButton = new Button("Setting", new FontIcon(Feather.SETTINGS));

        var toolbar1 = new ToolBar(newProjectButton, openProjectButton, settingButton);


        TextField searchField = new TextField();
        searchField.setPromptText("Search for your project");
        VBox.setMargin(searchField, new Insets(10, 10, 0, 10));

        ListView<FileModel> projectListView = new ListView<>();
        VBox.setMargin(projectListView, new Insets(10, 10, 10, 10));
        Styles.toggleStyleClass(projectListView, Styles.BORDERED);


        ObservableList<FileModel> items = FXCollections.observableArrayList(appConfig.getProjectList());
        projectListView.setItems(items);
        projectListView.setCellFactory(param -> new ProjectListCell());
        projectListView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                FileModel selectedProject = projectListView.getSelectionModel().getSelectedItem();
                if (selectedProject != null) {
                    enterProject(selectedProject);
                }
            }
        });

        VBox centerVBox = new VBox(searchField, projectListView);
        borderPane1.setTop(topText1);
        borderPane1.setCenter(centerVBox);
        borderPane1.setBottom(toolbar1);

        Scene scene1 = new Scene(borderPane1, 380, 500);

        // 创建第二个场景
        Text topText2 = new Text("Setting");
        topText2.getStyleClass().addAll(Styles.TITLE_1);
        BorderPane.setAlignment(topText2, Pos.CENTER);

        GridPane gridPane = new GridPane();
        gridPane.setVgap(10);
        gridPane.setHgap(10);
        gridPane.setPadding(new Insets(30));
        BorderPane.setMargin(gridPane, new Insets(10));

        BorderPane borderPane2 = new BorderPane();
        Button backButton = new Button("Back", new FontIcon(Feather.ARROW_LEFT));
        BorderPane.setAlignment(backButton, Pos.BOTTOM_RIGHT);

        List<String> themeNames = ThemeInfo.getThemeNames();
        ObservableList<String> options = FXCollections.observableArrayList(themeNames);
        ComboBox<String> comboBox = new ComboBox<>(options);
        comboBox.setValue(appConfig.getTheme().getDisplayName());
        comboBox.setOnAction(e -> {
            String selectedTheme = comboBox.getValue();
            ThemeInfo themeByName = getThemeByName(selectedTheme);
            if (themeByName != null) appConfig.setTheme(themeByName);
        });
        gridPane.addRow(0,new Label("Theme:"), comboBox);
        borderPane2.setTop(topText2);
        borderPane2.setCenter(gridPane);
        borderPane2.setBottom(backButton);

        Scene scene2 = new Scene(borderPane2, 380, 500);


        // 设置初始场景
        primaryStage.setScene(scene1);

        // 设置舞台标题
        primaryStage.setTitle("ideFX Welcome Window");
        primaryStage.setResizable(false);
        // 显示舞台
        primaryStage.show();

        // 切换到第二个场景

        newProjectButton.setOnAction(event ->{});
        settingButton.setOnAction(event -> primaryStage.setScene(scene2));
        backButton.setOnAction(event -> {
            primaryStage.setScene(scene1);
            //TODO 保存当前appConfig到指定目录
        });
    }

    private ThemeInfo getThemeByName(String themeName) {
        for (ThemeInfo value : ThemeInfo.values()) {
            if (value.getDisplayName().equals(themeName)) {
                return value;
            }
        }
        return null;
    }
    public static void setAppConfig(AppConfig config) {
        appConfig = config;
    }
    private void enterProject(FileModel project){

    }
}

