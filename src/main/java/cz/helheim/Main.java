package cz.helheim;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;

/**
 * @author Jakub Šmrha
 * @version 1.0
 * @since 1.0
 */
public class Main {
	private static final Logger L = LogManager.getLogger(Main.class);
	private static final String CONFIG_FILE = "config.yml";
	private static final String FILES_PATH = "files";

	public static void main(String[] args) throws IOException {
		// get the default yaml parser
		final Yaml yaml = getYaml();

		// get the config and read it
		final File f = getConfig();
		if (!f.exists()) {
			if (!f.createNewFile()) {
				throw new IOException("Could not create a new file");
			}
		}
		Map<String, Double> weights = readConfig(yaml, f);

		if (weights == null) {
			weights = new LinkedHashMap<>();
		}

		// read and parse the items
		final Collection<Item> items = parseItems(readFiles(yaml, FILES_PATH, new LinkedHashMap<>()));

		final List<Item> itemsRanked = new ArrayList<>();

		for (Item item : items) {
			double minRank = 0;
			double avgRank = 0;
			double maxRank = 0;

			for (Attribute attr : item.getAttributes()) {
				final String attrStr = attr.getAttribute();
				if (!weights.containsKey(attrStr)) {
					weights.put(attrStr, 1d);
				}
				try {
					final double w = weights.get(attrStr);
					minRank += w * attr.getMin();
					avgRank += w * ((attr.getMax() + attr.getMin()) / 2d);
					maxRank += w * attr.getMin();
				} catch (Exception e) {
					L.error(String.format("Invalid line '%s'", weights.get(attrStr)));
				}
			}
			if (avgRank > 0) {
				item.setMinRank(minRank);
				item.setAvgRank(avgRank);
				item.setMaxRank(maxRank);
				itemsRanked.add(item);
			}
		}
		itemsRanked.sort(Item::compareTo);

		Map<String, Item> itemRanked = new LinkedHashMap<>();
		for (Item i : itemsRanked) {
			itemRanked.put(i.getItemId(), i);
		}
		addToConfig(yaml, f, weights);
		dumpItems(yaml, "test-" + System.currentTimeMillis() + ".yml", itemRanked);
	}

	private static void dumpItems(final Yaml yaml, final String file, final Object itemsRanked)
			throws IOException {
		try (FileWriter fw = new FileWriter(file)) {
			yaml.dump(itemsRanked, fw);
		}
	}

	private static File getConfig() {
		return new File(new File(
				Main.class
						.getProtectionDomain()
						.getCodeSource()
						.getLocation()
						.getPath()
		).getParentFile().getParent(), CONFIG_FILE);
	}

	private static Collection<Item> parseItems(Map<String, ?> map) {
		final Collection<Item> items = new ArrayList<>();

		for (Map.Entry<String, ?> entry : map.entrySet()) {
			Collection<Attribute> attributes = new ArrayList<>();
			if (!(entry.getValue() instanceof Map)) {
				continue;
			}
			Map<String, ?> values = (Map<String, ?>) entry.getValue();
			if (!(values.containsKey("lore") && values.get("lore") instanceof List)) {
				continue;
			}
			List<String> lore = (List<String>) values.get("lore");
			for (String s : lore) {
				if (s == null || s.isEmpty()) {
					continue;
				}
				String attribute = "";
				int min = 0;
				int max = 0;
				Matcher attribMatcher = Attribute.ATTRIBUTE_PATTERN.matcher(s);
				Matcher classAttribMatcher = Attribute.CLASS_PATTERN.matcher(s);
				if (attribMatcher.find()) {
					min = Integer.parseInt(attribMatcher.group(1));
					try {
						max = Integer.parseInt(attribMatcher.group(3));
					} catch (NumberFormatException e) {
						max = min;
					}
					attribute = attribMatcher.group(4);
				} else if (classAttribMatcher.find()) {
					attribute = classAttribMatcher.group(1);
					min = Integer.parseInt(classAttribMatcher.group(2));
					try {
						max = Integer.parseInt(classAttribMatcher.group(4));
					} catch (NumberFormatException e) {
						max = min;
					}
				} else {
					continue;
				}
				attributes.add(new Attribute(attribute, min, max));
			}

			items.add(new Item(entry.getKey(), attributes));
		}

		return items;
	}

	private static Map<String, ?> readFiles(final Yaml yaml, final String filesPath, final Map<String, ?> map)
			throws FileNotFoundException {
		final File dir = new File(filesPath);
		File[] files = dir.listFiles(x -> x.getName().endsWith(".yml"));
		if (files == null) {
			dir.mkdir();
			return map;
		}

		for (File f : files) {
			if (f.isDirectory()) {
				readFiles(yaml, f.getAbsolutePath(), map);
			} else {
				map.putAll(yaml.load(new FileInputStream(f)));
			}
		}
		return map;
	}

	private static Map<String, Double> readConfig(final Yaml yaml, final File f) throws FileNotFoundException {
		return yaml.load(new FileInputStream(f));
	}

	private static Yaml getYaml() {
		final DumperOptions options = new DumperOptions();
		options.setIndent(2);
		options.setPrettyFlow(true);
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		return new Yaml(options);
	}

	private static void addToConfig(Yaml yaml, File f, Map<String, Double> map) throws IOException {
		try (FileWriter fw = new FileWriter(f)) {
			yaml.dump(map, fw);
		}
	}
}
