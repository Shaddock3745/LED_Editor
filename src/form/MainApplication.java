package form;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

public final class MainApplication extends Application {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		// TODO Auto-generated method stub
		FXMLLoader loader = new FXMLLoader(getClass().getResource("MainView.fxml"));
		Parent root = loader.load();
		MainController controller = loader.getController();
		Scene scene = new Scene(root, 1250, 840);

		stage.setTitle("LED Color Editor");
		stage.setScene(scene);
		stage.setResizable(false);

		stage.setOnCloseRequest(e -> {
			e.consume();

			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("Exit");
			alert.setHeaderText("Unsaved progress will be lost.\nDo you want to exit?");
			alert.showAndWait().ifPresent(t -> {
				if (t == ButtonType.OK)
					stage.close();
			});
		});

		stage.show();
		controller.addNewStep();
	}

}
