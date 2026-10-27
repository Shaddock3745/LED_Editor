package customObject;

import javafx.scene.paint.Color;

public final class Constants {

	public static int maxStepNumber = 1500;
	public static double defaultBoxColorValue = 0.25, minDelayTimePerStep = 0.1, maxDelayTimePerStep = 10,
			delayTimeMinDelta = 0.1, maxTotalDelayTime = maxStepNumber / (1.0 / delayTimeMinDelta);
	public static javafx.scene.paint.Color defaultBoxColor = new javafx.scene.paint.Color(defaultBoxColorValue,
			defaultBoxColorValue, defaultBoxColorValue, 1), outlineColor = javafx.scene.paint.Color.LIGHTGRAY;

	public static boolean isNotLightedColor(Color color) {
		return color.getBrightness() <= Constants.defaultBoxColorValue;
	}

	public static double getHue(Color color) {
		double red = color.getRed(), green = color.getGreen(), blue = color.getBlue();
		double max = Math.max(red, Math.max(green, blue)), min = Math.min(red, Math.min(green, blue));
		double chroma = max - min, hue;

		if (chroma == 0)
			hue = 0;
		else if (max == red)
			hue = 60 * (((green - blue) / chroma) % 6);
		else if (max == green)
			hue = 60 * (((blue - red) / chroma) + 2);
		else
			hue = 60 * (((red - green) / chroma) + 4);

		if (hue < 0)
			hue += 360;

		return hue;
	}

	public static double getChroma(Color color) {
		double red = color.getRed(), green = color.getGreen(), blue = color.getBlue();
		double max = Math.max(red, Math.max(green, blue)), min = Math.min(red, Math.min(green, blue));

		return max - min;
	}

	private Constants() {
		throw new UnsupportedOperationException("This is a constants class and cannot be instantiated");
	}
}
