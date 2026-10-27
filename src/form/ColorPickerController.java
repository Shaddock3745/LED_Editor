package form;

import customObject.Constants;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public final class ColorPickerController {

	@FXML
	private Rectangle canvas;

	@FXML
	private Slider chromaSlider;

	@FXML
	private Slider hueSlider;

	@FXML
	private CheckBox isLighted;

	@FXML
	private Button saveColor;

	@FXML
	private FlowPane tempColors;

	@FXML
	void lightingChange(ActionEvent event) {
		boolean disabled = !isLighted.isSelected();
		setDisabled(disabled);

		if (disabled)
			canvas.setFill(Constants.defaultBoxColor);
		else
			syncFromSlider();
	}

	@FXML
	void saveColor(ActionEvent event) {
		tempColorArray[tempColorIndex] = (Color) canvas.getFill();
		tempColorRectangles[tempColorIndex].setFill(tempColorArray[tempColorIndex]);

		tempColorIndex++;
		if (tempColorIndex == tempColorArray.length)
			tempColorIndex = 7;
	}

	@FXML
	void initialize() {
		hueSlider.valueProperty().addListener(_ -> {
			syncFromSlider();
		});
		chromaSlider.valueProperty().addListener(_ -> {
			syncFromSlider();
		});

		if (tempColorArray == null) {
			tempColorArray = new Color[30];

			tempColorArray[0] = Color.WHITE;
			for (int i = 0; i < 6; i++)
				tempColorArray[i + 1] = Color.hsb(60.0 * i, 1.0, 1.0);

			for (int i = 7; i < tempColorArray.length; i++)
				tempColorArray[i] = Color.WHITE;
		}

		tempColorRectangles = new Rectangle[tempColorArray.length];
		for (int i = 0; i < tempColorArray.length; i++) {
			tempColorRectangles[i] = createRectangle(tempColorArray[i]);
			tempColors.getChildren().add(tempColorRectangles[i]);
		}
	}

	private static int tempColorIndex = 7;
	private static Color[] tempColorArray;
	private Rectangle[] tempColorRectangles;

	private Rectangle createRectangle(Color fill) {
		Rectangle rectangle = new Rectangle();
		rectangle.setHeight(20);
		rectangle.setWidth(20);
		rectangle.setStroke(Color.BLACK);
		rectangle.setStrokeWidth(1);
		rectangle.setFill(fill);
		rectangle.setOnMouseClicked(e -> {
			if (e.getButton() == MouseButton.PRIMARY) {
				if (!isLighted.isSelected()) {
					isLighted.setSelected(true);
					setDisabled(false);
				}

				loadColor((Color) rectangle.getFill());
			}
		});

		return rectangle;
	}

	private void setDisabled(boolean disabled) {
		chromaSlider.setDisable(disabled);
		hueSlider.setDisable(disabled);
		saveColor.setDisable(disabled);
	}

	private void syncFromSlider() {
		double hue = hueSlider.getValue(), chroma = chromaSlider.getValue();
		canvas.setFill(Color.hsb(hue, chroma, 1.0));
	}

	private void loadColor(Color color) {
		double hue = Constants.getHue(color), chroma = Constants.getChroma(color);
		hueSlider.setValue(hue);
		chromaSlider.setValue(chroma);
		canvas.setFill(Color.hsb(hue, chroma, 1.0));
	}

	public void setColor(Color color) {
		boolean flag = Constants.isNotLightedColor(color);
		isLighted.setSelected(!flag);
		setDisabled(flag);

		if (flag)
			canvas.setFill(Constants.defaultBoxColor);
		else
			loadColor(color);
	}

	public Color getColor() {
		return (Color) canvas.getFill();
	}
}
