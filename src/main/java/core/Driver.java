package core;

import controller.MainScreenController;
import eu.hansolo.enzo.notification.Notification;
import handler.ConfigHandler;
import handler.StatisticsHandler;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

public class Driver extends Application{
    /**
     * The current version of the program.
     * Whenever a significant change is made, this should be changed along with the online handler.
     */
    private static final String PROGRAM_VERSION = "11";


    public static void main(final String[] args) {
        launch();
    }

    @Override
    public void init() {
        // Do something before the application starts.
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        final ConfigHandler configHandler = new ConfigHandler();
        final StatisticsHandler statisticsHandler = new StatisticsHandler();

        // Load Config File:
        configHandler.loadConfigSettings();

        // Check for Updates:
        if(configHandler.isCheckForUpdates()) {
            checkForUpdate();
        }

        // Setup the primary stage:
        primaryStage.getIcons().add(new Image("icon.png"));

        // Setup Enzo:
        Notification.Notifier.setPopupLocation(primaryStage, Pos.BOTTOM_CENTER);

        // Add the frst scene to the primary stage:
        final Scene scene = new Scene(new MainScreenController(primaryStage, configHandler, statisticsHandler).getView());

        scene.getStylesheets().add("global.css");
        scene.getRoot().getStyleClass().add("main-root");

        primaryStage.setTitle("Schillsaver - Powered by /g/entoomen\u00a9\u00ae");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Checks the website to see if there is a new version of the program.
     *
     * If there is a new version, then a dialog is shown to the user to explain
     * the situation and how to update.
     */
    public static void checkForUpdate() {
        // todo Fix the issue where an HTML tag is returned instead of the version number.
        try {
            final URL url = new URL("http://valkryst.com/schillsaver/version.txt");
            Scanner scanner = new Scanner(url.openStream());
            final String newVersion = scanner.nextLine();
            scanner.close();

            if(! newVersion.equals(PROGRAM_VERSION)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("New Version Available");
                alert.setHeaderText("This program is out of date.");
                alert.setContentText("Get the latest version at http://valkryst.com/schillsaver/\n\n" +
                                     "Current Version - " + PROGRAM_VERSION + "\n" +
                                     "New Version - " + newVersion);
                alert.showAndWait();
            }
        }
        catch(IOException e) {
            final Logger logger = LogManager.getLogger();
            logger.warn(e);
        }
    }
}
