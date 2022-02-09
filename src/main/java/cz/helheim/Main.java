package cz.helheim;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jakub Šmrha
 * @version 1.0
 * @since 1.0
 */
public class Main {
	private static final Logger L = LogManager.getLogger(Main.class);
	private static final String CONFIG_FILE = "config.yml";
	private static final String FILES_PATH = "files";

	private static final Map<String, Object> DEFAULTS = new HashMap<>() {
		{
			put("test", "abc");
			put("rofl", "lmao");
			put("foo", "bar");
		}
	};

	public static void main(String[] args) throws IOException {
		final DumperOptions options = new DumperOptions();
		options.setIndent(2);
		options.setPrettyFlow(true);
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		final Yaml yaml = new Yaml(options);

		final File f = new File(new File(
				Main.class
						.getProtectionDomain()
						.getCodeSource()
						.getLocation()
						.getPath()
		).getParentFile().getParent(), CONFIG_FILE);
		L.info("File: " + f.getAbsolutePath());
		if (!f.exists()) {
			createDefaultConfig(yaml, f);
		}

	}

	private static void createDefaultConfig(Yaml yaml, File f) throws IOException {
		if (!f.createNewFile()) {
			L.fatal("Failed to create a default config, exiting...");
			System.exit(1);
		}

		L.info(String.format("Creating default file %s", f.getAbsolutePath()));
		try (FileWriter fw = new FileWriter(f)) {
			yaml.dump(DEFAULTS, fw);
		}
	}
}
