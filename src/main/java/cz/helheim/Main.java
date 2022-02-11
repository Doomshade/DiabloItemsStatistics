package cz.helheim;

import cz.helheim.items.ItemStatistics;
import cz.helheim.mobs.MobStatistics;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.svg.SVGGraphics2D;
import org.jfree.svg.SVGUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

/**
 * The entry point
 *
 * @author Jakub Šmrha
 * @version 1.0
 * @since 1.0
 */
public class Main {
	private static final String OUT_PATH = "out";
	private static final int WIDTH = 1920;
	private static final int HEIGHT = 1080;
	private static final Yaml yaml;

	// set up YAML
	static {
		final DumperOptions options = new DumperOptions();
		options.setIndent(2);
		options.setPrettyFlow(true);
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		yaml = new Yaml(options);
	}

	public static void main(String[] args) throws IOException {
		ItemStatistics.parseItems();
		MobStatistics.parseMobs();
	}

	public static File getOutDir() {
		File outDir = new File(getCwd(), OUT_PATH);
		if (!outDir.isDirectory()) {
			outDir.mkdir();
		}
		return outDir;
	}

	/**
	 * Gets the current working directory, which is actually the parent directory of this jar
	 * as this jar is located in folder/bin
	 *
	 * @return the current working directory
	 */
	public static File getCwd() {
		return new File(new File(
				Main.class
						.getProtectionDomain()
						.getCodeSource()
						.getLocation()
						.getPath()
		).getParentFile().getParent());
	}

	/**
	 * Reads data from the files with the given file path
	 *
	 * @param filesPath the file path
	 * @param map       the map to write the data to
	 * @return the data
	 * @throws FileNotFoundException if the file path is invalid
	 */
	public static Map<String, ?> readFiles(final String filesPath, final Map<String, ?> map)
			throws FileNotFoundException {
		final File dir = new File(filesPath);
		File[] files = dir.listFiles(x -> x.getName().endsWith(".yml"));
		if (files == null) {
			dir.mkdir();
			return map;
		}

		for (File f : files) {
			if (f.isDirectory()) {
				readFiles(f.getAbsolutePath(), map);
			} else {
				map.putAll(yaml.load(new FileInputStream(f)));
			}
		}
		return map;
	}

	public static <T> T readFile(final File f) throws FileNotFoundException {
		return readFile(f, null);
	}

	public static <T> T readFile(final File f, T defaultValue) throws FileNotFoundException {
		final T read = yaml.load(new FileInputStream(f));
		return read == null ? defaultValue : read;
	}

	public static File getDirectory(File parent, String fileName) {
		File f = new File(parent, fileName);
		if (!f.isDirectory()) {
			f.mkdir();
		}
		return f;
	}

	public static Yaml getYaml() {
		return yaml;
	}

	/**
	 * Dumps a chart into a file in .svg format
	 *
	 * @param fileName the file name
	 * @param chart    the chart
	 * @throws IOException if the chart could not be exported
	 */
	public static void dumpChart(String fileName, JFreeChart chart) throws IOException {
		XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) chart.getXYPlot().getRenderer();
		renderer.setDefaultItemLabelsVisible(true);
		renderer.setDefaultItemLabelGenerator(new StandardXYItemLabelGenerator("{2}"));

		File file = new File(getOutDir(), fileName);

		SVGGraphics2D g2 = new SVGGraphics2D(WIDTH, HEIGHT);
		Rectangle r = new Rectangle(0, 0, WIDTH, HEIGHT);
		chart.draw(g2, r);
		SVGUtils.writeToSVG(file, g2.getSVGElement());
	}
}
