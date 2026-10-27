package form;

import customObject.Constants;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

public final class StepPaneController {

	@FXML
	private Label stepName;

	@FXML
	private Spinner<Double> delayTime;

	@FXML
	private Button goDown;

	@FXML
	private Button goUp;

	@FXML
	private Button copyStep;

	@FXML
	private Button deleteStep;

	@FXML
	private Button renameStep;

	@FXML
	void initialize() {
		delayTime.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(Constants.minDelayTimePerStep,
				Constants.maxDelayTimePerStep, 1, Constants.delayTimeMinDelta));
	}

	public void setName(String name) {
		stepName.setText(name);
	}

	public void setDelayTime(double delay) {
		delayTime.getValueFactory().setValue(delay);
	}

	public String getName() {
		return stepName.getText();
	}

	public double getDelayTime() {
		return delayTime.getValue();
	}

	public final Button getGoDown() {
		return goDown;
	}

	public final Button getGoUp() {
		return goUp;
	}

	public final Button getCopyStep() {
		return copyStep;
	}

	public final Button getDeleteStep() {
		return deleteStep;
	}

	public final Button getRenameStep() {
		return renameStep;
	}
}
