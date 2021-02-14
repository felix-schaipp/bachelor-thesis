package svgTransformation;

import org.opencv.core.Core;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;
import javafx.stage.WindowEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
		try {
			//load the FXML resource
			Parent root = FXMLLoader.load(getClass().getResource("/frontend.fxml"));
			//create and style a new scene
			Scene scene = new Scene(root);
			scene.getStylesheets().add(getClass().getResource("/svgTransformationStyles.css").toExternalForm());
			primaryStage.setTitle("svgTransformation");

			//listen for key presses to move the xy-table
			scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
				@Override
				public void handle(KeyEvent event) {
					switch (event.getCode()) {
						case W:
							moveUp();
							break;
						case S:
							moveDown();
							break;
						case A:
							moveLeft();
							break;
						case D:
							moveRight();
							break;
					}
				}
			});

			scene.setOnKeyReleased(new EventHandler<KeyEvent>() {
				@Override
				public void handle(KeyEvent event) {
					switch (event.getCode()) {
						case W:
							stopXYTable();
							break;
						case S:
							stopXYTable();
							break;
						case A:
							stopXYTable();
							break;
						case D:
							stopXYTable();
							break;
					}
				}
			});

			primaryStage.setScene(scene);
			primaryStage.show();
			Controller controller = new Controller();
			primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
				public void handle(WindowEvent windowEvent) {
					controller.setClosed();
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
		}
    }

	/**
	 * on button press "up" move the xy-table in the up direction (+y)
	 */
	private void moveUp() {
		System.out.println("↑ Up");
		//detect press of up button
		//send ASCI code  to motor
	}

	/**
	 * on button press "down" move the xy-table in the down direction (-y)
	 */
	private void moveDown() {
		System.out.println("↓ Down");
		//detect press of up button
		//send ASCI code  to motor
	}

	/**
	 * on button press "left" move the xy-table in the left direction (-x)
	 */
	private void moveLeft() {
		System.out.println("← Left");
		//detect press of up button
		//send ASCI code  to motor
	}

	/**
	 * on button press "up" move the xy-table in the up-direction (+x)
	 */
	private void moveRight() {
		System.out.println("→ Right");
		//detect press of up button
		//send ASCI code  to motor
	}

	/**
	 * stops the motion of the xy-table when the key is released
	 */
	private void stopXYTable() {
		System.out.println("Stop the table!");
		//detect the release of the button pressed
	}

    public static void main(String[] args) {

		//load the native OpenCV library
		try {
			System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		} catch (UnsatisfiedLinkError e) {
			try {
				NativeUtils.loadLibraryFromJar("/" + System.mapLibraryName(Core.NATIVE_LIBRARY_NAME));
			} catch (IOException e1) {
				throw new RuntimeException(e1);
			}
		}

        launch(args);

	}
}
