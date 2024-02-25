package com.rwj.idefx.view;

import com.rwj.idefx.controller.ProjectController;
import com.rwj.idefx.controller.RuntimeController;
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
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;


public class MainView {
    private FileModel project;
    private TextArea fileContentArea;

    private TreeView<File> directoryTree;
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

        refreshButton.setOnAction(e -> refreshDirectory());

        ComboBox<File> fileComboBox = new ComboBox<>();

        // 创建弹性空间
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        ToolBar toolBarLeft = new ToolBar(
                newButton,
                projectButton,

                new Separator(Orientation.VERTICAL),
                configButton,
                refreshButton
        );

        ToolBar toolBarRight = new ToolBar(
                fileComboBox,
                menuButton,
                compileButton,
                runButton
        );

        // 将ToolBar和弹性空间添加到HBox中
        HBox hbox = new HBox(toolBarLeft, spacer, toolBarRight);



        fileContentArea = new TextArea();
        fileContentArea.setEditable(false);



        createTree();
        SplitPane runtimePane = new SplitPane();
        runtimePane.setOrientation(Orientation.VERTICAL);
        runtimePane.setDividerPositions(0.8);
        SplitPane projectPane = new SplitPane(directoryTree,fileContentArea);
        projectPane.setOrientation(Orientation.HORIZONTAL);
        projectPane.setDividerPositions(0.2);
        TextFlow runtimeText = new TextFlow(new Text("Java Runtime Version: " + System.getProperty("java.runtime.version")));
        runtimePane.getItems().addAll(projectPane, new ScrollPane(runtimeText));




        BorderPane borderPane = new BorderPane();
        borderPane.setTop(hbox);
        borderPane.setCenter(runtimePane);
        Scene scene = new Scene(borderPane, 1000, 800);


        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setTitle(project.fileName() + "[" + project.filePath() + "]");
        stage.setOnShown(windowEvent -> ownerStage.close());
        stage.show();

    }

    private void openFile(File file) {
        String fileNameWithExtension = file.getName();
        System.out.println(fileNameWithExtension);
        System.out.println(file.getParent());
        String substring = fileNameWithExtension.substring(0, fileNameWithExtension.lastIndexOf('.'));
        System.out.println(substring);
        System.out.println(file.getAbsolutePath());
        try {
            String content = Files.readString(file.toPath());
            if (file.getName().endsWith(".class")) {
                content = RuntimeController.Decompile(file.getParent(), fileNameWithExtension.substring(0, fileNameWithExtension.lastIndexOf('.')));
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
                    .addMenuItem("Refresh", this::refreshDirectory)
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
                File selectFile = directoryTree.getSelectionModel().getSelectedItem().getValue();
                if (selectFile.isDirectory()) {
                    scan(selectFile,1);
                    fileContentArea.setText("You are browsing FilePath: " + selectFile.getAbsolutePath());
                } else {
                    openFile(selectFile);
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
        var parent = new TreeItem<>(new File(dir.getAbsolutePath()));
        File[] files = dir.listFiles();
        depth--;

        if (files != null) {
            for (File f : files) {
                if (f.isDirectory() && depth > 0) {
                    parent.getChildren().add(scan(f, depth));
                } else {
                    var leaf = new TreeItem<>(new File(f.getAbsolutePath()));
                    parent.getChildren().add(leaf);
                }
            }
            parent.getChildren().sort(Comparator.comparing((TreeItem<File> ti) -> !ti.getValue().isDirectory())
                    .thenComparing(ti -> ti.getValue().getName()));

        }

        return parent;
    }

    private void refreshDirectory() {
        TreeItem<File> selectedItem = directoryTree.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            selectedItem.getChildren().clear();
            File directory = selectedItem.getValue();
            if (directory.isDirectory()) {
                scan(directory, 1);
            }
        }
    }

    private void deleteFile(){
        //TODO 修改代码逻辑
        TreeItem<File> selectedItem = directoryTree.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            try {
                if (ProjectController.deleteFile(selectedItem.getValue())) {
                    refreshDirectory();
                }
            } catch (RuntimeException e) {
                DialogView.alertException("Delete failed", e);
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
            refreshDirectory();
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
    private void runProgram(File file) {

    }
}
