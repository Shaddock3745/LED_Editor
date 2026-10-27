package customObject;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.paint.Color;

public final class StepInterpolator {

	private StepInterpolator() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	private static class Node {
		final int x, y, z;
		final Color color;

		Node(int x, int y, int z, Color color) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.color = color;
		}
	}

	private static class HueChromaNode {
		double hueX, hueY, chromas, weights;
		static final double cutoff = 0.05;

		HueChromaNode() {
			clear();
		}

		void addColor(double hue, double chroma, double weight) {
			if (!Double.isFinite(weight) || weight <= 0)
				return;

			if (!Double.isFinite(hue) || !Double.isFinite(chroma) || chroma < 0)
				return;

			double hueWeight = chroma * weight;
			if (hueWeight > 0) {
				double heuRadius = Math.toRadians(hue);
				hueX += Math.cos(heuRadius) * hueWeight;
				hueY += Math.sin(heuRadius) * hueWeight;
			}

			chromas += chroma * weight;
			weights += weight;
		}

		void clear() {
			hueX = hueY = chromas = weights = 0;
		}

		Color getColor() {
			if (weights < cutoff)
				return Constants.defaultBoxColor;

			double hueDeg;
			if (Math.abs(hueX) < 1e-12 && Math.abs(hueY) < 1e-12) {
				hueDeg = 0.0;
			} else {
				hueDeg = Math.toDegrees(Math.atan2(hueY, hueX));
				if (hueDeg < 0)
					hueDeg += 360.0;
			}

			double chroma = chromas / weights;
			if (!Double.isFinite(chroma))
				chroma = 0;
			if (chroma < 0)
				chroma = 0;
			else if (chroma > 1)
				chroma = 1;

			return Color.hsb(hueDeg, chroma, 1.0);
		}
	}

	private static double shortestDeltaDeg(double a, double b) {
		double d = b - a;
		d = (d + 540.0) % 360.0 - 180.0;
		return d;
	}

	private static class Line {
		final int startX, startY, startZ;
		final double deltaX, deltaY, deltaZ, startHue, startChroma, deltaHue, deltaChroma;
		static final double EPS = 1e-6;

		Line(int startX, int startY, int startZ, Color startColor, int endX, int endY, int endZ, Color endColor) {
			this.startX = startX;
			this.startY = startY;
			this.startZ = startZ;
			deltaX = endX - startX;
			deltaY = endY - startY;
			deltaZ = endZ - startZ;

			double startHue = Constants.getHue(startColor);
			double endHue = Constants.getHue(endColor);
			double startChroma = Constants.getChroma(startColor);
			double endChroma = Constants.getChroma(endColor);

			if (startChroma < EPS && endChroma >= EPS)
				startHue = endHue;
			else if (endChroma < EPS && startChroma >= EPS)
				endHue = startHue;

			this.startHue = startHue;
			this.startChroma = startChroma;
			deltaHue = shortestDeltaDeg(startHue, endHue);
			deltaChroma = endChroma - startChroma;
		}

		void splat(HueChromaNode[][][] hcNodes, int totalFrame, int currentFrame) {
			double t = (double) currentFrame / totalFrame;
			double x = startX + deltaX * t, y = startY + deltaY * t, z = startZ + deltaZ * t;
			double hue = startHue + deltaHue * t;
			hue = ((hue % 360) + 360) % 360;
			double chroma = startChroma + deltaChroma * t;

			if (chroma < 0)
				chroma = 0;
			else if (chroma > 1)
				chroma = 1;

			int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y), z0 = (int) Math.floor(z);
			int x1 = x0 + 1, y1 = y0 + 1, z1 = z0 + 1;

			double dx = x - x0, dy = y - y0, dz = z - z0;

			int nx = hcNodes.length, ny = nx, nz = nx;

			for (int i = 0; i < 2; i++) {
				int xi = (i == 0 ? x0 : x1);

				if (xi < 0 || xi >= nx)
					continue;

				double wx = i == 0 ? 1 - dx : dx;

				for (int j = 0; j < 2; j++) {
					int yi = (j == 0 ? y0 : y1);

					if (yi < 0 || yi >= ny)
						continue;

					double wy = j == 0 ? 1 - dy : dy;

					for (int k = 0; k < 2; k++) {
						int zi = k == 0 ? z0 : z1;

						if (zi < 0 || zi >= nz)
							continue;

						double w = wx * wy * (k == 0 ? 1 - dz : dz);

						if (w > 0)
							hcNodes[xi][yi][zi].addColor(hue, chroma, w);
					}
				}
			}
		}
	}

	private static int distance(Node a, Node b) {
		int dx = a.x - b.x, dy = a.y - b.y, dz = a.z - b.z;
		return dx * dx + dy * dy + dz * dz;
	}

	private static List<Node> collectLegalNodes(Color[][][] colors) {
		int GRID = colors.length;
		List<Node> result = new ArrayList<>();

		for (int x = 0; x < GRID; x++)
			for (int y = 0; y < GRID; y++)
				for (int z = 0; z < GRID; z++)
					if (!Constants.isNotLightedColor(colors[x][y][z]))
						result.add(new Node(x, y, z, colors[x][y][z]));

		return result;
	}

	private static int closestNodeIndex(Node node, List<Node> list) {
		int resultIndex = -1, resultDistance = Integer.MAX_VALUE;

		for (int i = 0; i < list.size(); i++) {
			int dis = distance(node, list.get(i));

			if (dis < resultDistance) {
				resultIndex = i;
				resultDistance = dis;
			}
		}

		return resultIndex;
	}

	private static List<Line> getLinesBetweenTwoMatrix(Color[][][] start, Color[][][] end) {
		List<Line> result = new ArrayList<>();
		List<Node> startNodes = collectLegalNodes(start), endNodes = collectLegalNodes(end);

		boolean[] coveredEndNodes = new boolean[endNodes.size()];

		for (Node node : startNodes) {
			int index = closestNodeIndex(node, endNodes);
			Node target = endNodes.get(index);
			coveredEndNodes[index] = true;
			result.add(new Line(node.x, node.y, node.z, node.color, target.x, target.y, target.z, target.color));
		}

		for (int i = 0; i < coveredEndNodes.length; i++)
			if (!coveredEndNodes[i]) {
				Node target = endNodes.get(i);
				int index = closestNodeIndex(target, startNodes);
				Node node = startNodes.get(index);
				result.add(new Line(node.x, node.y, node.z, node.color, target.x, target.y, target.z, target.color));
			}

		return result;
	}

	private static List<Step> extensionBetweenTwoSteps(Step start, Step end) {
		List<Step> result = new ArrayList<>();
		Step startCopy = Step.extensionFromStep(start, 0, start.getColors());
		result.add(startCopy);
		int totalFrame = (int) Math.round((start.getDelay() / Constants.delayTimeMinDelta));

		if (totalFrame > 1) {
			int GRID = start.getColors().length;
			List<Line> lines = getLinesBetweenTwoMatrix(start.getColors(), end.getColors());
			HueChromaNode[][][] nodes = new HueChromaNode[GRID][GRID][GRID];

			for (int x = 0; x < GRID; x++)
				for (int y = 0; y < GRID; y++)
					for (int z = 0; z < GRID; z++)
						nodes[x][y][z] = new HueChromaNode();

			for (int i = 1; i < totalFrame; i++) {

				for (int x = 0; x < GRID; x++)
					for (int y = 0; y < GRID; y++)
						for (int z = 0; z < GRID; z++)
							nodes[x][y][z].clear();

				for (Line line : lines)
					line.splat(nodes, totalFrame, i);

				Color[][][] colors = new Color[GRID][GRID][GRID];

				for (int x = 0; x < GRID; x++)
					for (int y = 0; y < GRID; y++)
						for (int z = 0; z < GRID; z++)
							colors[x][y][z] = nodes[x][y][z].getColor();

				Step step = Step.extensionFromStep(start, i, colors);
				result.add(step);
			}
		}

		return result;
	}

	public static List<Step> extension(List<Step> originalSteps) {
		List<Step> result = new ArrayList<>();

		for (int i = 0; i < originalSteps.size() - 1; i++)
			result.addAll(extensionBetweenTwoSteps(originalSteps.get(i), originalSteps.get(i + 1)));

		result.addAll(extensionBetweenTwoSteps(originalSteps.getLast(), originalSteps.getFirst()));

		return result;
	}
}
