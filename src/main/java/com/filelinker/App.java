// Initializes the primary JavaFX stage, scene graph, and loads the main FXML layout window.
package com.filelinker;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("main"), 1000, 600);
        try {
            stage.getIcons().add(new javafx.scene.image.Image(App.class.getResourceAsStream("icon.png")));
        } catch (Exception e) {
            System.out.println("No icon.png found yet.");
        }
        stage.setTitle("File Linker");
        stage.setScene(scene);
        stage.show();
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }

}