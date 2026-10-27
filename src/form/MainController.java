package form;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import customObject.Constants;
import customObject.Step;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

public final class MainController {

	@FXML
	private RadioButton AllView;

	@FXML
	private RadioButton XY_View;

	@FXML
	private RadioButton XZ_View;

	@FXML
	private RadioButton YZ_View;

	@FXML
	private MenuItem clearSteps;

	@FXML
	private MenuItem exportFile;

	@FXML
	private MenuItem extendedSteps;

	@FXML
	private MenuItem importFile;

	@FXML
	private MenuItem importFileAndConcatenate;

	@FXML
	private Pane mainViewPane;

	@FXML
	private Label currentLayer;

	@FXML
	private Button nextLayer;

	@FXML
	private Button pauseAnimation;

	@FXML
	private Button playAnimation;

	@FXML
	private Button previousLayer;

	@FXML
	private CheckBox outlineVisible;

	@FXML
	private ScrollPane stepPanes;

	@FXML
	private VBox stepPanesContainer;

	@FXML
	private Button stopAnimation;

	@FXML
	private ToggleGroup viewModes;

	@FXML
	void changeToAllView(ActionEvent event) {
		viewMode = ViewMode.all;
		nextLayer.setDisable(true);
		previousLayer.setDisable(true);
		updateView();
	}

	@FXML
	void changeToXY(ActionEvent event) {
		viewMode = ViewMode.xy;
		nextLayer.setDisable(xyLayer == GRID - 1);
		previousLayer.setDisable(xyLayer == 0);
		updateView();
	}

	@FXML
	void changeToXZ(ActionEvent event) {
		viewMode = ViewMode.xz;
		nextLayer.setDisable(xzLayer == GRID - 1);
		previousLayer.setDisable(xzLayer == 0);
		updateView();
	}

	@FXML
	void changeToYZ(ActionEvent event) {
		viewMode = ViewMode.yz;
		nextLayer.setDisable(yzLayer == GRID - 1);
		previousLayer.setDisable(yzLayer == 0);
		updateView();
	}

	@FXML
	void clearSteps(ActionEvent event) {
		stopAnimation(null);

		Stage stage = (Stage) mainViewPane.getScene().getWindow();
		Alert comfirmtion = new Alert(AlertType.CONFIRMATION);
		comfirmtion
				.setHeaderText("All steps will be cleared.\nUnsaved progress will be lost.\nDo you want to continue?");
		comfirmtion.setTitle("Clear all steps");
		comfirmtion.initOwner(stage);
		comfirmtion.showAndWait().ifPresent(b -> {
			if (b == ButtonType.OK) {
				steps.clear();
				addNewStep();

				Alert ok = new Alert(AlertType.INFORMATION, "Clear successfully.");
				ok.setTitle("Information");
				ok.setHeaderText(null);
				ok.initOwner(stage);
				ok.show();
			}
		});
	}

	@FXML
	void exportFile(ActionEvent event) {
		Stage stage = (Stage) mainViewPane.getScene().getWindow();

		try {
			if (steps.size() > Constants.maxStepNumber)
				throw new Exception(String.format("This file is too big, total step number must smaller than %d.",
						Constants.maxStepNumber));

		} catch (Exception e) {
			showErrorAlert(e.getMessage(), stage);
			return;
		}

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Export");
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
				String.format("Script Files (*%s)", fileExtension), "*" + fileExtension));
		fileChooser.setInitialFileName(String.format("ExportFile_%s",
				DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())));

		File saveFile = fileChooser.showSaveDialog(stage);

		if (saveFile != null) {
			DecimalFormat formattter = new DecimalFormat("0.0");
			StringBuilder contentBuilder = new StringBuilder();
			contentBuilder.append("float PROGMEM delay_T[]={");

			for (int i = 0; i < steps.size(); i++) {
				if (i > 0)
					contentBuilder.append(',');

				contentBuilder.append(formattter.format(steps.get(i).getDelay()));
			}

			contentBuilder.append("};\nuint32_t PROGMEM LED[][512]={");

			for (int i = 0; i < steps.size(); i++) {
				if (i > 0)
					contentBuilder.append(',');

				contentBuilder.append("\n{");

				for (int x = 0; x < GRID; x++)
					for (int y = GRID - 1; y >= 0; y--)
						for (int j = 0; j < GRID; j++) {
							if (x != 0 || y != GRID - 1 || j != 0)
								contentBuilder.append(',');

							int z = y % 2 == 0 ? GRID - j - 1 : j;
							Color color = steps.get(i).getColor(x, y, z);

							if (Constants.isNotLightedColor(color))
								contentBuilder.append(0);
							else
								contentBuilder.append(colorToInt(color));
						}

				contentBuilder.append('}');
			}

			contentBuilder.append("\n};");

			try {
				Files.write(saveFile.toPath(), contentBuilder.toString().getBytes(StandardCharsets.UTF_8),
						StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				Alert ok = new Alert(AlertType.INFORMATION, "Export successfully.");
				ok.setHeaderText(null);
				ok.setTitle("Information");
				ok.initOwner(stage);
				ok.show();
			} catch (IOException e) {
				showErrorAlert(e.getMessage(), stage);
			}
		}

	}

	@FXML
	void importFile(ActionEvent event) {
		importFileData(true);
	}

	@FXML
	void importFileAndConcatenate(ActionEvent event) {
		importFileData(false);
	}

	@FXML
	void extendedSteps(ActionEvent event) {
		stopAnimation(null);
		Stage stage = (Stage) mainViewPane.getScene().getWindow();
		Alert comfirmtion = new Alert(AlertType.CONFIRMATION);
		comfirmtion.setHeaderText("All steps in list will be extended.\nDo you want to continue?");
		comfirmtion.setTitle("Extension");
		comfirmtion.initOwner(stage);
		comfirmtion.showAndWait().ifPresent(b -> {
			if (b == ButtonType.OK) {
				try {
					if (steps.size() == 1)
						throw new Exception("There's only one step");

					double totalDelayTime = 0;

					for (Step step : steps) {
						if (step.isAllNotLighted())
							throw new Exception(
									String.format("All LEDs in one step are not lighted： %s", step.getName()));

						totalDelayTime += step.getDelay();
					}

					if (totalDelayTime > Constants.maxTotalDelayTime)
						throw new Exception(
								String.format("This script is too long, total delay time must smaller than %f.",
										Constants.maxTotalDelayTime));

				} catch (Exception e) {
					showErrorAlert(e.getMessage(), stage);
					return;
				}

				List<Step> extensionSteps = customObject.StepInterpolator.extension(steps);
				steps.clear();

				for (Step step : extensionSteps) {
					steps.add(step);
					addEvent(step);
				}

				refreshStepsList();

				selectStep(steps.getFirst());
				Alert ok = new Alert(AlertType.INFORMATION, "Extended successfully.");
				ok.setTitle("Information");
				ok.setHeaderText(null);
				ok.initOwner(stage);
				ok.show();
			}
		});
	}

	@FXML
	void nextLayer(ActionEvent event) {
		if (nextLayer.isDisable())
			return;

		switch (viewMode) {
		case xy:
			xyLayer++;
			if (xyLayer == GRID - 1)
				nextLayer.setDisable(true);

			break;
		case yz:
			yzLayer++;
			if (yzLayer == GRID - 1)
				nextLayer.setDisable(true);

			break;
		case xz:
			xzLayer++;
			if (xzLayer == GRID - 1)
				nextLayer.setDisable(true);

			break;
		default:
			break;
		}
		previousLayer.setDisable(false);
		updateView();
	}

	@FXML
	void previousLayer(ActionEvent event) {
		if (previousLayer.isDisable())
			return;

		switch (viewMode) {
		case xy:
			xyLayer--;
			if (xyLayer == 0)
				previousLayer.setDisable(true);

			break;
		case yz:
			yzLayer--;
			if (yzLayer == 0)
				previousLayer.setDisable(true);

			break;
		case xz:
			xzLayer--;
			if (xzLayer == 0)
				previousLayer.setDisable(true);

			break;
		default:
			break;
		}
		nextLayer.setDisable(false);
		updateView();
	}

	@FXML
	void pauseAnimation(ActionEvent event) {
		if (timeline != null)
			timeline.stop();

		playAnimation.setDisable(false);
		pauseAnimation.setDisable(true);

		addNewStepPane.setDisable(false);
		for (Step step : steps)
			step.getPane().setDisable(false);

		animationPlaying = false;
		selectedStep.getPane().setStyle(selectedStepPaneStyle);
	}

	@FXML
	void playAnimation(ActionEvent event) {
		playAnimation.setDisable(true);
		pauseAnimation.setDisable(false);
		stopAnimation.setDisable(false);

		addNewStepPane.setDisable(true);
		for (Step step : steps)
			step.getPane().setDisable(true);

		animationPlaying = true;
		playStep();
	}

	@FXML
	void stopAnimation(ActionEvent event) {
		if (timeline != null)
			timeline.stop();

		playAnimation.setDisable(false);
		pauseAnimation.setDisable(true);
		stopAnimation.setDisable(true);

		addNewStepPane.setDisable(false);
		for (Step step : steps)
			step.getPane().setDisable(false);

		animationPlaying = false;
		selectStep(steps.get(0));
		stepPanes.setVvalue(0);
	}

	@FXML
	void outlineVisibleChanged(ActionEvent event) {
		updateOutlineVisible();
	}

	@FXML
	public void initialize() {
		outlineVisible.selectedProperty().addListener(_ -> {
			updateOutlineVisible();
		});

		setFirstLEDGroup();
		loadMainView();
		changeToXY(null);
	}

	private enum ViewMode {
		all, xy, yz, xz;
	}

	private static final String fileExtension = ".h";
	private static final int SPACING = 30, GRID = 8, WIDTH = 20, startPosition = -SPACING * (GRID - 1) / 2,
			INITIAL_ANGLE_X = 20, INITIAL_ANGLE_Y = -45;
	private static final String defaultPaneStyle = "-fx-background-radius: 10; -fx-background-color: lightgray; -fx-background-insets: 2; -fx-border-color: black; -fx-border-radius: 10; -fx-border-insets: 2;",
			hoverPaneStyle = "-fx-background-radius: 10; -fx-background-color: #E9E9E9; -fx-background-insets: 2; -fx-border-color: black; -fx-border-radius: 10; -fx-border-insets: 2;",
			selectedStepPaneStyle = "-fx-background-radius: 10; -fx-background-color: lightgreen; -fx-background-insets: 2; -fx-border-color: black; -fx-border-radius: 10; -fx-border-insets: 2;",
			stepPlayingStyleFormat = "-fx-background-radius: 10; -fx-background-color: linear-gradient(to right, lightgreen %d%%, lightgray %d%%); -fx-background-insets: 2; -fx-border-color: black; -fx-border-radius: 10; -fx-border-insets: 2;",
			wrongFileFormatWarning = "This file is in a wrong format.",
			wrongFileArrayLengthWarning = "The lengths of the Delay array and the color value array are not equal.";
	private int xyLayer = 0, yzLayer = 0, xzLayer = 0;
	private int lastFaseX = Integer.MIN_VALUE, lastFaseY = Integer.MIN_VALUE, lastFaceZ = Integer.MIN_VALUE;
	private boolean lastOutlineVisible = true, animationPlaying = false;
	private Group LEDs;
	private Box[][][] boxs = new Box[GRID][GRID][GRID], outlines = new Box[GRID][GRID][GRID];
	private List<Step> steps = new LinkedList<Step>();
	private Step selectedStep = null;
	private ViewMode viewMode = ViewMode.xy;
	private Pane addNewStepPane = createAddNewStepPane();
	private Timeline timeline;
	private PerspectiveCamera camera;
	private static final String delayPattern = "(?s)float\\s+PROGMEM\\s+delay_T\\s*\\[\\]\\s*=\\s*\\{([^}]*)\\}\\s*;",
			allColorPattern = "(?s)uint32_t\\s+PROGMEM\\s+LED\\s*\\[\\]\\s*\\[512\\]\\s*=\\s*(\\{\\s*(?:\\{[^{}]*\\}\\s*,\\s*)*\\{[^{}]*\\}\\s*\\})\\s*;",
			eachColorPattern = "(?s)(?<=\\{)([^{}]*?)(?=\\})";

	private static double[] readDelayFromText(String text) throws Exception {
		Matcher matcher = Pattern.compile(delayPattern).matcher(text);

		if (matcher.find()) {
			String result = matcher.group(1);

			return Arrays.stream(result.split(",")).map(String::trim).filter(s -> !s.isEmpty())
					.mapToDouble(Double::parseDouble).toArray();
		} else
			throw new Exception(wrongFileFormatWarning);
	}

	private static Color[][] readColorFromText(String text) throws Exception {
		Matcher allMatcher = Pattern.compile(allColorPattern).matcher(text);

		if (allMatcher.find()) {
			String allResult = allMatcher.group(1);
			Matcher eachMatcher = Pattern.compile(eachColorPattern).matcher(allResult);
			List<Color[]> colorArraysList = new ArrayList<>();

			while (eachMatcher.find()) {
				String result = eachMatcher.group(1);
				Color[] colors = Arrays.stream(result.split(",")).map(String::trim).filter(s -> !s.isEmpty())
						.mapToInt(Integer::parseInt).mapToObj(x -> intToColor(x)).toArray(Color[]::new);

				colorArraysList.add(colors);
			}

			return colorArraysList.toArray(Color[][]::new);
		} else
			throw new Exception(wrongFileFormatWarning);
	}

	private static void showErrorAlert(String errMassage, Stage stage) {
		Alert err = new Alert(AlertType.ERROR, errMassage);
		err.setHeaderText(null);
		err.setTitle("Error");
		err.initOwner(stage);
		err.show();
	}

	private void importFileData(boolean clear) {
		stopAnimation(null);

		Stage stage = (Stage) mainViewPane.getScene().getWindow();
		Alert comfirmtion = new Alert(AlertType.CONFIRMATION);
		comfirmtion.setHeaderText(
				clear ? "Import data from file.\nUnsaved progress will be lost.\nDo you want to continue?"
						: "Import data from file and concatenate them.\nDo you want to continue?");
		comfirmtion.setTitle("Import");
		comfirmtion.initOwner(stage);
		comfirmtion.showAndWait().ifPresent(b -> {
			if (b == ButtonType.OK) {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle("Import");
				fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
						String.format("Script Files (*%s)", fileExtension), "*" + fileExtension));
				File readFile = fileChooser.showOpenDialog(stage);

				if (readFile != null) {
					try {
						String content = Files.readString(readFile.toPath(), StandardCharsets.UTF_8);
						double[] delays = readDelayFromText(content);
						Color[][] colorsBySteps = readColorFromText(content);

						if (delays.length != colorsBySteps.length)
							throw new Exception(wrongFileArrayLengthWarning);
						else {
							if (clear)
								steps.clear();

							for (int i = 0; i < delays.length; i++) {
								Color[][][] colors = new Color[GRID][GRID][GRID];
								int index = 0;
								double delay = BigDecimal.valueOf(delays[i]).setScale(1, RoundingMode.HALF_UP)
										.doubleValue();

								if (delay < Constants.minDelayTimePerStep || delay > Constants.maxDelayTimePerStep)
									throw new Exception(wrongFileFormatWarning);

								for (int x = 0; x < GRID; x++)
									for (int y = GRID - 1; y >= 0; y--)
										for (int j = 0; j < GRID; j++) {
											int z = y % 2 == 0 ? GRID - j - 1 : j;
											Color color = colorsBySteps[i][index++];
											colors[x][y][z] = Constants.isNotLightedColor(color)
													? Constants.defaultBoxColor
													: color;
										}

								addLoadedStep(delay, colors);
							}

							refreshStepsList();
							selectStep(steps.getFirst());

							Alert ok = new Alert(AlertType.INFORMATION, "Import successfully.");
							ok.setTitle("Information");
							ok.setHeaderText(null);
							ok.initOwner(stage);
							ok.show();
						}
					} catch (Exception e) {
						showErrorAlert(e.getMessage(), stage);
					}
				}
			}
		});
	}

	private void playStep() {
		long startTime = System.currentTimeMillis();

		timeline = new Timeline(new KeyFrame(Duration.millis(20), _ -> {
			double elapsed = (System.currentTimeMillis() - startTime) / 1000.0;
			int percent = (int) (Math.min(1.0, elapsed / selectedStep.getDelay()) * 100);
			selectedStep.getPane().setStyle(String.format(stepPlayingStyleFormat, percent, percent + 1));

			if (percent >= 100) {
				timeline.stop();

				int index = steps.indexOf(selectedStep) + 1;
				selectStep(steps.get(index % steps.size()));
				playStep();
			}
		}));
		timeline.setCycleCount(Animation.INDEFINITE);
		timeline.play();
	}

	public void goUp(Step step) {
		int index = steps.indexOf(step);

		if (index > 0) {
			Collections.swap(steps, index, index - 1);
			refreshStepsList();
		}
	}

	public void goDown(Step step) {
		int index = steps.indexOf(step);

		if (index < steps.size() - 1) {
			Collections.swap(steps, index, index + 1);
			refreshStepsList();
		}
	}

	public void renameStep(Step step) {
		TextInputDialog dialog = new TextInputDialog();
		dialog.setTitle("Rename Step");
		dialog.initOwner((Stage) mainViewPane.getScene().getWindow());
		dialog.setHeaderText("Input the new name");
		dialog.getEditor().setText(step.getName());
		dialog.showAndWait().ifPresent(name -> {
			step.setName(name);
		});
	}

	public void copyStep(Step originalStep) {
		Step step = Step.copyFromStep(originalStep);
		addEvent(step);

		steps.add(steps.indexOf(originalStep) + 1, step);
		refreshStepsList();
	}

	public void deleteStep(Step step) {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle(String.format("Delete %s", step.getName()));
		alert.setHeaderText("Are you sure?");
		alert.initOwner((Stage) mainViewPane.getScene().getWindow());
		alert.showAndWait().ifPresent(t -> {
			if (t == ButtonType.OK) {

				if (step == selectedStep)
					if (steps.size() == 1) {
						steps.remove(step);
						addNewStep();
					} else if (steps.getLast() == step) {
						selectStep(steps.get(steps.size() - 2));
						steps.remove(step);
						refreshStepsList();
					} else {
						selectStep(steps.get(steps.indexOf(step) + 1));
						steps.remove(step);
						refreshStepsList();
					}
				else {
					steps.remove(step);
					refreshStepsList();
				}
			}
		});
	}

	private void addEvent(Step step) {
		step.getPane().setOnMouseClicked(e -> {
			if (e.getButton() == MouseButton.PRIMARY)
				selectStep(step);
		});
		step.getPane().setOnMouseEntered(_ -> {
			if (selectedStep != step)
				step.getPane().setStyle(hoverPaneStyle);
		});
		step.getPane().setOnMouseExited(_ -> {
			if (selectedStep != step)
				step.getPane().setStyle(defaultPaneStyle);
		});
	}

	private Pane createAddNewStepPane() {
		StackPane pane = new StackPane();
		pane.setPrefHeight(40);
		pane.setStyle(defaultPaneStyle);

		Label label = new Label("+");
		label.setStyle("-fx-font-size: 24px;");
		pane.getChildren().add(label);

		Tooltip tooltip = new Tooltip("New step");
		Tooltip.install(pane, tooltip);

		pane.setOnMouseClicked(e -> {
			if (e.getButton() == MouseButton.PRIMARY)
				addNewStep();
		});
		pane.setOnMouseEntered(_ -> {
			pane.setStyle(hoverPaneStyle);
		});
		pane.setOnMouseExited(_ -> {
			pane.setStyle(defaultPaneStyle);
		});

		return pane;
	}

	private void refreshStepsList() {
		stepPanesContainer.getChildren().clear();

		for (Step step : steps)
			stepPanesContainer.getChildren().add(step.getPane());

		stepPanesContainer.getChildren().add(addNewStepPane);
	}

	public void addLoadedStep(double delay, Color[][][] colors) {
		Step step = Step.newStep(steps.size() + 1, delay, colors, this);
		addEvent(step);
		steps.add(step);
	}

	public void addNewStep() {
		Step step = Step.newDefaultStep(steps.size() + 1, GRID, this);
		addEvent(step);
		steps.add(step);
		selectStep(step);
		refreshStepsList();
	}

	public void selectStep(Step step) {
		if (selectedStep != null) {
			selectedStep.getPane().setStyle(defaultPaneStyle);
		}

		if (!animationPlaying)
			step.getPane().setStyle(selectedStepPaneStyle);

		for (int x = 0; x < boxs.length; x++)
			for (int y = 0; y < boxs.length; y++)
				for (int z = 0; z < boxs.length; z++)
					((PhongMaterial) boxs[x][y][z].getMaterial()).setDiffuseColor(step.getColor(x, y, z));

		selectedStep = step;
		updateView();
	}

	private void setFirstLEDGroup() {
		LEDs = new Group();
		int translateX, translateY, translageZ;

		for (int i = 0; i < GRID; i++) {
			translateX = startPosition + i * SPACING;

			for (int j = 0; j < GRID; j++) {
				translateY = startPosition + j * SPACING;

				for (int k = 0; k < GRID; k++) {
					translageZ = startPosition + k * SPACING;

					Box node = new Box(WIDTH, WIDTH, WIDTH);
					node.setTranslateX(translateX);
					node.setTranslateY(translateY);
					node.setTranslateZ(translageZ);

					PhongMaterial material = new PhongMaterial();
					material.setDiffuseColor(Constants.defaultBoxColor);
					node.setMaterial(material);
					node.setOnMouseClicked(e -> {
						if (e.getButton() == MouseButton.PRIMARY && !animationPlaying) {
							int x = (int) (node.getTranslateX() - startPosition) / SPACING;
							int y = (int) (node.getTranslateY() - startPosition) / SPACING;
							int z = (int) (node.getTranslateZ() - startPosition) / SPACING;

							try {
								FXMLLoader loader = new FXMLLoader(getClass().getResource("ColorPickerView.fxml"));
								Parent content = loader.load();
								ColorPickerController controller = loader.getController();
								controller.setColor(((PhongMaterial) node.getMaterial()).getDiffuseColor());

								Dialog<Color> dialog = new Dialog<Color>();
								int serial = x * 64 + (GRID - y - 1) * 8 + (y % 2 == 0 ? (GRID - z - 1) : z);
								dialog.setTitle(String.format("Pick color at X：%d, Y：%d, Z：%d (Serial：%d)", x,
										GRID - y - 1, z, serial));
								dialog.initOwner((Stage) mainViewPane.getScene().getWindow());
								dialog.getDialogPane().setContent(content);
								dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
								dialog.setResultConverter(b -> {
									if (b == ButtonType.OK)
										return controller.getColor();
									else
										return null;
								});
								dialog.showAndWait().ifPresent(color -> {
									material.setDiffuseColor(color);
									selectedStep.setColor(x, y, z, color);
								});
							} catch (Exception exc) {
								exc.printStackTrace();
							}
						}
					});

					boxs[i][j][k] = node;
					LEDs.getChildren().add(node);

					if (i == 0 || i == GRID - 1 || j == 0 || j == GRID - 1 || k == 0 || k == GRID - 1) {
						Box outline = new Box(WIDTH, WIDTH, WIDTH);
						outline.setTranslateX(translateX);
						outline.setTranslateY(translateY);
						outline.setTranslateZ(translageZ);
						outline.setMaterial(new PhongMaterial(Constants.outlineColor));
						outline.setDrawMode(DrawMode.LINE);
						outline.setMouseTransparent(true);
						outlines[i][j][k] = outline;
						LEDs.getChildren().add(outline);
					}
				}
			}
		}
	}

	public static int colorToInt(Color color) {
		int r = (int) Math.round(color.getRed() * 255);
		int g = (int) Math.round(color.getGreen() * 255);
		int b = (int) Math.round(color.getBlue() * 255);

		return (r << 16) | (g << 8) | b;
	}

	public static Color intToColor(int rgb) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;

		return Color.rgb(r, g, b);
	}

	private static void hideBox(Box box) {
		box.setVisible(false);
	}

	private static void displayBox(Box box) {
		box.setVisible(true);
	}

	private static boolean colorIllegal(Box box) {
		PhongMaterial material = (PhongMaterial) box.getMaterial();

		return Constants.isNotLightedColor(material.getDiffuseColor());
	}

	private void updateView() {
		switch (viewMode) {
		case all:
			for (int i = 0; i < GRID; i++)
				for (int j = 0; j < GRID; j++)
					for (int k = 0; k < GRID; k++)
						if (colorIllegal(boxs[i][j][k]))
							hideBox(boxs[i][j][k]);
						else
							displayBox(boxs[i][j][k]);

			currentLayer.setText("Current Layer： N/A");
			break;
		case xy:
			for (int i = 0; i < GRID; i++)
				for (int j = 0; j < GRID; j++)
					for (int k = 0; k < GRID; k++)
						if (k != xyLayer)
							hideBox(boxs[i][j][k]);
						else
							displayBox(boxs[i][j][k]);

			currentLayer.setText(String.format("Current Layer： %d", xyLayer));
			break;
		case yz:
			for (int i = 0; i < GRID; i++)
				for (int j = 0; j < GRID; j++)
					for (int k = 0; k < GRID; k++)
						if (i != yzLayer)
							hideBox(boxs[i][j][k]);
						else
							displayBox(boxs[i][j][k]);

			currentLayer.setText(String.format("Current Layer： %d", yzLayer));
			break;
		case xz:
			for (int i = 0; i < GRID; i++)
				for (int j = 0; j < GRID; j++)
					for (int k = 0; k < GRID; k++)
						if (j != GRID - xzLayer - 1)
							hideBox(boxs[i][j][k]);
						else
							displayBox(boxs[i][j][k]);

			currentLayer.setText(String.format("Current Layer： %d", xzLayer));
		}
	}

	private void updateOutlineVisible() {
		Point3D camInScene = camera.localToScene(Point3D.ZERO);
		Point3D camInLeds = LEDs.sceneToLocal(camInScene);

		int faceX = camInLeds.getX() >= 0 ? GRID - 1 : 0;
		int faceY = camInLeds.getY() >= 0 ? GRID - 1 : 0;
		int faceZ = camInLeds.getZ() >= 0 ? GRID - 1 : 0;

		if (faceX != lastFaseX || faceY != lastFaseY || faceZ != lastFaceZ
				|| outlineVisible.isSelected() != lastOutlineVisible) {
			lastFaseX = faceX;
			lastFaseY = faceY;
			lastFaceZ = faceZ;
			lastOutlineVisible = outlineVisible.isSelected();

			for (int i = 0; i < GRID; i++)
				for (int j = 0; j < GRID; j++)
					for (int k = 0; k < GRID; k++) {
						Box outline = outlines[i][j][k];
						if (outline != null)
							outline.setVisible(outlineVisible.isSelected() && (i == faceX || j == faceY || k == faceZ));
					}
		}
	}

	private void loadMainView() {
		Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
		Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
		rotateX.setAngle(INITIAL_ANGLE_X);
		rotateY.setAngle(INITIAL_ANGLE_Y);
		LEDs.getTransforms().addAll(rotateX, rotateY);

		SubScene ledScene = new SubScene(LEDs, mainViewPane.getWidth(), mainViewPane.getHeight(), true,
				SceneAntialiasing.BALANCED);
		ledScene.setFill(Color.LIGHTGRAY);

		camera = new PerspectiveCamera(true);
		camera.setTranslateZ(-SPACING * 25);
		camera.setNearClip(1);
		camera.setFarClip(2000);

		ledScene.setCamera(camera);
		ledScene.widthProperty().bind(mainViewPane.widthProperty());
		ledScene.heightProperty().bind(mainViewPane.heightProperty());

		Group axisGroup = new Group();
		axisGroup.setTranslateX(40);
		axisGroup.setTranslateY(40);

		Cylinder xAxis = new Cylinder(1, 30);
		xAxis.setMaterial(new PhongMaterial(Color.RED));
		xAxis.setTranslateX(15);
		xAxis.setRotationAxis(Rotate.Z_AXIS);
		xAxis.setRotate(90);

		Cylinder yAxis = new Cylinder(1, 30);
		yAxis.setMaterial(new PhongMaterial(Color.GREEN));
		yAxis.setTranslateY(-15);

		Cylinder zAxis = new Cylinder(1, 30);
		zAxis.setMaterial(new PhongMaterial(Color.BLUE));
		zAxis.setTranslateZ(15);
		zAxis.setRotationAxis(Rotate.X_AXIS);
		zAxis.setRotate(90);

		Text xLabel = new Text("X");
		xLabel.setFill(Color.RED);
		xLabel.setFont(Font.font(12));
		xLabel.setTranslateX(28);

		Text yLabel = new Text("Y");
		yLabel.setFill(Color.GREEN);
		yLabel.setFont(Font.font(12));
		yLabel.setTranslateY(-28);

		Text zLabel = new Text("Z");
		zLabel.setFill(Color.BLUE);
		zLabel.setFont(Font.font(12));
		zLabel.setTranslateZ(28);

		axisGroup.getChildren().addAll(xLabel, yLabel, zLabel, xAxis, yAxis, zAxis);
		axisGroup.getTransforms().setAll(new Rotate(INITIAL_ANGLE_X, Rotate.X_AXIS),
				new Rotate(INITIAL_ANGLE_Y, Rotate.Y_AXIS));

		SubScene axisScene = new SubScene(axisGroup, 80, 80, true, SceneAntialiasing.BALANCED);
		axisScene.setCamera(new PerspectiveCamera(false));
		axisScene.setFill(Color.TRANSPARENT);

		ledScene.setOnMousePressed(e -> {
			if (e.getButton() == MouseButton.SECONDARY) {
				mainViewPane.requestFocus();
				LEDs.setUserData(new double[] { e.getSceneX(), e.getSceneY(), rotateX.getAngle(), rotateY.getAngle() });
			}
		});

		ledScene.setOnMouseDragged(e -> {
			if (e.getButton() == MouseButton.SECONDARY) {
				double[] arr = (double[]) LEDs.getUserData();
				double dx = e.getSceneX() - arr[0];
				double dy = e.getSceneY() - arr[1];
				rotateY.setAngle(arr[3] + dx * 0.5);
				rotateX.setAngle(arr[2] - dy * 0.5);

				axisGroup.getTransforms().setAll(new Rotate(rotateX.getAngle(), Rotate.X_AXIS),
						new Rotate(rotateY.getAngle(), Rotate.Y_AXIS));

				updateOutlineVisible();
			}
		});

		ledScene.setOnScroll(e -> {
			camera.setTranslateZ(
					Math.max(Math.min(camera.getTranslateZ() + e.getDeltaY(), -SPACING * 8), -SPACING * 40));
		});

		axisScene.setOnMouseClicked(_ -> {
			rotateX.setAngle(INITIAL_ANGLE_X);
			rotateY.setAngle(INITIAL_ANGLE_Y);
			camera.setTranslateZ(-SPACING * 25);

			axisGroup.getTransforms().setAll(new Rotate(INITIAL_ANGLE_X, Rotate.X_AXIS),
					new Rotate(INITIAL_ANGLE_Y, Rotate.Y_AXIS));

			updateOutlineVisible();
		});

		StackPane stackPane = new StackPane();
		stackPane.getChildren().addAll(ledScene, axisScene);
		StackPane.setAlignment(axisScene, Pos.TOP_RIGHT);
		updateOutlineVisible();

		mainViewPane.getChildren().add(stackPane);
	}
}
