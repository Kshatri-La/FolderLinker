package com.filelinker;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.util.Callback;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

public class BrowserController {

    @FXML private TextField pathField;
    @FXML private TreeTableView<FileItem> fileTable;
    @FXML private TreeTableColumn<FileItem, FileItem> nameColumn;
    @FXML private TreeTableColumn<FileItem, String> sizeColumn;
    @FXML private TreeTableColumn<FileItem, String> dateColumn;

    private File currentDirectory;
    private Runnable onLinkStateChanged;

    @FXML
    public void initialize() {
        sizeColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("sizeFormatted"));
        dateColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("dateFormatted"));

        nameColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getValue()));
        nameColumn.setCellFactory(createNameCellFactory());

        fileTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        fileTable.setSortPolicy(tv -> false);
        
        // Căn chỉnh giãn cách trên dưới theo yêu cầu và tăng rõ độ thụt dòng
        fileTable.setFixedCellSize(32);
        fileTable.setStyle("-fx-indent: 20;"); 

        setupDragAndDrop();
    }

    public void setOnLinkStateChanged(Runnable onLinkStateChanged) {
        this.onLinkStateChanged = onLinkStateChanged;
    }

    @FXML
    private void handleImport() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Folder");
        File dir = chooser.showDialog(pathField.getScene().getWindow());
        if (dir != null) {
            loadDirectory(dir);
        }
    }

    private void loadDirectory(File dir) {
        this.currentDirectory = dir;
        pathField.setText(dir.getAbsolutePath());
        
        TreeItem<FileItem> rootItem = createNode(new FileItem(dir));
        rootItem.setExpanded(true); 
        
        fileTable.setRoot(rootItem);
        fileTable.setShowRoot(false);
        refreshPaths(rootItem);
    }

    public void refreshList() {
        if (fileTable.getRoot() != null) {
            refreshPaths(fileTable.getRoot());
            fileTable.refresh();
        }
    }

    @FXML
    private void handleRefresh() {
        refreshList();
    }

    @FXML
    private void handleNewFolder() {
        if (currentDirectory == null) return;
        
        File targetDir = currentDirectory;
        ObservableList<TreeItem<FileItem>> selected = fileTable.getSelectionModel().getSelectedItems();
        if (!selected.isEmpty()) {
            FileItem item = selected.get(0).getValue();
            if (item != null && item.isDirectory() && item.getFile() != null && !item.getFile().getName().isEmpty()) {
                targetDir = item.getFile();
            }
        }
        
        TextInputDialog dialog = new TextInputDialog("New Folder");
        dialog.setTitle("Create New Folder");
        dialog.setHeaderText("Create a new folder inside:\n" + targetDir.getAbsolutePath());
        dialog.setContentText("Please enter folder name:");
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            File newFolder = new File(targetDir, result.get().trim());
            if (!newFolder.exists()) {
                if (newFolder.mkdirs()) {
                    refreshList();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Failed to create directory!").showAndWait();
                }
            } else {
                new Alert(Alert.AlertType.ERROR, "Directory already exists!").showAndWait();
            }
        }
    }

    @FXML
    private void handleDelete() {
        ObservableList<TreeItem<FileItem>> selected = fileTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) return;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete " + selected.size() + " item(s)?");
        alert.setContentText("Are you sure you want to permanently delete the selected item(s)?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            List<TreeItem<FileItem>> toDelete = new ArrayList<>(selected);
            for (TreeItem<FileItem> treeItem : toDelete) {
                if (treeItem.getValue() != null && treeItem.getValue().getFile() != null) {
                    deleteRecursively(treeItem.getValue().getFile());
                }
            }
            refreshList();
        }
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }

    @FXML
    private void handleExport() {
        ObservableList<TreeItem<FileItem>> selected = fileTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) return;
        
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Destination to Export");
        File destDir = chooser.showDialog(pathField.getScene().getWindow());
        
        if (destDir != null) {
            for (TreeItem<FileItem> treeItem : selected) {
                if (treeItem.getValue() != null && treeItem.getValue().getFile() != null && !treeItem.getValue().getFile().getName().isEmpty()) {
                    File src = treeItem.getValue().getFile();
                    File dest = new File(destDir, src.getName());
                    try {
                        copyRecursively(src, dest, true); // true = overwrite if exists
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            new Alert(Alert.AlertType.INFORMATION, "Export Completed!").showAndWait();
        }
    }

    private void refreshPaths(TreeItem<FileItem> node) {
        if (!node.isLeaf() && node.getChildren() != null) {
            ObservableList<TreeItem<FileItem>> children = node.getChildren();

            boolean hasDummy = children.size() == 1 && children.get(0).getValue() != null && children.get(0).getValue().getFile().getName().isEmpty();
            if (hasDummy) return;

            List<TreeItem<FileItem>> safeChildren = new ArrayList<>(children);
            for (TreeItem<FileItem> child : safeChildren) {
                FileItem item = child.getValue();
                if (item.isDirectory()) {
                    boolean linked = LinkManager.getInstance().isLinked(item.getName());
                    item.setLinked(linked);
                    if (linked) {
                        item.setLinkColor(LinkManager.getInstance().getColor(item.getName()));
                    }
                    if (child.isExpanded()) {
                        refreshPaths(child);
                    }
                }
            }
            sortItems(children);
        }
    }

    private TreeItem<FileItem> createNode(FileItem fileItem) {
        TreeItem<FileItem> item = new TreeItem<>(fileItem);
        
        if (fileItem.isDirectory()) {
            FileItem dummyItem = new FileItem(new File("")); // dummy
            item.getChildren().add(new TreeItem<>(dummyItem));

            item.expandedProperty().addListener((obs, wasExpanded, isNowExpanded) -> {
                if (isNowExpanded) {
                    // Lazy load
                    if (item.getChildren().size() == 1 && item.getChildren().get(0).getValue().getFile().getName().isEmpty()) {
                        item.getChildren().clear();
                        File[] files = item.getValue().getFile().listFiles();
                        if (files != null) {
                            List<TreeItem<FileItem>> newChildren = new ArrayList<>();
                            for (File f : files) {
                                FileItem subFileItem = new FileItem(f);
                                boolean linked = LinkManager.getInstance().isLinked(subFileItem.getName());
                                subFileItem.setLinked(linked);
                                if (linked) {
                                    subFileItem.setLinkColor(LinkManager.getInstance().getColor(subFileItem.getName()));
                                }
                                newChildren.add(createNode(subFileItem));
                            }
                            item.getChildren().setAll(newChildren);
                            sortItems(item.getChildren());
                        }
                    }
                }
            });
        }
        return item;
    }

    private void sortItems(ObservableList<TreeItem<FileItem>> items) {
        items.sort((o1, o2) -> {
            FileItem item1 = o1.getValue();
            FileItem item2 = o2.getValue();
            if (item1 == null || item2 == null || item1.getFile().getName().isEmpty()) return 0;
            
            if (item1.isLinked() && !item2.isLinked()) return -1;
            if (!item1.isLinked() && item2.isLinked()) return 1;
            
            if (item1.isDirectory() && !item2.isDirectory()) return -1;
            if (!item1.isDirectory() && item2.isDirectory()) return 1;
            
            return item1.getName().compareToIgnoreCase(item2.getName());
        });
    }

    private Callback<TreeTableColumn<FileItem, FileItem>, TreeTableCell<FileItem, FileItem>> createNameCellFactory() {
        return param -> new TreeTableCell<>() {
            private final HBox container = new HBox();
            private final Button caretBtn = new Button();
            private final Label iconLabel = new Label();
            private final Label nameLabel = new Label();
            private final Button linkBtn = new Button("🔗");

            {
                container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                container.setSpacing(5);
                
                linkBtn.setStyle("-fx-padding: 0 4; -fx-cursor: hand;");
                linkBtn.setOnAction(e -> {
                    FileItem item = getItem();
                    if (item != null && item.isDirectory()) {
                        LinkManager.getInstance().toggleLink(item.getName());
                        if (onLinkStateChanged != null) {
                            onLinkStateChanged.run();
                        }
                    }
                });
                
                caretBtn.setOnAction(e -> {
                    TreeTableRow<?> row = getTreeTableRow();
                    if (row != null && row.getTreeItem() != null) {
                        TreeItem<?> treeItem = row.getTreeItem();
                        treeItem.setExpanded(!treeItem.isExpanded());
                    }
                });
            }

            @Override
            protected void updateItem(FileItem item, boolean empty) {
                super.updateItem(item, empty);
                
                TreeTableRow<?> row = getTreeTableRow();
                if (row != null) {
                    // Cố tình giấu đi mũi tên mặc định của JavaFX để tránh bị đè (overlap) lên Folder
                    row.setDisclosureNode(new javafx.scene.layout.Region());
                }

                if (empty || item == null || item.getFile().getName().isEmpty()) {
                    setGraphic(null);
                    setText(null);
                    setStyle("");
                } else {
                    int level = 0;
                    if (row != null && row.getTreeItem() != null && getTreeTableView() != null) {
                        level = getTreeTableView().getTreeItemLevel(row.getTreeItem());
                    }
                    
                    // Thụt dòng (Indent) chính xác: Mỗi cấp lùi vào 20px
                    int indent = Math.max(0, (level - 1) * 20);
                    container.setPadding(new javafx.geometry.Insets(0, 0, 0, indent));
                    
                    container.getChildren().clear();
                    
                    TreeItem<?> treeItem = row != null ? row.getTreeItem() : null;
                    boolean isExpanded = treeItem != null && treeItem.isExpanded();
                    
                    if (item.isDirectory()) {
                        caretBtn.setText(isExpanded ? "▼" : "▶");
                        caretBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: black; -fx-padding: 0; -fx-cursor: hand; -fx-font-weight: 900; -fx-font-size: 14px;");
                        caretBtn.setDisable(false);
                    } else {
                        // Thêm mũi tên góc vuông (↳) cho file y hệt sơ đồ của bạn
                        caretBtn.setText("↳");
                        caretBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: gray; -fx-padding: 0; -fx-font-weight: 900; -fx-font-size: 18px;");
                        caretBtn.setDisable(true); // không cho bấm vào mũi tên của file
                    }
                    
                    container.getChildren().add(caretBtn);
                    
                    iconLabel.setText(item.isDirectory() ? "📁" : "📄");
                    container.getChildren().add(iconLabel);
                    
                    if (item.isDirectory()) {
                        container.getChildren().add(linkBtn); 
                    }

                    nameLabel.setText(item.getName());
                    container.getChildren().add(nameLabel);
                    
                    if (item.isLinked()) {
                        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + item.getLinkColor() + ";");
                        linkBtn.setStyle("-fx-background-color: " + item.getLinkColor() + "; -fx-text-fill: white; -fx-padding: 0 4; -fx-cursor: hand;");
                    } else {
                        nameLabel.setStyle("");
                        linkBtn.setStyle("-fx-padding: 0 4; -fx-cursor: hand;");
                    }

                    setGraphic(container);
                }
            }
        };
    }

    private void setupDragAndDrop() {
        fileTable.setOnDragDetected(event -> {
            ObservableList<TreeItem<FileItem>> selectedItems = fileTable.getSelectionModel().getSelectedItems();
            if (selectedItems.isEmpty()) return;

            Dragboard db = fileTable.startDragAndDrop(TransferMode.COPY_OR_MOVE);
            ClipboardContent content = new ClipboardContent();
            
            List<File> filesToDrag = new ArrayList<>();
            for (TreeItem<FileItem> treeItem : selectedItems) {
                if (treeItem.getValue() != null && !treeItem.getValue().getFile().getName().isEmpty()) {
                    filesToDrag.add(treeItem.getValue().getFile());
                }
            }
            content.putFiles(filesToDrag);
            db.setContent(content);
            event.consume();
        });

        fileTable.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles() && currentDirectory != null) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        fileTable.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            
            if (db.hasFiles() && currentDirectory != null) {
                List<File> files = db.getFiles();
                File dropTargetDir = currentDirectory;
                
                for (File file : files) {
                    if (file.getParentFile().equals(dropTargetDir)) continue;
                    
                    Path sourcePath = file.toPath();
                    Path targetPath = dropTargetDir.toPath().resolve(file.getName());
                    
                    try {
                        if (Files.exists(targetPath)) {
                            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                            alert.setTitle("File exists");
                            alert.setHeaderText("The file/folder already exists: " + file.getName());
                            alert.setContentText("Do you want to Overwrite or Skip?");
                            
                            ButtonType btnOverwrite = new ButtonType("Overwrite");
                            ButtonType btnSkip = new ButtonType("Skip", ButtonBar.ButtonData.CANCEL_CLOSE);
                            alert.getButtonTypes().setAll(btnOverwrite, btnSkip);
                            
                            Optional<ButtonType> result = alert.showAndWait();
                            if (result.isPresent() && result.get() == btnOverwrite) {
                                performTransfer(event.getTransferMode(), sourcePath, targetPath, true);
                            }
                        } else {
                            performTransfer(event.getTransferMode(), sourcePath, targetPath, false);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                success = true;
                if (onLinkStateChanged != null) {
                    onLinkStateChanged.run();
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }
    
    private void performTransfer(TransferMode mode, Path source, Path target, boolean replace) throws IOException {
        CopyOption[] options = replace ? new CopyOption[]{StandardCopyOption.REPLACE_EXISTING} : new CopyOption[]{};
        
        if (mode == TransferMode.MOVE) {
            Files.move(source, target, options);
        } else {
            copyRecursively(source.toFile(), target.toFile(), replace);
        }
    }

    private void copyRecursively(File src, File dest, boolean replace) throws IOException {
        if (src.isDirectory()) {
            if (!dest.exists()) dest.mkdir();
            for (String file : src.list()) {
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);
                copyRecursively(srcFile, destFile, replace);
            }
        } else {
            StandardCopyOption opt = replace ? StandardCopyOption.REPLACE_EXISTING : null;
            if (opt != null) {
                Files.copy(src.toPath(), dest.toPath(), opt);
            } else {
                Files.copy(src.toPath(), dest.toPath());
            }
        }
    }
}
