// Coordinates standard UI synchronization between the two independent BrowserController instances.
package com.filelinker;

import javafx.fxml.FXML;

public class MainController {

    @FXML private BrowserController leftBrowserController;
    @FXML private BrowserController rightBrowserController;

    @FXML
    public void initialize() {
        Runnable refreshBoth = () -> {
            leftBrowserController.refreshList();
            rightBrowserController.refreshList();
        };

        leftBrowserController.setOnLinkStateChanged(refreshBoth);
        rightBrowserController.setOnLinkStateChanged(refreshBoth);
    }
}
