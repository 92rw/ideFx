package com.rwj.idefx.view;

import atlantafx.base.theme.Styles;
import com.rwj.idefx.model.FileModel;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

public class ProjectListCell extends ListCell<FileModel> {
    private final Rectangle icon;
    private final Text initial, nameText,pathText;
    private final StackPane iconContainer;
    private final VBox textContainer;
    private final HBox root;

    public ProjectListCell() {
        icon = new Rectangle(50, 50);
        initial = new Text();
        initial.getStyleClass().addAll(Styles.TITLE_2);
        nameText = new Text();
        nameText.getStyleClass().addAll(Styles.TEXT_BOLD);
        pathText = new Text();

        iconContainer = new StackPane(icon, initial);
        textContainer = new VBox(5, nameText, pathText);
        textContainer.setAlignment(Pos.CENTER_LEFT);

        root = new HBox(15,iconContainer, textContainer);
    }
    @Override
    public void updateItem(FileModel project, boolean empty) {
        super.updateItem(project, empty);

        if (empty || project == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        String projectName = project.fileName();
        icon.setFill(Color.valueOf(hashColor(projectName.hashCode())));
        initial.setText(String.valueOf(projectName.charAt(0)));
        nameText.setText(projectName);
        pathText.setText(project.filePath());

        setText(null);
        setGraphic(root);
    }

    private String hashColor(int hashCode) {
        String[] colors = {"#FFC0CB", "#FFA500", "#00C957", "#A066D3",
                "#E3CF57", "#DDA0DD", "#33A1D2",
                "#2E8B57", "#FCE6C9", "#808A87"};
        int index = Math.abs(hashCode) % colors.length;
        return colors[index];
    }
}
