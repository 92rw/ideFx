package com.rwj.idefx.view;

import com.rwj.idefx.controller.ProjectController;
import com.rwj.idefx.controller.RuntimeController;
import com.rwj.idefx.model.ExecutionResult;
import com.rwj.idefx.model.FileModel;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class MainView {
    private final BooleanProperty fileModified = new SimpleBooleanProperty(false);
    private final StringProperty currentFileType = new SimpleStringProperty("");

    private final FileModel project;
    private TextArea contentArea, consoleArea;

    private TreeView<File> directoryTree;

    private ComboBox<String> fileComboBox;
    private Button runButton;
    public MainView(FileModel project) {
        this.project = project;
    }

    private boolean codeRunning;
    private Path currentPath;
    private String originalContent = ""; // 用于存储原始内容的变量
    // 在文本更改监听器中比较当前内容和原始内容
    private final ChangeListener<String> textChangeListener = (observable, oldValue, newValue) -> {
        if (!newValue.equals(originalContent)) {
            fileModified.set(true);
        } else {
            fileModified.set(false);
        }
    };

    public void display(Stage ownerStage) {

        Button newButton = new Button("New", new FontIcon(Feather.PLUS));
        Button projectButton = new Button("Project", new FontIcon(Feather.FOLDER));

        runButton = new Button("Run", new FontIcon(Feather.PLAY));
        Button compileButton = new Button("Compile", new FontIcon(Feather.LAYERS));
        Button refreshButton = new Button("Refresh", new FontIcon(Feather.ROTATE_CCW));
        Button configButton = new Button("Config", new FontIcon(Feather.TOOL));
        Button menuButton = new Button("Menu", new FontIcon(Feather.MENU));
        Button saveButton = new Button("Save", new FontIcon(Feather.SAVE));

        newButton.setOnAction(e-> createItem());
        saveButton.setOnAction(e -> saveFile());
        projectButton.setOnAction(e -> showinExplorer(directoryTree.getRoot()));
        refreshButton.setOnAction(e -> refreshDirectory());
        runButton.setOnAction(e-> runProgram());
        compileButton.setOnAction(e -> compileProject());
        fileComboBox = new ComboBox<>();
        fileComboBox.setPrefWidth(200);


        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ToolBar toolBar = new ToolBar(
                newButton,saveButton, projectButton,new Separator(Orientation.VERTICAL),
                configButton,refreshButton,spacer,
                fileComboBox,menuButton,compileButton,runButton
        );
        contentArea = new TextArea();
        contentArea.setEditable(false);
        contentArea.textProperty().addListener((observable, oldValue, newValue) -> {
            fileModified.set(true);
        });

        contentArea.textProperty().addListener(textChangeListener);




        createTree();
        openFile();
        SplitPane runtimePane = new SplitPane();
        runtimePane.setOrientation(Orientation.VERTICAL);
        runtimePane.setDividerPositions(0.7);
        SplitPane projectPane = new SplitPane(directoryTree,contentArea);
        projectPane.setOrientation(Orientation.HORIZONTAL);
        projectPane.setDividerPositions(0.2);
        consoleArea = new TextArea(RuntimeController.getJVMInfo() + "\n");
        consoleArea.setEditable(false);
        TextField inputField = new TextField();
        inputField.setOnAction(event -> {
            String inputText = inputField.getText();
            RuntimeController.redirectToProcess(inputText);
            consoleArea.appendText(inputText + "\n");
            inputField.clear();
        });

        runtimePane.getItems().addAll(projectPane, new VBox(consoleArea, inputField));

        BorderPane borderPane = new BorderPane();
        borderPane.setTop(toolBar);
        borderPane.setCenter(runtimePane);

        Stage stage = getStage(ownerStage, borderPane);
        stage.show();

    }

    private Stage getStage(Stage ownerStage, BorderPane borderPane) {
        Scene scene = new Scene(borderPane, 1000, 800);

        Stage stage = new Stage();
        stage.setScene(scene);

        stage.setTitle("Project: " + project.fileName() + " [" + project.filePath() + "]");
        stage.setOnShown(windowEvent -> ownerStage.close());
        stage.setOnCloseRequest(event -> {
            if (fileModified.get()) {
                // 提示用户保存更改或自动保存
                saveFile(); // 根据需要调整逻辑
            }
            try {
                ProjectController.saveCustomizeModel(null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return stage;
    }

    private void openFile() {
        if (fileModified.get()) {
            comfirmAndSave(); // 如果文件被修改，提示用户保存
        }

        contentArea.setEditable(false);
        contentArea.textProperty().removeListener(textChangeListener);

        FileModel fileModel = ProjectController.loadCurrentFile(project);
        fileComboBox.setValue(fileModel.fileName());
        TreeItem<File> selectedItem = directoryTree.getSelectionModel().getSelectedItem();

        File selectFile = selectedItem != null ? selectedItem.getValue() : new File(fileModel.filePath());

        if (selectFile.isDirectory()) {
            refreshDirectory(selectFile);
            contentArea.setText("You are browsing FilePath: " + selectFile.getAbsolutePath());
        } else {
            selectTreeItem(directoryTree, selectFile);
            String fileName = selectFile.getName();
            currentFileType.set(fileName.substring(fileName.lastIndexOf('.') + 1));
            if (fileName.endsWith(".class")) {
                contentArea.setText(RuntimeController.deCompile(selectFile.getAbsolutePath()));
            } else {
                if (fileName.endsWith(".java")) {
                    fileComboBox.setValue(fileName);
                    ProjectController.setCurrentFile(selectFile);
                }
                contentArea.setEditable(true);
                try {
                    currentPath = selectFile.toPath();
                    String content = Files.readString(currentPath);
                    originalContent = content;
                    contentArea.setText(content);
                    fileModified.set(false);
                } catch (IOException e) {
                    contentArea.setText("Error reading file: " + selectFile.getAbsolutePath() +"\n\n" + e);
                    currentPath = null;
                    contentArea.setEditable(false);
                }
            }
        }
        contentArea.textProperty().addListener(textChangeListener);
        fileModified.set(false); // 重新添加监听器后重置文件修改状态

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
                    .addMenuItem("open", this::openFile)
                    .addSeperate()
                    .addMenuItem("Copy", this::copyFile)
                    .addMenuItem("Paste", this::pasteFile)
                    .addMenuItem("Delete", this::deleteFile)
                    .addSeperate()
                    .addMenuItem("Refresh",this::refreshDirectory)
                    .addMenuItem("Show in Explorer", this::showinExplorer)
                    .build();

            cell.contextMenuProperty().bind(
                    Bindings.when(cell.emptyProperty()).then((ContextMenu)null).otherwise(contextMenu)
            );

            return cell;
        });

        directoryTree.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                comfirmAndSave();
                openFile();
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

    private void comfirmAndSave() {
        if (fileModified.get()) {
            if ("java".equals(currentFileType.get())) {
                saveFile(); // 直接保存 .java 文件
            } else {
                // 对于非 .java 文件，可能需要用户确认是否保存
                boolean confirmSave = DialogView.comfirmOperation("Do you want to save the modified file?");
                if (confirmSave) {
                    saveFile(); // 用户确认后保存
                }
            }
        }
    }
    private void selectTreeItem(TreeView<File> treeView, File file) {
        TreeItem<File> root = directoryTree.getRoot();
        TreeItem<File> foundItem = findTreeItem(root, file);

        if (foundItem != null) {
            treeView.getSelectionModel().select(foundItem);
        }
    }

    private TreeItem<File> findTreeItem(TreeItem<File> root, File file) {
        if (root == null || file == null) {
            return null;
        }

        if (root.getValue().getAbsolutePath().equals(file.getAbsolutePath())) {
            return root;
        }

        for (TreeItem<File> child : root.getChildren()) {
            TreeItem<File> found = findTreeItem(child, file);
            if (found != null) {
                return found;
            }
        }

        return null;
    }
    private void refreshDirectory() {
        refreshDirectory(directoryTree.getSelectionModel().getSelectedItem().getValue());
        DialogView.operationResult("Refresh successfully");
    }

    private void refreshDirectory(File directory) {
        if (directory == null) {
            directory = directoryTree.getRoot().getValue();
        }

        TreeItem<File> refreshItem = findTreeItem(directoryTree.getRoot(), directory);
        if (!directory.isDirectory()) refreshItem = refreshItem.getParent();

        refreshItem.getChildren().clear();
        TreeItem<File> newScannedItem  = scan(directory, 2);
        refreshItem.getChildren().addAll(newScannedItem.getChildren());
        selectTreeItem(directoryTree, directory);
    }

    private boolean compileProject() {
        try {
            saveFile();
            consoleArea.appendText("Start to compile project files...");
            if (RuntimeController.buildProject(project.filePath())) {
                refreshDirectory(null);
                consoleArea.appendText("Compile success\n");
                return true;
            }
        } catch (Exception e) {
            DialogView.alertException("Fail to Compile Project", e);
        }
        return false;
    }
    private void deleteFile(){
        TreeItem<File> selectedItem = directoryTree.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            File deletePath = selectedItem.getValue();
            boolean comfirmDelete = DialogView.comfirmOperation("Do you want to delete file: " + deletePath.getAbsolutePath() + " ?");
            if (comfirmDelete) {
                if (deletePath.exists() && ProjectController.deleteFiles(deletePath)) {
                    TreeItem<File> parentItem = selectedItem.getParent();
                    if (parentItem != null) {
                        refreshDirectory(parentItem.getValue());
                    } else {
                        refreshDirectory(null);
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
            refreshDirectory(selectedItem.getValue());
        }
    }

    private void copyFile(){
        TreeItem<File> selectedItem = directoryTree.getSelectionModel().getSelectedItem();
        ProjectController.copyFile(selectedItem.getValue());
    }
    private void createItem() {
        Stage stage = new Stage();
        TabPane tabPane = new TabPane();

        Tab newFileTab = new Tab("New File");
        newFileTab.setClosable(false);

        TextField fileNameField = new TextField();
        fileNameField.setPromptText("Enter file name");

        String projectPath = ProjectController.getProjectPath();
        VBox vbox = createNewFileLayout(projectPath, stage, fileNameField);
        vbox.setPadding(new Insets(10));
        vbox.setAlignment(Pos.CENTER);

        newFileTab.setContent(vbox);
        tabPane.getTabs().add(newFileTab);

        Scene scene = new Scene(tabPane, 400, 300);
        stage.setScene(scene);
        stage.setTitle("Create New File");
        stage.show();

    }

    private VBox createNewFileLayout(String projectPath, Stage stage, TextField fileNameField) {
        Label directoryLabel = new Label(projectPath);

        Button chooseDirectoryButton = new Button("Choose Directory");
        chooseDirectoryButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select Folder");
            directoryChooser.setInitialDirectory(new File(projectPath));
            File selectedDirectory = directoryChooser.showDialog(stage);

            if (selectedDirectory != null) {
                directoryLabel.setText(selectedDirectory.getAbsolutePath());
            }
        });

        Button createButton = new Button("Create");
        createButton.setOnAction(event -> createFile(directoryLabel.getText(),fileNameField.getText(), stage));

        VBox vbox = new VBox(10, directoryLabel, chooseDirectoryButton, fileNameField, createButton);
        return vbox;
    }

    private void createFile(String path, String fileName, Stage stage) {
        // 假设所有文件都创建在项目根目录下
        File rootDir = new File(path);
        File newFile = new File(rootDir, fileName);

        try {
            boolean created = newFile.createNewFile();
            if (created) {
                Platform.runLater(() -> {
                    refreshDirectory(rootDir); // 刷新目录以显示新文件
                    selectTreeItem(directoryTree, newFile); // 选择新创建的文件
                });
                stage.close(); // 关闭创建文件的窗口
            } else {
                // 文件创建失败的处理
                Alert alert = new Alert(Alert.AlertType.ERROR, "File creation failed.");
                alert.showAndWait();
            }
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "An error occurred: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void showinExplorer() {
        TreeItem<File> currentItem = directoryTree.getSelectionModel().getSelectedItem();
        showinExplorer(currentItem);
    }

    private void showinExplorer(TreeItem<File> selectedItem) {
        if (selectedItem != null && selectedItem.getValue() != null) {
            try {
                ProjectController.showinExplorer(selectedItem.getValue());
            } catch (IOException e) {
                DialogView.alertException("Error",e);
            }
        }
    }

    private void saveFile() {
        try {
            ProjectController.saveContext(contentArea.getText(), currentPath);
            fileModified.set(false);
        } catch (IOException e) {
            DialogView.alertException("Error saving file" + currentPath.toString(),e);
        }
    }
    private void runProgram() {
        if (!codeRunning) {
            if (compileProject()) {
                new Thread(() -> {
                    Platform.runLater(() -> {
                        runButton.setText("Stop");
                        runButton.setGraphic(new FontIcon(Feather.SQUARE));
                    });
                    codeRunning = true;
                    FileModel fileModel = ProjectController.loadCurrentFile(project);
                    ExecutionResult res = RuntimeController.executeCode(project.filePath(), fileModel, consoleArea::appendText);
                    if (res.exitCode() != 0) {
                        consoleArea.appendText(res.output());
                    }
                    Platform.runLater(() -> {
                        consoleArea.appendText("\nProcess finished with exit code " + res.exitCode() + "\n");
                        runButton.setText("Run");
                        runButton.setGraphic(new FontIcon(Feather.PLAY));
                        codeRunning = false;
                    });
                }).start();
            } else {
                openFile();
                DialogView.operationResult("Fail to compile your project");
            }
        } else {
            RuntimeController.stopProcess();
            codeRunning = false;
        }
    }

}
