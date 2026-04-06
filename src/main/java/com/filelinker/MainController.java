package com.filelinker;

import javafx.fxml.FXML;

public class MainController {

    @FXML private BrowserController leftBrowserController;
    @FXML private BrowserController rightBrowserController;

    @FXML
    public void initialize() {
        // When one browser changes link state or transfers files, notify both to refresh
        Runnable refreshBoth = () -> {
            leftBrowserController.refreshList();
            rightBrowserController.refreshList();
        };

        leftBrowserController.setOnLinkStateChanged(refreshBoth);
        rightBrowserController.setOnLinkStateChanged(refreshBoth);
    }
}
