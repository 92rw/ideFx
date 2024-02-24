package com.rwj.idefx.view;

import com.rwj.idefx.controller.ApplicationController;
import com.rwj.idefx.controller.ProjectController;
import com.rwj.idefx.model.FileModel;
import com.rwj.idefx.model.ProjectConfig;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Optional;

public class CreateProjectDialog {

    private TextField nameField, pathField;
    private Stage stage;
    private GridPane gridPane;

    private Label pathLabel;

    private CheckBox samplecodeBox;

    ToggleGroup projectLanguage, projectSystem;

    public FileModel showDialog(Stage primaryStage) {
        stage = primaryStage;
        Dialog<FileModel> dialog = new Dialog<>();
        dialog.initModality(Modality.APPLICATION_MODAL); // 设置为模态窗口
        dialog.setTitle("Create Project");
        dialog.initOwner(primaryStage);

        createGridPane();
        dialog.getDialogPane().setContent(gridPane);


        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                // 用户点击创建，调用 createProject 并返回 FileModel
                ProjectConfig project = createProject();
                if (project != null) {
                    return new FileModel(project.name(), project.rootPath());
                }
            }
            // 对于其他情况（包括取消），返回 null
            return null;
        });

        Optional<FileModel> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private void setProjectLocation() {
        if (ApplicationController.validString(nameField.getText()) && ApplicationController.validString(pathField.getText())) {
            pathLabel.setText(pathField.getText() + "\\" + nameField.getText());
        }
    }
    private ProjectConfig createProject() {
        if (!ApplicationController.validString(pathField.getText())) return null;
        String firstPath = samplecodeBox.isSelected() ? "/src/Main.java" : "";
        ToggleButton selectedSystem = (ToggleButton) projectSystem.getSelectedToggle();
        ToggleButton selectedLanguage = (ToggleButton) projectLanguage.getSelectedToggle();
        String projectSystem = selectedSystem.getText();
        String projectLanguage = selectedLanguage.getText();
        ProjectConfig projectConfig = new ProjectConfig(nameField.getText(), pathLabel.getText(), firstPath,projectSystem, projectLanguage);

        if (ProjectController.createProject(projectConfig)) {
            return projectConfig;
        } else {
            return null;
        }
    }
    private void createGridPane() {
        gridPane = new GridPane();
        gridPane.setVgap(10);
        gridPane.setHgap(10);
        gridPane.setPadding(new Insets(30));
        BorderPane.setMargin(gridPane, new Insets(10));

        ColumnConstraints col1 = new ColumnConstraints(100, 100, Double.MAX_VALUE);
        ColumnConstraints col2 = new ColumnConstraints(500, 500, Double.MAX_VALUE);

        gridPane.getColumnConstraints().addAll(col1, col2);

        pathLabel = new Label("");
        pathLabel.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS); // 设置文本溢出策略为省略号
        pathLabel.setMaxWidth(260);

        nameField = new TextField();
        nameField.setPromptText("Please key in the project name");
        nameField.setOnKeyTyped(e -> setProjectLocation());

        pathField = new TextField();
        pathField.setPromptText("Please select the project path");
        pathField.setOnKeyTyped(e -> setProjectLocation());
        HBox.setHgrow(pathField, Priority.ALWAYS);


        samplecodeBox = new CheckBox("Add sample code");
        samplecodeBox.setSelected(true);
        gridPane.add(samplecodeBox, 1, 5);


        gridPane.addRow(0,new Label("Name:"), nameField);
        gridPane.addRow(1, new Label("Location:"), pathBox());
        gridPane.addRow(2, new Region(), new HBox(new Label("Project will be created in: "), pathLabel));
        gridPane.addRow(3, new Label("Language:"), languageBox());
        gridPane.addRow(4, new Label("Build system:"), systemBox());
    }

    private HBox pathBox() {
        Button pathButton = new Button(null, new FontIcon(Feather.FOLDER));
        pathButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select Directory");
            var selectedDirectory = directoryChooser.showDialog(stage);
            if (selectedDirectory != null) {
                pathField.setText(selectedDirectory.getAbsolutePath());
                setProjectLocation();
            }
        });

        return new HBox(pathField, pathButton);
    }

    private HBox languageBox() {
        projectLanguage = new ToggleGroup();
        ToggleButton languageBtn1 = new ToggleButton("Java");
        languageBtn1.setToggleGroup(projectLanguage);
        languageBtn1.setSelected(true);

        ToggleButton languageBtn2 = new ToggleButton("Kotlin");
        languageBtn2.setToggleGroup(projectLanguage);

        ToggleButton languageBtn3 = new ToggleButton("Groovy");
        languageBtn3.setToggleGroup(projectLanguage);

        ToggleButton languageBtn4 = new ToggleButton("HTML");
        languageBtn4.setToggleGroup(projectLanguage);

        languageBtn2.setDisable(true);
        languageBtn3.setDisable(true);
        languageBtn4.setDisable(true);

        return new HBox(languageBtn1, languageBtn2, languageBtn3, languageBtn4);
    }

    private HBox systemBox() {
        projectSystem = new ToggleGroup();
        ToggleButton systemBtn1 = new ToggleButton("IntelliJ");
        systemBtn1.setToggleGroup(projectSystem);
        systemBtn1.setSelected(true);

        ToggleButton systemBtn2 = new ToggleButton("Maven");
        systemBtn2.setToggleGroup(projectSystem);

        ToggleButton systemBtn3 = new ToggleButton("Gradle");
        systemBtn3.setToggleGroup(projectSystem);

        systemBtn2.setDisable(true);
        systemBtn3.setDisable(true);

        return new HBox(systemBtn1, systemBtn2, systemBtn3);
    }
}
