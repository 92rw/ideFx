package com.rwj.idefx.view;

import com.rwj.idefx.JavaSyntaxHighlighter;
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
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.util.UndoUtils;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static javafx.scene.input.KeyCombination.CONTROL_DOWN;

public class MainView {
    private final BooleanProperty fileModified = new SimpleBooleanProperty(false);
    private final StringProperty currentFileType = new SimpleStringProperty("");

    private final FileModel project;
    private TextArea consoleArea;
    private CodeArea codeArea;

    private TreeView<File> directoryTree;

    private ComboBox<String> fileComboBox;
    private Button runButton;
    private String projectTheme;
    public MainView(FileModel project, String theme) {
        this.project = project;
        this.projectTheme = theme;
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
        projectButton.setOnAction(e -> showinExplorer(directoryTree.getRoot()));
        runButton.setOnAction(e-> runProgram());
        compileButton.setOnAction(e -> compileProject());
        refreshButton.setOnAction(e -> refreshDirectory());
        configButton.setOnAction(e -> projectConfig(ownerStage));
        menuButton.setOnAction(e -> {
            ownerStage.close();
            StartView.showStartView(ownerStage);
        });
        saveButton.setOnAction(e -> saveFile());
        fileComboBox = new ComboBox<>();
        fileComboBox.setPrefWidth(200);


        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ToolBar toolBar = new ToolBar(
                newButton,saveButton, projectButton,new Separator(Orientation.VERTICAL),
                configButton,refreshButton,spacer,
                fileComboBox,menuButton,compileButton,runButton
        );

        initCodeArea();


        createTree();
        openFile();
        SplitPane runtimePane = new SplitPane();
        runtimePane.setOrientation(Orientation.VERTICAL);
        runtimePane.setDividerPositions(0.7);
        SplitPane projectPane = new SplitPane(directoryTree, codeArea);
        projectPane.setOrientation(Orientation.HORIZONTAL);
        projectPane.setDividerPositions(0.2);
        consoleArea = new TextArea();
        consoleArea.appendText(RuntimeController.getJVMInfo() + "\n");
        consoleArea.setEditable(false);
        TextField inputField = new TextField();
        inputField.setPromptText("Enter command or input...");
        inputField.setOnAction(event -> {
            String inputText = inputField.getText();
            RuntimeController.redirectToProcess(inputText);
            consoleArea.appendText(inputText + "\n");
            inputField.clear();
        });
        BorderPane consolePane = new BorderPane();
        consolePane.setCenter(consoleArea);
        consolePane.setBottom(inputField);

        runtimePane.getItems().addAll(projectPane, consolePane);

        BorderPane borderPane = new BorderPane();
        borderPane.setTop(toolBar);
        borderPane.setCenter(runtimePane);

        Stage stage = getStage(ownerStage, borderPane);
        stage.show();

    }

    private void initCodeArea() {
        codeArea = new CodeArea();

        IntFunction<Node> noFactory = LineNumberFactory.get(codeArea);
        IntFunction<Node> graphicFactory = line -> {
            HBox lineBox = new HBox(noFactory.apply(line));
            lineBox.getStyleClass().add("lineno-box");
            lineBox.setAlignment(Pos.CENTER_LEFT);
            return lineBox;
        };

        var um = UndoUtils.plainTextUndoManager(codeArea);
        codeArea.setUndoManager(um);
        new JavaSyntaxHighlighter().start(codeArea);

        // auto-indent
        codeArea.addEventHandler( KeyEvent.KEY_PRESSED, KE -> {
            if ( KE.getCode() == KeyCode.ENTER && !KE.isControlDown()) indent(codeArea);
        });

        // auto-close brackets
        codeArea.setOnKeyTyped(keyEvent -> {
            String str = keyEvent.getCharacter().equals("{") ? "}" :
                    keyEvent.getCharacter().equals("[") ? "]" :
                            keyEvent.getCharacter().equals("(") ? ")" :
                                    keyEvent.getCharacter().equals("\"") ? "\"" :
                                            keyEvent.getCharacter().equals("'") ? "'" : null;
            if (str==null) return;
            codeArea.insertText(codeArea.getCaretPosition(), str);
            codeArea.getCaretSelectionBind().moveTo(codeArea.getCaretPosition()-1);
        });

        codeArea.setEditable(false);
        codeArea.textProperty().addListener((observable, oldValue, newValue) -> {
            fileModified.set(true);
        });
        codeArea.setParagraphGraphicFactory(graphicFactory);
        setCodeAreaTheme();
        codeArea.textProperty().addListener(textChangeListener);
        codeArea.requestFocus();

        ContextMenu contextMenu = ContextMenuBuilder.create().
                addMenuItem("_Undo", Feather.CORNER_DOWN_LEFT,
                        new KeyCodeCombination(KeyCode.Z, CONTROL_DOWN), true, () -> codeArea.undo()).
                addMenuItem("_Redo", Feather.CORNER_DOWN_RIGHT,
                        new KeyCodeCombination(KeyCode.Y, CONTROL_DOWN), true, () -> codeArea.redo()).
                addSeperate().
                addMenuItem("Cut", Feather.SCISSORS, new KeyCodeCombination(KeyCode.X, CONTROL_DOWN), false, () -> codeArea.cut()).
                addMenuItem("Copy", Feather.COPY, new KeyCodeCombination(KeyCode.C, CONTROL_DOWN), false, () -> codeArea.copy()).
                addMenuItem("Paste", Feather.CLIPBOARD, new KeyCodeCombination(KeyCode.V, CONTROL_DOWN), false, () -> codeArea.paste()).
                addSeperate().
                addMenuItem("Select All", null, null, null, () -> codeArea.selectAll()).
                build();
        codeArea.setContextMenu(contextMenu);
    }

    public void indent(CodeArea area) {
        final Pattern whiteSpace = Pattern.compile( "^\\s+" );
        int caretPosition = area.getCaretPosition();
        int currentParagraph = area.getCurrentParagraph();
        Matcher m0 = whiteSpace.matcher( area.getParagraph( currentParagraph-1 ).getSegments().get( 0 ) );
        if ( m0.find() ) area.insertText( caretPosition, m0.group());
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

        codeArea.setEditable(false);
        codeArea.textProperty().removeListener(textChangeListener);

        FileModel fileModel = ProjectController.loadCurrentFile(project);
        fileComboBox.setValue(fileModel.fileName());
        TreeItem<File> selectedItem = directoryTree.getSelectionModel().getSelectedItem();

        File selectFile = selectedItem != null ? selectedItem.getValue() : new File(fileModel.filePath());

        if (selectFile.isDirectory()) {
            refreshDirectory(selectFile);
            codeArea.replaceText("You are browsing FilePath: " + selectFile.getAbsolutePath());
        } else {
            selectTreeItem(directoryTree, selectFile);
            String fileName = selectFile.getName();
            currentFileType.set(fileName.substring(fileName.lastIndexOf('.') + 1));
            if (fileName.endsWith(".class")) {
                codeArea.replaceText(RuntimeController.deCompile(selectFile.getAbsolutePath()));
            } else {
                if (fileName.endsWith(".java")) {
                    fileComboBox.setValue(fileName);
                    ProjectController.setCurrentFile(selectFile);
                }
                codeArea.setEditable(true);
                try {
                    currentPath = selectFile.toPath();
                    String content = Files.readString(currentPath);
                    originalContent = content;
                    codeArea.replaceText(content);
                    fileModified.set(false);
                } catch (IOException e) {
                    codeArea.replaceText("Error reading file: " + selectFile.getAbsolutePath() +"\n\n" + e);
                    currentPath = null;
                    codeArea.setEditable(false);
                }
            }
        }
        codeArea.textProperty().addListener(textChangeListener);
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
            ProjectController.saveContext(codeArea.getText(), currentPath);
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

    private void setCodeAreaTheme() {
        if (projectTheme.equals("Dracula") || projectTheme.equals("Nord Dark")) {
            codeArea.getStylesheets().add(String.valueOf(getClass().getResource("/themes/codearea/dracula.css")));
        } else if (projectTheme.endsWith("Dark")) {
            codeArea.getStylesheets().add(String.valueOf(getClass().getResource("/themes/codearea/monokai.css")));
        } else {
            codeArea.getStylesheets().add(String.valueOf(getClass().getResource("/themes/codearea/light.css")));
        }
    }

    public void projectConfig(Stage currentStage) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Change JVM Version");
        ButtonType confirmButtonType = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        Button openFileChooserBtn = new Button("Choose File");
        TextField filePathField = new TextField();
        filePathField.setEditable(false);

        openFileChooserBtn.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Java Runtime Environment");

            if (RuntimeController.isWindowsOS()) {
                fileChooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("Executable Files", "*.exe")
                );
            } // 在Linux或macOS上，通常不需要设置扩展名过滤器

            File selectedFile = fileChooser.showOpenDialog(currentStage);
            if (selectedFile != null) {
                File javaExe = RuntimeController.getJavaExecutable(selectedFile);
                if (javaExe != null) {
                    String jreHome = Paths.get(javaExe.getParentFile().getAbsolutePath(), "java").toString();
                    filePathField.setText(jreHome);
                } else {
                    // 显示错误信息,选择的文件不是有效的 Java 可执行文件
                    DialogView.operationResult("Please choose an available Java File");
                }
            }
        });

        VBox dialogContent = new VBox(10, filePathField, openFileChooserBtn);
        dialog.getDialogPane().setContent(dialogContent);


        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                return filePathField.getText();
            }
            return null;
        });

        // Show the dialog and capture the result
        dialog.showAndWait().ifPresent(result -> {
            // Update the label with the selected file path
            RuntimeController.setJavaPath(result);
            consoleArea.appendText(RuntimeController.getJVMInfo() + "\n");
        });

    }

}