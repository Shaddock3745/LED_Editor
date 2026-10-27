package customObject;

import form.MainController;
import form.StepPaneController;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public final class Step {
	private Color[][][] colors;
	private Pane pane;
	private StepPaneController controller;
	private MainController parentController;

	public Step(String name, double delay, Color[][][] colors, Pane pane, StepPaneController controller,
			MainController parentController) {
		this.colors = colors;
		this.pane = pane;
		this.controller = controller;
		this.parentController = parentController;

		controller.setName(name);
		controller.setDelayTime(delay);

		addEvents(parentController);

	}

	public static Step newStep(int serialNumber, double delay, Color[][][] colors, MainController parentController) {
		String name = String.format("Step_%d", serialNumber);
		FXMLLoader loader;
		Pane pane = null;
		StepPaneController controller = null;
		try {
			loader = new FXMLLoader(Step.class.getResource("/form/StepPaneView.fxml"));
			pane = loader.load();
			controller = loader.getController();
			controller.setName(name);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return new Step(name, delay, colors, pane, controller, parentController);
	}

	public static Step newDefaultStep(int serialNumber, int GRID, MainController parentController) {
		Color[][][] colors = new Color[GRID][GRID][GRID];

		for (int i = 0; i < GRID; i++)
			for (int j = 0; j < GRID; j++)
				for (int k = 0; k < GRID; k++)
					colors[i][j][k] = Constants.defaultBoxColor;

		return newStep(serialNumber, 1, colors, parentController);
	}

	public static Step copyFromStep(Step step) {
		String name = String.format("%s_copy", step.getName());
		int GRID = step.colors.length;
		Color[][][] colors = new Color[GRID][GRID][GRID];

		for (int i = 0; i < GRID; i++)
			for (int j = 0; j < GRID; j++)
				for (int k = 0; k < GRID; k++)
					colors[i][j][k] = step.getColor(i, j, k);

		FXMLLoader loader;
		Pane pane = null;
		StepPaneController controller = null;
		try {
			loader = new FXMLLoader(Step.class.getResource("/form/StepPaneView.fxml"));
			pane = loader.load();
			controller = loader.getController();
			controller.setName(name);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return new Step(name, step.getDelay(), colors, pane, controller, step.getParentController());
	}

	public static Step extensionFromStep(Step step, int serialNumber, Color[][][] colors) {
		String name = serialNumber == 0 ? step.getName()
				: String.format("%s_extension_%d", step.getName(), serialNumber);

		FXMLLoader loader;
		Pane pane = null;
		StepPaneController controller = null;
		try {
			loader = new FXMLLoader(Step.class.getResource("/form/StepPaneView.fxml"));
			pane = loader.load();
			controller = loader.getController();
			controller.setName(name);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return new Step(name, Constants.delayTimeMinDelta, colors, pane, controller, step.getParentController());
	}

	private void addEvents(MainController parentController) {
		controller.getGoUp().setOnAction(_ -> {
			parentController.goUp(this);
		});
		controller.getGoDown().setOnAction(_ -> {
			parentController.goDown(this);
		});
		controller.getCopyStep().setOnAction(_ -> {
			parentController.copyStep(this);
		});
		controller.getDeleteStep().setOnAction(_ -> {
			parentController.deleteStep(this);
		});
		controller.getRenameStep().setOnAction(_ -> {
			parentController.renameStep(this);
		});

	}

	public final MainController getParentController() {
		return parentController;
	}

	public final Color[][][] getColors() {
		return colors;
	}

	public final Pane getPane() {
		return pane;
	}

	public final StepPaneController getController() {
		return controller;
	}

	public final String getName() {
		return controller.getName();
	}

	public final void setName(String name) {
		this.controller.setName(name);
	}

	public final double getDelay() {
		return controller.getDelayTime();
	}

	public final void setDelay(double delay) {
		this.controller.setDelayTime(delay);
	}

	public final Color getColor(int x, int y, int z) {
		return colors[x][y][z];
	}

	public final void setColor(int x, int y, int z, Color color) {
		colors[x][y][z] = color;
	}

	public final void setColor(int x, int y, int z, double red, double green, double blue) {
		colors[x][y][z] = new Color(red, green, blue, 1);
	}

	public final boolean isAllNotLighted() {
		int GRID = colors.length;

		for (int i = 0; i < GRID; i++)
			for (int j = 0; j < GRID; j++)
				for (int k = 0; k < GRID; k++)
					if (!Constants.isNotLightedColor(colors[i][j][k]))
						return false;

		return true;
	}
}
