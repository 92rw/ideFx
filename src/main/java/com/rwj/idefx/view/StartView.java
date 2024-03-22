package com.rwj.idefx.view;

import atlantafx.base.theme.Styles;
import com.rwj.idefx.controller.ApplicationController;
import com.rwj.idefx.controller.ProjectController;
import com.rwj.idefx.model.FileModel;
import com.rwj.idefx.model.ProjectConfig;
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
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class StartView extends Application {
    private ListView<FileModel> projectListView;
    private Button openProjectButton, newProjectButton, settingButton, backButton;

    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        ApplicationController.setTheme();

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
                enterProject(createdProject);
            }
        });

        settingButton.setOnAction(event -> primaryStage.setScene(scene2));

        backButton.setOnAction(event -> {
            primaryStage.setScene(scene1);
            ApplicationController.saveConfigure();
        });
    }

    private void loadProject(){
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Project");
        File selectedDirectory = directoryChooser.showDialog(primaryStage);
        if (selectedDirectory != null) {
            String name = selectedDirectory.getName();
            String path = selectedDirectory.getAbsolutePath();

            ProjectConfig loadedProjectConfig = new ProjectConfig(name, path);
            FileModel loadedProjectModel = null;
            try {
                loadedProjectModel = ProjectController.createProject(loadedProjectConfig);
            } catch (IOException e) {
                DialogView.alertException("Error when handling your request",e);
            }
            if (loadedProjectModel != null) {
                enterProject(loadedProjectModel);
            }
        }
    }

    private void enterProject(FileModel project){
        ApplicationController.addProject(project);
        new MainView(project, ApplicationController.currentTheme()).display(primaryStage);
    }

    private Scene createScene1() {
        // 创建第一个场景
        BorderPane borderPane1 = new BorderPane();

        Text topText1 = new Text("Welcome to ideFx");
        topText1.getStyleClass().addAll(Styles.TITLE_1);
        BorderPane.setAlignment(topText1, Pos.CENTER);

        newProjectButton = new Button("Create Project", new FontIcon(Feather.PLUS));
        openProjectButton = new Button("Open Project", new FontIcon(Feather.FOLDER));
        settingButton = new Button("Settings", new FontIcon(Feather.SETTINGS));

        var toolbar1 = new ToolBar(newProjectButton, openProjectButton, settingButton);

        TextField searchField = new TextField();
        searchField.setPromptText("Search for your project");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            // 假设有一个方法filterProjects根据搜索关键字过滤项目
            List<FileModel> filteredProjects = filterProjects(newValue);

            // 更新项目列表视图的项目
            projectListView.setItems(FXCollections.observableArrayList(filteredProjects));
        });

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
        comboBox.setValue(ApplicationController.currentTheme());
        comboBox.setOnAction(e -> {
            String selectedTheme = comboBox.getValue();
            ApplicationController.setThemeByName(selectedTheme);
        });
        gridPane.addRow(0,new Label("Theme:"), comboBox);
        borderPane2.setTop(topText2);
        borderPane2.setCenter(gridPane);
        borderPane2.setBottom(backButton);

        return new Scene(borderPane2, 380, 500);
    }
    private ListView<FileModel> createProjectList(){
        projectListView = new ListView<>();
        VBox.setMargin(projectListView, new Insets(10, 10, 10, 10));
        Styles.toggleStyleClass(projectListView, Styles.BORDERED);

        ObservableList<FileModel> items = FXCollections.observableArrayList(ApplicationController.currentProjects());
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

        ContextMenu contextMenu = ContextMenuBuilder.create()
                .addMenuItem("Move Up", () -> {
                    int selectedIndex = projectListView.getSelectionModel().getSelectedIndex();
                    if (selectedIndex > 0) {
                        ApplicationController.moveUp(selectedIndex);
                        items.setAll(ApplicationController.currentProjects());
                        projectListView.getSelectionModel().select(selectedIndex - 1);
                    }
                })
                .addMenuItem("Move Down", () -> {
                    int selectedIndex = projectListView.getSelectionModel().getSelectedIndex();
                    if (selectedIndex < items.size() - 1) {
                        ApplicationController.moveDown(selectedIndex);
                        items.setAll(ApplicationController.currentProjects());
                        projectListView.getSelectionModel().select(selectedIndex + 1);
                    }
                })
                .addMenuItem("Delete", () -> {
                    FileModel selectedProject = projectListView.getSelectionModel().getSelectedItem();
                    if (selectedProject != null) {
                        ApplicationController.delete(selectedProject);
                        items.setAll(ApplicationController.currentProjects());
                    }
                }).build();

        projectListView.setContextMenu(contextMenu);

        return projectListView;
    }
    private List<FileModel> filterProjects(String keyword) {
        return ApplicationController.currentProjects().stream()
                .filter(project -> project.filePath().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

}
