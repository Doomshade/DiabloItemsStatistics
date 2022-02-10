package cz.helheim;

import cz.helheim.items.ItemStatistics;
import cz.helheim.mobs.MobStatistics;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

/**
 * @author Jakub Šmrha
 * @version 1.0
 * @since 1.0
 */
public class Main {
	public static final String OUT_PATH = "out";
	public static final int WIDTH = 1920;
	public static final int HEIGHT = 1080;
	private static final Yaml yaml;

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

	public static File getCwd() {
		return new File(new File(
				Main.class
						.getProtectionDomain()
						.getCodeSource()
						.getLocation()
						.getPath()
		).getParentFile().getParent());
	}

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
}
