package cz.helheim;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.statistics.Regression;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
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
	public static final String MIN = "min";
	public static final String AVG = "avg";
	public static final String MAX = "max";
	private static final Logger L = LogManager.getLogger(Main.class);
	private static final String CONFIG_FILE = "config.yml";
	private static final String BLACKLIST = "blacklist.yml";
	private static final String FILES_PATH = "files";

	private static final int THRESHOLD = 5;

	public static void main(String[] args) throws IOException {
		// get the default yaml parser
		final Yaml yaml = getYaml();

		// get the config and read it
		final File config = getConfig();
		if (!config.exists()) {
			if (!config.createNewFile()) {
				throw new IOException("Could not create a new file");
			}
		}
		Map<String, Double> weights = readFile(yaml, config);

		if (weights == null) {
			weights = new LinkedHashMap<>();
		}

		final File filter = getFilter();
		if (!filter.exists()) {
			filter.createNewFile();
		}
		Map<String, Object> blacklist = readFile(yaml, filter);

		// read and parse the items
		final Collection<Item> items = parseItems(readFiles(yaml, FILES_PATH, new LinkedHashMap<>()),
				(Collection<String>) blacklist.getOrDefault("blacklist", new ArrayList<>()));

		final List<Item> avgRanks = new ArrayList<>();

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
					maxRank += w * attr.getMax();
				} catch (Exception e) {
					L.error(String.format("Invalid line '%s'", weights.get(attrStr)));
				}
			}
			if (avgRank > 0) {
				item.setMinRank(minRank);
				item.setAvgRank(avgRank);
				item.setMaxRank(maxRank);
				avgRanks.add(item);
			}
		}
		addToConfig(yaml, config, weights);
		final List<Item> minRanks = new ArrayList<>(avgRanks);
		final List<Item> maxRanks = new ArrayList<>(avgRanks);

		minRanks.sort(Comparator.comparingInt(Item::getLvl));
		avgRanks.sort(Comparator.comparingInt(Item::getLvl));
		maxRanks.sort(Comparator.comparingInt(Item::getLvl));

		Map<String, Map<String, Item>> itemRanked = new LinkedHashMap<>();
		for (Item i : minRanks) {
			Map<String, Item> map = itemRanked.getOrDefault(MIN, new LinkedHashMap<>());
			map.put(i.getItemId(), i);
			itemRanked.putIfAbsent(MIN, map);
		}
		for (Item i : avgRanks) {
			Map<String, Item> map = itemRanked.getOrDefault(AVG, new LinkedHashMap<>());
			map.put(i.getItemId(), i);
			itemRanked.putIfAbsent(AVG, map);
		}
		//dumpItems(yaml, "test-" + System.currentTimeMillis() + ".yml", itemRanked);
		for (Item i : maxRanks) {
			Map<String, Item> map = itemRanked.getOrDefault(MAX, new LinkedHashMap<>());
			map.put(i.getItemId(), i);
			itemRanked.putIfAbsent(MAX, map);
		}
		dumpItems(yaml, "test-" + System.currentTimeMillis() + ".yml", itemRanked.get(AVG));
		createChart("avg-only.jpeg", itemRanked);
	}

	private static File getFilter() {
		return new File(getCwd(), BLACKLIST);
	}

	private static void createChart(final String fileName, final Map<String, Map<String, Item>> itemRanked)
			throws IOException {
		XYSeriesCollection ds = new XYSeriesCollection();

		for (Map.Entry<String, Map<String, Item>> eentry : itemRanked.entrySet()) {
			final XYSeries changePerLevelSeries = new XYSeries(eentry.getKey() + "%");
			final XYSeries series = new XYSeries(eentry.getKey());
			Map<Integer, Number> vals = new HashMap<>();
			Map<Integer, Integer> itemsPerLvl = new HashMap<>();

			for (Map.Entry<String, Item> entry : eentry.getValue().entrySet()) {
				Item item = entry.getValue();
				Number value = 0d;
				switch (eentry.getKey()) {
					case MIN:
						value = item.getMinRank();
						break;
					case AVG:
						value = item.getAvgRank();
						break;
					case MAX:
						value = item.getMaxRank();
						break;
				}
//				ds.addSeries(series);
//				ds.addValue(value, eentry.getKey(), id++);
				vals.put(item.getLvl(), vals.getOrDefault(item.getLvl(), 0d).doubleValue() + value.doubleValue());
				itemsPerLvl.put(item.getLvl(), itemsPerLvl.getOrDefault(item.getLvl(), 0) + 1);
			}
			Map.Entry<Integer, Number> prev = null;
			for (Map.Entry<Integer, Number> entry : vals.entrySet()) {
				final int amount = itemsPerLvl.getOrDefault(entry.getKey(), Integer.MAX_VALUE);
				if (amount >= THRESHOLD) {
					final Number curVal = (entry.getValue().doubleValue() /
							amount);
					series.add(entry.getKey(), curVal);
					if (prev != null) {
						final double prevVal = prev.getValue().doubleValue() /
								itemsPerLvl.getOrDefault(prev.getKey(), Integer.MAX_VALUE);
						double v = (100 * (curVal.doubleValue() / prevVal)) - 100;
						changePerLevelSeries.add(entry.getKey(), (Number) v);
					}
					prev = entry;
				}

			}
			ds.addSeries(series);
			ds.addSeries(changePerLevelSeries);
		}
		XYSeries avgRegression = new XYSeries("avg%-reg");
		final double[] vals = Regression.getOLSRegression(ds, 3);
		for (int i = 0; i < 35; i++) {
			avgRegression.add(i, vals[0] + vals[1] * i);
		}
		ds.addSeries(avgRegression);


		final JFreeChart lineChartObject = ChartFactory.createXYLineChart("foo", "bar", "foobar", ds);
		File file = new File(getCwd(), fileName);
		ChartUtils.saveChartAsJPEG(file, lineChartObject, 1920, 1080);
	}

	private static void dumpItems(final Yaml yaml, final String file, final Object itemsRanked)
			throws IOException {
		try (FileWriter fw = new FileWriter(file)) {
			yaml.dump(itemsRanked, fw);
		}
	}

	private static File getConfig() {
		return new File(getCwd(), CONFIG_FILE);
	}

	private static String getCwd() {
		return new File(
				Main.class
						.getProtectionDomain()
						.getCodeSource()
						.getLocation()
						.getPath()
		).getParentFile().getParent();
	}

	private static Collection<Item> parseItems(Map<String, ?> map,
	                                           final Collection<String> blacklist) {
		final Collection<Item> items = new ArrayList<>();

		loop:
		for (Map.Entry<String, ?> entry : map.entrySet()) {
			final String itemId = entry.getKey();
			for (String s : blacklist) {
				if (itemId.toLowerCase().contains(s.toLowerCase())) {
					L.info("Filtered " + itemId);
					continue loop;
				}
			}

			Collection<Attribute> attributes = new ArrayList<>();
			if (!(entry.getValue() instanceof Map)) {
				continue;
			}
			Map<String, ?> values = (Map<String, ?>) entry.getValue();
			if (!(values.containsKey("lore") && values.get("lore") instanceof List)) {
				continue;
			}
			List<String> lore = (List<String>) values.get("lore");
			int lvl = 0;
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
				if (attribute.equalsIgnoreCase("Potřebný Lvl")) {
					lvl = min;
				}
				attributes.add(new Attribute(attribute, min, max));
			}

			items.add(new Item(itemId, attributes, lvl));
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

	private static <T> T readFile(final Yaml yaml, final File f) throws FileNotFoundException {
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
