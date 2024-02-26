package com.rwj.idefx.view;

import com.rwj.idefx.controller.ProjectController;
import com.rwj.idefx.controller.RuntimeController;
import com.rwj.idefx.model.ExecutionResult;
import com.rwj.idefx.model.FileModel;
import javafx.beans.binding.Bindings;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.Optional;


public class MainView {
    private FileModel project;
    private TextArea fileContentArea;

    private TextArea runtimeText;

    private TreeView<File> directoryTree;

    private ComboBox<String> fileComboBox;
    public MainView(FileModel project) {
        this.project = project;
    }

    public void display(Stage ownerStage) {

        Button newButton = new Button("New", new FontIcon(Feather.PLUS));
        Button projectButton = new Button("Project", new FontIcon(Feather.FOLDER));

        Button runButton = new Button("Run", new FontIcon(Feather.PLAY));
        Button compileButton = new Button("Compile", new FontIcon(Feather.LAYERS));
        Button refreshButton = new Button("Refresh", new FontIcon(Feather.ROTATE_CCW));
        Button configButton = new Button("Config", new FontIcon(Feather.TOOL));
        Button menuButton = new Button("Menu", new FontIcon(Feather.MENU));

        refreshButton.setOnAction(e -> refreshDirectory(directoryTree.getSelectionModel().getSelectedItem()));
        runButton.setOnAction(e-> runProgram());
        compileButton.setOnAction(e -> compileProject());
        fileComboBox = new ComboBox<>();
        fileComboBox.setPrefWidth(200);
        setCurrentFile(null);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ToolBar toolBar = new ToolBar(
                newButton,projectButton,new Separator(Orientation.VERTICAL),
                configButton,refreshButton,spacer,
                fileComboBox,menuButton,compileButton,runButton
        );
        fileContentArea = new TextArea();
        fileContentArea.setEditable(false);

        createTree();
        SplitPane runtimePane = new SplitPane();
        runtimePane.setOrientation(Orientation.VERTICAL);
        runtimePane.setDividerPositions(0.8);
        SplitPane projectPane = new SplitPane(directoryTree,fileContentArea);
        projectPane.setOrientation(Orientation.HORIZONTAL);
        projectPane.setDividerPositions(0.2);
        runtimeText = new TextArea(("Java Runtime Version: " + System.getProperty("java.runtime.version") + "\n"));
        runtimePane.getItems().addAll(projectPane, runtimeText);

        BorderPane borderPane = new BorderPane();
//        borderPane.setTop(hbox);
        borderPane.setTop(toolBar);
        borderPane.setCenter(runtimePane);
        Scene scene = new Scene(borderPane, 1000, 800);


        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("Project: " + project.fileName() + " [" + project.filePath() + "]");
        stage.setOnShown(windowEvent -> ownerStage.close());
        stage.show();

    }

    private void openFile(File file) {
        try {
            String content = null;
            if (file.getName().endsWith(".class")) {
                content = RuntimeController.deCompile(file.getAbsolutePath());
            } else {
                content = Files.readString(file.toPath());
            }
            fileContentArea.setText(content);
            fileContentArea.setEditable(true); // 根据需求调整是否允许编辑
        } catch (Exception e) {
            fileContentArea.setText(e.getClass() + "Error reading file: " + e.getMessage());
            System.out.println(e.getMessage());
        }
    }

    private void createTree() {
        TreeItem<File> rootItem = scan(new File(project.filePath()), 3);
        rootItem.setExpanded(true);

        directoryTree = new TreeView<>(rootItem);
        directoryTree.setShowRoot(true);


        directoryTree.setCellFactory(tv -> {
            TreeCell<File> cell = new TreeCell<>() {
                @Override
                protected void updateItem(File item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        String fileName = item.getName();
                        setText(fileName);
                        // 设置图标示例，实际应用中需要调整图标资源
                        if (item.isDirectory()) {
                            setGraphic(new FontIcon(Feather.FOLDER));
                        } else {
                            if (fileName.endsWith(".class") || fileName.endsWith(".java")) {
                                setGraphic(new FontIcon(Feather.ACTIVITY));
                            } else {
                                setGraphic(new FontIcon(Feather.FILE));
                            }
                        }
                    }
                }
            };
            ContextMenu contextMenu = ContextMenuBuilder.create()
                    .addMenuItem("New", this::createItem)
                    .addMenuItem("Copy", this::copyFile)
                    .addMenuItem("Paste", this::pasteFile)
                    .addMenuItem("Delete", this::deleteFile)
                    .addMenuItem("Refresh",(() ->refreshDirectory(directoryTree.getSelectionModel().getSelectedItem())))
                    .addMenuItem("Show in Explorer", this::showinExplorer)
                    .build();

            cell.contextMenuProperty().bind(
                    Bindings.when(cell.emptyProperty()).then((ContextMenu)null).otherwise(contextMenu)
            );

//            cell.setContextMenu(contextMenu);
            return cell;
        });

        directoryTree.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                fileContentArea.setEditable(false);
                TreeItem<File> selectedItem = directoryTree.getSelectionModel().getSelectedItem();
                File selectFile = selectedItem.getValue();
                if (selectFile.isDirectory()) {
                    refreshDirectory(selectedItem);
                    fileContentArea.setText("You are browsing FilePath: " + selectFile.getAbsolutePath());
                } else {
                    openFile(selectFile);
                    setCurrentFile(selectFile);
                }

            }
        });

        directoryTree.addEventHandler(TreeItem.branchExpandedEvent(), event -> {
            TreeItem item = event.getTreeItem();
            if (item.getChildren().size() == 1 && item.getChildren().isEmpty()) {
                item.getChildren().clear();
                scan((File)item.getValue(),1);
            }
        });
    }
    private TreeItem<File> scan(File dir, int depth) {
        var parent = new TreeItem<>(dir);
        File[] files = dir.listFiles();
        depth--;

        if (files != null && depth >= 0) {
            for (File f : files) {
                TreeItem<File> childItem = f.isDirectory() ? scan(f, depth) : new TreeItem<>(f);
                parent.getChildren().add(childItem);
            }
            parent.getChildren().sort(Comparator.
                    comparing((TreeItem<File> ti) -> !ti.getValue().isDirectory())
                    .thenComparing(ti -> ti.getValue().getName()));

        }

        return parent;
    }

    private void refreshDirectory(TreeItem<File> directoryItem) {
        if (directoryItem == null) {
            directoryItem = directoryTree.getSelectionModel().getSelectedItem();
            if (directoryItem == null || directoryItem.getParent() == null) {
                directoryItem = directoryTree.getRoot();
            } else {
                directoryItem = directoryItem.getParent();
            }
        }

        File directory = directoryItem.getValue();
        if (directory.isDirectory()) {
            directoryItem.getChildren().clear();
            TreeItem<File> newScannedItem  = scan(directory, 2);
            directoryItem.getChildren().addAll(newScannedItem.getChildren());
        }
    }

    private void compileProject() {
        try {
            if (RuntimeController.buildProject(project.filePath())) {
                refreshDirectory(directoryTree.getRoot());
            }
        } catch (Exception e) {
            DialogView.alertException("Fail to Compile Project", e);
        }

    }
    private void deleteFile(){
        TreeItem<File> selectedItem = directoryTree.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            File deletePath = selectedItem.getValue();
            boolean comfirmDelete = DialogView.comfirmDelete(deletePath.getAbsolutePath());
            if (comfirmDelete) {
                if (deletePath.exists() && ProjectController.deleteFiles(deletePath)) {
                    TreeItem<File> parentItem = selectedItem.getParent();
                    if (parentItem != null) {
                        refreshDirectory(parentItem);
                    } else {
                        refreshDirectory(directoryTree.getRoot());
                    }
                } else {
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR, "File could not be deleted.", ButtonType.OK);
                    errorAlert.showAndWait();
                }
            }

        }
    }
    private void pasteFile(){
        TreeItem<File> selectedItem = directoryTree.getSelectionModel().getSelectedItem();
        try {
            ProjectController.pasteFiles(selectedItem.getValue());
        } catch (IOException e) {
            DialogView.alertException("Error",e);
        } finally {
            refreshDirectory(selectedItem);
        }
    }

    private void setCurrentFile(File file) {
        String currentFileName = ProjectController.loadCurrentFile(project);
        if (file == null || !file.getName().endsWith(".java")) {
            fileComboBox.setValue(currentFileName);
        } else {
            fileComboBox.setValue(file.getName());
            ProjectController.setCurrentFile(file);
        }
    }
    private void copyFile(){
        TreeItem<File> selectedItem = directoryTree.getSelectionModel().getSelectedItem();
        ProjectController.copyFile(selectedItem.getValue());
    }
    private void createItem(){}

    private void showinExplorer() {
        TreeItem<File> selectedItem = directoryTree.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.getValue() != null) {
            try {
                ProjectController.showinExplorer(selectedItem.getValue());
            } catch (IOException e) {
                DialogView.alertException("Error",e);
            }
        }
    }
    private void runProgram() {
        compileProject();
        new Thread(() -> {
            ExecutionResult res = RuntimeController.executeCode(directoryTree.getSelectionModel().getSelectedItem().getValue().getAbsolutePath(), "java", "Main", runtimeText::appendText);
            if (res.exitCode() != 0) {
                runtimeText.appendText(res.output());
            }
            runtimeText.appendText("\nProcess finished with exit code "+ res.exitCode() + "\n");

        }).start();
    }
}
