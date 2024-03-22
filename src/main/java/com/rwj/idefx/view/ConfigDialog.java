package com.rwj.idefx.view;

import com.rwj.idefx.controller.RuntimeController;
import com.rwj.idefx.model.OSType;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;


import java.io.File;
import java.nio.file.Paths;

import static com.rwj.idefx.controller.RuntimeController.getOperatingSystem;

public class ConfigDialog extends Application {
    //TODO If JVM can run smoothly, the "selectedFilePathLabel" should show correct result
    //TODO If I fix this bug, this function can add to MainView's "Config" Button
    private OSType os = getOperatingSystem();
    private TextArea selectedFilePathLabel; // Label to display the selected file path

    @Override
    public void start(Stage primaryStage) {
        Button showDialogBtn = new Button("Open Dialog");
        showDialogBtn.setOnAction(event -> showCustomDialog(primaryStage));
        selectedFilePathLabel = new TextArea("No file selected"); // Initialize the label

        VBox root = new VBox(10, showDialogBtn, selectedFilePathLabel);

        Scene scene = new Scene(root, 300, 200);

        primaryStage.setTitle("Custom Dialog Test");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void showCustomDialog(Stage primaryStage) {
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

            if (os == OSType.WINDOWS) {
                fileChooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("Executable Files", "*.exe")
                );
            } // 在Linux或macOS上，通常不需要设置扩展名过滤器

            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                File javaExe = getJavaExecutable(selectedFile);
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
            selectedFilePathLabel.setText(RuntimeController.getJVMInfo());
        });


    }

    private File getJavaExecutable(File file) {
        if (os == OSType.WINDOWS) {
            // 在 Windows 上,我们需要检查文件是否为 java.exe
            if (file.getName().equals("java.exe")) {
                return file;
            }
        } else {
            // 在 Unix 系统上,我们只需要检查文件是否可执行
            if (file.canExecute()) {
                return file;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
