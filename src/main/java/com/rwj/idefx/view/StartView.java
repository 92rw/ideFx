package com.rwj.idefx.view;

import atlantafx.base.theme.Styles;
import com.rwj.idefx.controller.ApplicationController;
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
    private Button openProjectButton, newProjectButton, settingButton, backButton;

    @Override
    public void start(Stage primaryStage) {
        appConfig.getTheme().setTheme();

        Scene scene1 = createScene1();
        Scene scene2 = createScene2();
        primaryStage.setScene(scene1);

        // 设置舞台标题
        primaryStage.setTitle("ideFX Welcome Window");
        primaryStage.setResizable(false);
        // 显示舞台
        primaryStage.show();

        openProjectButton.setOnAction(event ->loadProject());
        newProjectButton.setOnAction(event -> {
            CreateProjectDialog newProjectDialog = new CreateProjectDialog();
            FileModel createdProject = newProjectDialog.showDialog(primaryStage);
            if (createdProject != null) {
                primaryStage.close();
                appConfig.addProjectToList(createdProject);
                ApplicationController.saveConfigure(appConfig);
                enterProject(createdProject);
            }
        });

        settingButton.setOnAction(event -> primaryStage.setScene(scene2));

        backButton.setOnAction(event -> {
            primaryStage.setScene(scene1);
            ApplicationController.saveConfigure(appConfig);
        });
    }
    public static void setAppConfig(AppConfig config) {
        appConfig = config;
    }

    private void loadProject(){
        //TODO 给“打开项目”按钮增加功能
    }
    private void enterProject(FileModel project){
        System.out.println("打开项目" + project.filePath());
    }

    private Scene createScene1() {
        // 创建第一个场景
        BorderPane borderPane1 = new BorderPane();

        Text topText1 = new Text("Welcome to ideFx");
        topText1.getStyleClass().addAll(Styles.TITLE_1);
        BorderPane.setAlignment(topText1, Pos.CENTER);

        newProjectButton = new Button("Create Project", new FontIcon(Feather.PLUS));
        openProjectButton = new Button("Open Project", new FontIcon(Feather.FOLDER));
        settingButton = new Button("Setting", new FontIcon(Feather.SETTINGS));

        var toolbar1 = new ToolBar(newProjectButton, openProjectButton, settingButton);

        TextField searchField = new TextField();
        searchField.setPromptText("Search for your project");
        VBox.setMargin(searchField, new Insets(10, 10, 0, 10));

        VBox centerVBox = new VBox(searchField, createProjectList());
        borderPane1.setTop(topText1);
        borderPane1.setCenter(centerVBox);
        borderPane1.setBottom(toolbar1);

        return new Scene(borderPane1, 380, 500);
    }

    private Scene createScene2() {
        Text topText2 = new Text("Setting");
        topText2.getStyleClass().addAll(Styles.TITLE_1);
        BorderPane.setAlignment(topText2, Pos.CENTER);

        GridPane gridPane = new GridPane();
        gridPane.setVgap(10);
        gridPane.setHgap(10);
        gridPane.setPadding(new Insets(30));
        BorderPane.setMargin(gridPane, new Insets(10));


        BorderPane borderPane2 = new BorderPane();
        backButton = new Button("Back", new FontIcon(Feather.ARROW_LEFT));
        BorderPane.setAlignment(backButton, Pos.BOTTOM_RIGHT);

        List<String> themeNames = ThemeInfo.getThemeNames();
        ObservableList<String> options = FXCollections.observableArrayList(themeNames);
        ComboBox<String> comboBox = new ComboBox<>(options);
        comboBox.setValue(appConfig.getTheme().getDisplayName());
        comboBox.setOnAction(e -> {
            String selectedTheme = comboBox.getValue();
            ThemeInfo themeByName = ApplicationController.getThemeByName(selectedTheme);
            if (themeByName != null) appConfig.setTheme(themeByName);
        });
        gridPane.addRow(0,new Label("Theme:"), comboBox);
        borderPane2.setTop(topText2);
        borderPane2.setCenter(gridPane);
        borderPane2.setBottom(backButton);

        return new Scene(borderPane2, 380, 500);
    }
    private ListView<FileModel> createProjectList(){
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

        ContextMenu contextMenu = new ContextMenu();
        MenuItem moveUpItem = new MenuItem("Move Up");
        MenuItem moveDownItem = new MenuItem("Move Down");
        MenuItem deleteItem = new MenuItem("Delete");

        moveUpItem.setOnAction(actionEvent -> {
            int selectedIndex = projectListView.getSelectionModel().getSelectedIndex();
            if (selectedIndex > 0) {
                appConfig.moveProjectUp(selectedIndex);
                items.setAll(appConfig.getProjectList());
                projectListView.getSelectionModel().select(selectedIndex - 1);

            }
        });
        moveDownItem.setOnAction(actionEvent -> {
            int selectedIndex = projectListView.getSelectionModel().getSelectedIndex();
            if (selectedIndex < items.size() - 1) {
                appConfig.moveProjectDown(selectedIndex);
                items.setAll(appConfig.getProjectList());
                projectListView.getSelectionModel().select(selectedIndex + 1);
            }

        });
        deleteItem.setOnAction(actionEvent -> {
            FileModel selectedProject = projectListView.getSelectionModel().getSelectedItem();
            if (selectedProject != null) {
                appConfig.removeProjectIf(project -> project.equals(selectedProject));
                items.setAll(appConfig.getProjectList());
            }
        });

        contextMenu.getItems().addAll(moveUpItem,moveDownItem,deleteItem);
        projectListView.setContextMenu(contextMenu);

        return projectListView;
    }
}