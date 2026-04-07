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
        
        fileTable.setSortPolicy(table -> {
            if (table.getRoot() != null) {
                sortItemsRecursively(table.getRoot(), table.getSortOrder());
            }
            return true;
        });
        
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
        if (node.getValue() != null && node.getValue().isDirectory()) {
            ObservableList<TreeItem<FileItem>> children = node.getChildren();

            boolean hasDummy = children.size() == 1 && children.get(0).getValue() != null && children.get(0).getValue().getFile().getName().isEmpty();
            if (hasDummy) return;

            File dir = node.getValue().getFile();
            File[] files = dir.listFiles();
            if (files != null) {
                children.removeIf(child -> {
                    File f = child.getValue().getFile();
                    return f != null && !f.exists();
                });
                for (File f : files) {
                    boolean found = false;
                    for (TreeItem<FileItem> child : children) {
                        if (child.getValue().getFile().equals(f)) {
                            found = true;
                            child.getValue().updateInfo();
                            break;
                        }
                    }
                    if (!found) {
                        FileItem subFileItem = new FileItem(f);
                        boolean linked = LinkManager.getInstance().isLinked(subFileItem.getName());
                        subFileItem.setLinked(linked);
                        if (linked) {
                            subFileItem.setLinkColor(LinkManager.getInstance().getColor(subFileItem.getName()));
                        }
                        children.add(createNode(subFileItem));
                    }
                }
            }

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
            sortItems(children, fileTable.getSortOrder());
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
                            sortItems(item.getChildren(), fileTable.getSortOrder());
                        }
                    }
                }
            });
        }
        return item;
    }

    private void sortItemsRecursively(TreeItem<FileItem> node, ObservableList<TreeTableColumn<FileItem, ?>> sortOrder) {
        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            sortItems(node.getChildren(), sortOrder);
            for (TreeItem<FileItem> child : node.getChildren()) {
                sortItemsRecursively(child, sortOrder);
            }
        }
    }

    private void sortItems(ObservableList<TreeItem<FileItem>> items, ObservableList<TreeTableColumn<FileItem, ?>> sortOrder) {
        TreeTableColumn<FileItem, ?> sortColumn = sortOrder.isEmpty() ? nameColumn : sortOrder.get(0);
        TreeTableColumn.SortType sortType = sortColumn == null ? TreeTableColumn.SortType.ASCENDING : sortColumn.getSortType();
        int dir = (sortType == TreeTableColumn.SortType.DESCENDING) ? -1 : 1;

        items.sort((o1, o2) -> {
            FileItem item1 = o1.getValue();
            FileItem item2 = o2.getValue();
            if (item1 == null || item2 == null || item1.getFile().getName().isEmpty()) return 0;
            
            if (item1.isLinked() && !item2.isLinked()) return -1;
            if (!item1.isLinked() && item2.isLinked()) return 1;
            
            if (item1.isDirectory() && !item2.isDirectory()) return -1;
            if (!item1.isDirectory() && item2.isDirectory()) return 1;
            
            int result = 0;
            if (sortColumn == dateColumn) {
                long d1 = item1.getFile().lastModified();
                long d2 = item2.getFile().lastModified();
                result = Long.compare(d1, d2);
            } else if (sortColumn == sizeColumn) {
                long s1 = item1.getFile().length();
                long s2 = item2.getFile().length();
                result = Long.compare(s1, s2);
            } else {
                result = compareNatural(item1.getName(), item2.getName());
            }
            return result * dir;
        });
    }

    private int compareNatural(String a, String b) {
        int i = 0, j = 0;
        int len1 = a.length(), len2 = b.length();
        while (i < len1 && j < len2) {
            char c1 = a.charAt(i);
            char c2 = b.charAt(j);
            if (Character.isDigit(c1) && Character.isDigit(c2)) {
                long num1 = 0, num2 = 0;
                while (i < len1 && Character.isDigit(a.charAt(i))) {
                    num1 = num1 * 10 + (a.charAt(i) - '0');
                    i++;
                }
                while (j < len2 && Character.isDigit(b.charAt(j))) {
                    num2 = num2 * 10 + (b.charAt(j) - '0');
                    j++;
                }
                if (num1 != num2) {
                    return Long.compare(num1, num2);
                }
            } else {
                int cmp = Character.toLowerCase(c1) - Character.toLowerCase(c2);
                if (cmp != 0) return cmp;
                i++;
                j++;
            }
        }
        return len1 - len2;
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

                container.getChildren().addAll(caretBtn, iconLabel, linkBtn, nameLabel);
            }

            @Override
            protected void updateItem(FileItem item, boolean empty) {
                super.updateItem(item, empty);
                
                TreeTableRow<?> row = getTreeTableRow();
                if (row != null) {
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
                    
                    int indent = Math.max(0, (level - 1) * 20);
                    container.setPadding(new javafx.geometry.Insets(0, 0, 0, indent));
                    
                    TreeItem<?> treeItem = row != null ? row.getTreeItem() : null;
                    boolean isExpanded = treeItem != null && treeItem.isExpanded();
                    
                    if (item.isDirectory()) {
                        caretBtn.setText(isExpanded ? "▼" : "▶");
                        caretBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: black; -fx-padding: 0; -fx-cursor: hand; -fx-font-weight: 900; -fx-font-size: 14px;");
                        caretBtn.setDisable(false);
                        linkBtn.setVisible(true);
                        linkBtn.setManaged(true);
                    } else {
                        caretBtn.setText("↳");
                        caretBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: gray; -fx-padding: 0; -fx-font-weight: 900; -fx-font-size: 18px;");
                        caretBtn.setDisable(true);
                        linkBtn.setVisible(false);
                        linkBtn.setManaged(false);
                    }
                    
                    iconLabel.setText(item.isDirectory() ? "📁" : "📄");
                    nameLabel.setText(item.getName());
                    
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

        fileTable.setRowFactory(tv -> {
            TreeTableRow<FileItem> row = new TreeTableRow<>();
            row.setOnDragEntered(event -> {
                if (event.getDragboard().hasFiles() && currentDirectory != null && !row.isEmpty()) {
                    row.setStyle("-fx-background-color: #e0e0e0;"); // Neutral 'bôi đen' background instead of lightblue
                }
            });
            row.setOnDragExited(event -> {
                row.setStyle(null);
            });
            row.setOnDragOver(event -> {
                if (event.getDragboard().hasFiles() && currentDirectory != null) {
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                    event.consume();
                }
            });
            row.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasFiles() && currentDirectory != null) {
                    File dropTargetDir = currentDirectory;
                    if (!row.isEmpty() && row.getItem() != null && row.getItem().getFile() != null) {
                        File itemFile = row.getItem().getFile();
                        dropTargetDir = itemFile.isDirectory() ? itemFile : itemFile.getParentFile();
                    }
                    boolean success = processDrop(db.getFiles(), dropTargetDir);
                    event.setDropCompleted(success);
                    event.consume();
                }
                row.setStyle(null);
            });
            return row;
        });

        fileTable.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles() && currentDirectory != null) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        fileTable.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles() && currentDirectory != null) {
                boolean success = processDrop(db.getFiles(), currentDirectory);
                event.setDropCompleted(success);
            }
            fileTable.setStyle("-fx-indent: 20;");
            event.consume();
        });
    }

    private TreeItem<FileItem> findTreeItem(TreeItem<FileItem> node, File target) {
        if (node == null || node.getValue() == null) return null;
        if (node.getValue().getFile() != null && node.getValue().getFile().equals(target)) return node;
        if (node.getChildren() != null) {
            for (TreeItem<FileItem> child : node.getChildren()) {
                if (child.getValue() != null && child.getValue().getFile().getName().isEmpty()) continue;
                TreeItem<FileItem> found = findTreeItem(child, target);
                if (found != null) return found;
            }
        }
        return null;
    }

    private boolean processDrop(List<File> files, File dropTargetDir) {
        List<File> topLevelFiles = new ArrayList<>();
        for (File f : files) {
            boolean hasParent = false;
            Path fPath = f.toPath().toAbsolutePath();
            for (File p : files) {
                if (p.equals(f)) continue;
                Path pPath = p.toPath().toAbsolutePath();
                if (fPath.startsWith(pPath)) {
                    hasParent = true;
                    break;
                }
            }
            if (!hasParent) topLevelFiles.add(f);
        }

        if (topLevelFiles.isEmpty()) return false;

        StringBuilder fileListMsg = new StringBuilder();
        int count = 0;
        List<File> landingFolders = new ArrayList<>();
        
        for (File f : topLevelFiles) {
            if (count < 10) {
                fileListMsg.append("- ").append(f.getName()).append("\n");
            }
            count++;
            
            // Predict routing for highlight
            File linkedParent = null;
            File temp = f;
            while (temp != null) {
                if (LinkManager.getInstance().isLinked(temp.getName())) linkedParent = temp;
                temp = temp.getParentFile();
            }
            
            File land = dropTargetDir;
            if (linkedParent != null && currentDirectory != null) {
                land = new File(currentDirectory, linkedParent.getName());
            }
            if (!landingFolders.contains(land)) landingFolders.add(land);
        }
        if (count > 10) {
            fileListMsg.append("... and ").append(count - 10).append(" more items.\n");
        }

        fileTable.getSelectionModel().clearSelection();
        boolean scrolled = false;
        for (File land : landingFolders) {
            TreeItem<FileItem> node = findTreeItem(fileTable.getRoot(), land);
            if (node != null) {
                fileTable.getSelectionModel().select(node);
                if (!scrolled) {
                    int index = fileTable.getRow(node);
                    if (index >= 0) fileTable.scrollTo(index);
                    scrolled = true;
                }
            }
        }

        Alert typeAlert = new Alert(Alert.AlertType.CONFIRMATION);
        typeAlert.setTitle("Transfer Action");
        typeAlert.setHeaderText("Transferring " + count + " items");
        typeAlert.setContentText("Files to transfer:\n" + fileListMsg.toString() + "\nChoose Action:");
        
        ButtonType btnCopy = new ButtonType("Copy");
        ButtonType btnMove = new ButtonType("Move");
        ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        typeAlert.getButtonTypes().setAll(btnCopy, btnMove, btnCancel);
        Optional<ButtonType> typeRes = typeAlert.showAndWait();
        
        fileTable.getSelectionModel().clearSelection();
        
        if (!typeRes.isPresent() || typeRes.get() == btnCancel) {
            return false;
        }
        
        TransferMode chosenMode = (typeRes.get() == btnMove) ? TransferMode.MOVE : TransferMode.COPY;



        List<Runnable> tasks = new ArrayList<>();
        List<Exception> errors = new java.util.concurrent.CopyOnWriteArrayList<>();
        boolean autoOverwrite = false;
        boolean autoSkip = false;

        for (File file : topLevelFiles) {
            if (file.getParentFile() != null && file.getParentFile().equals(dropTargetDir)) continue;
            
            Path sourcePath = file.toPath();
            Path targetPath = dropTargetDir.toPath().resolve(file.getName());
            
            // Smart Link Routing: Find the OUTERMOST linked parent folder
            File linkedParent = null;
            File temp = file;
            while (temp != null) {
                if (LinkManager.getInstance().isLinked(temp.getName())) {
                    linkedParent = temp;
                }
                temp = temp.getParentFile();
            }

            if (linkedParent != null && currentDirectory != null) {
                File targetLinkedFolder = new File(currentDirectory, linkedParent.getName());
                if (!targetLinkedFolder.exists()) {
                    targetLinkedFolder.mkdirs();
                }
                if (targetLinkedFolder.isDirectory()) {
                    Path relative = linkedParent.toPath().relativize(sourcePath.toAbsolutePath());
                    if (relative.toString().isEmpty()) {
                        targetPath = targetLinkedFolder.toPath();
                    } else {
                        targetPath = targetLinkedFolder.toPath().resolve(relative);
                    }
                    try {
                        Files.createDirectories(targetPath.getParent());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            
            final Path finalTargetPath = targetPath;
            if (Files.exists(targetPath)) {
                if (autoSkip) continue;
                if (!autoOverwrite) {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("File Conflict");
                    alert.setHeaderText("The file/folder already exists:\n" + targetPath.getFileName());
                    alert.setContentText("Do you want to Overwrite or Skip?");
                    
                    ButtonType btnOverwrite = new ButtonType("Overwrite");
                    ButtonType btnOverwriteAll = new ButtonType("Overwrite All");
                    ButtonType btnSkip = new ButtonType("Skip");
                    ButtonType btnSkipAll = new ButtonType("Skip All", ButtonBar.ButtonData.CANCEL_CLOSE);
                    alert.getButtonTypes().setAll(btnOverwrite, btnOverwriteAll, btnSkip, btnSkipAll);
                    
                    // Need to run dialogue on UI Thread safely because processDrop might be triggered
                    // Wait, processDrop is currently ON the UI thread! The background thread is launched LATER.
                    Optional<ButtonType> result = alert.showAndWait();
                    if (!result.isPresent() || result.get() == btnSkip) {
                        continue;
                    } else if (result.get() == btnSkipAll) {
                        autoSkip = true;
                        continue;
                    } else if (result.get() == btnOverwriteAll) {
                        autoOverwrite = true;
                    }
                }
                tasks.add(() -> {
                    try { 
                        performTransfer(chosenMode, sourcePath, finalTargetPath, true); 
                    } catch (Exception e) { 
                        e.printStackTrace(); 
                        errors.add(e);
                    }
                });
            } else {
                tasks.add(() -> {
                    try { 
                        performTransfer(chosenMode, sourcePath, finalTargetPath, false); 
                    } catch (Exception e) { 
                        e.printStackTrace();
                        errors.add(e);
                    }
                });
            }
        }
        
        if (!tasks.isEmpty()) {
            new Thread(() -> {
                for (Runnable t : tasks) {
                    t.run();
                }
                javafx.application.Platform.runLater(() -> {
                    if (onLinkStateChanged != null) onLinkStateChanged.run();
                    if (!errors.isEmpty()) {
                        Alert a = new Alert(Alert.AlertType.ERROR);
                        a.setTitle("Transfer Error");
                        a.setHeaderText("Some files failed to transfer");
                        a.setContentText("An error occurred with " + errors.size() + " item(s). Message: " + errors.get(0).getMessage());
                        a.showAndWait();
                    }
                });
            }).start();
        }
        return true;
    }
    
    private void performTransfer(TransferMode mode, Path source, Path target, boolean replace) throws IOException {
        if (mode == TransferMode.MOVE) {
            if (replace && Files.isDirectory(target)) {
                copyRecursively(source.toFile(), target.toFile(), replace);
                deleteRecursively(source.toFile());
            } else {
                CopyOption[] options = replace ? new CopyOption[]{StandardCopyOption.REPLACE_EXISTING} : new CopyOption[]{};
                Files.move(source, target, options);
            }
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
                if (!dest.exists()) {
                    Files.copy(src.toPath(), dest.toPath());
                }
            }
        }
    }
}
