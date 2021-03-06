package cz.helheim.items;


import cz.helheim.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;

/**
 * The "main" of item statistics
 *
 * @author Jakub Šmrha
 * @version 1.0
 * @since 1.0
 */
public class ItemStatistics {
	private static final String MIN = "min";
	private static final String AVG = "avg";
	private static final String MAX = "max";
	private static final String CONFIG_FILE = "items-config.yml";
	private static final Logger L = LogManager.getLogger(ItemStatistics.class);
	private static final String BLACKLIST = "items-blacklist.yml";
	private static final String ITEMS_PATH = "items";

	private static final int THRESHOLD = 5;

	/**
	 * The "main"
	 *
	 * @throws IOException if an IO based operation failed
	 */
	public static void parseItems() throws IOException {
		// read config files
		final Map<String, Double> weights = Main.readFile(getConfig(), new LinkedHashMap<>());
		final Map<String, Object> blacklist = Main.readFile(getFilter(), new LinkedHashMap<>());

		// read and parse the items
		final Collection<Item> items = parseItems(
				Main.readFiles(ITEMS_PATH, new LinkedHashMap<>()),
				(Collection<String>) blacklist.getOrDefault("blacklist", new ArrayList<>()));

		// rank the items
		final List<Item> avgRanks = new ArrayList<>();
		for (Item item : items) {
			double minRank = 0;
			double avgRank = 0;
			double maxRank = 0;

			// count the weight based on the attributes
			for (Attribute attr : item.getAttributes()) {
				final String attrStr = attr.getAttribute();

				// add an unknown attribute to the map
				weights.putIfAbsent(attrStr, 1d);
				try {
					final double w = weights.get(attrStr);
					minRank += w * attr.getMin();
					avgRank += w * ((attr.getMax() + attr.getMin()) / 2d);
					maxRank += w * attr.getMax();
				} catch (Exception e) {
					L.error(String.format("Invalid line '%s'", weights.get(attrStr)));
				}
			}

			// if the item had no attributes, skip it
			if (avgRank > 0) {
				item.setMinRank(minRank);
				item.setAvgRank(avgRank);
				item.setMaxRank(maxRank);
				avgRanks.add(item);
			}
		}

		// add additional new weights to the config
		addToConfig(getConfig(), weights);

		// get min/avg/max ranks and sort it by level
		final List<Item> minRanks = new ArrayList<>(avgRanks);
		final List<Item> maxRanks = new ArrayList<>(avgRanks);

		// sort them by level
		minRanks.sort(Comparator.comparingInt(Item::getLvl));
		avgRanks.sort(Comparator.comparingInt(Item::getLvl));
		maxRanks.sort(Comparator.comparingInt(Item::getLvl));

		final Map<String, Map<String, Item>> itemRanked = new LinkedHashMap<>();
		populateMap(minRanks, MIN, itemRanked);
		populateMap(avgRanks, AVG, itemRanked);
		populateMap(maxRanks, MAX, itemRanked);

		// dump it all into files
		dumpItems("items-" + System.currentTimeMillis() + ".yml", itemRanked.get(AVG));
		createChart("avg-only.svg", itemRanked);
	}

	private static void populateMap(Collection<Item> items, String key, Map<String, Map<String, Item>> itemRanked) {
		for (Item i : items) {
			Map<String, Item> map = itemRanked.getOrDefault(key, new LinkedHashMap<>());
			map.put(i.getItemId(), i);
			itemRanked.putIfAbsent(key, map);
		}
	}

	private static File getFilter() throws IOException {
		final File f = new File(Main.getCwd(), BLACKLIST);
		if (!f.exists()) {
			f.createNewFile();
		}
		return f;
	}

	/**
	 * Creates a line chart
	 *
	 * @param fileName   the file name to dump it to
	 * @param itemRanked the data, the key is "min"/"avg"/"max", the underlying map - key = item ID, value = item
	 * @throws IOException if the chart could not be created
	 */
	private static void createChart(final String fileName, final Map<String, Map<String, Item>> itemRanked)
			throws IOException {
		if (itemRanked.isEmpty()) {
			L.info("No items loaded, cannot create item charts");
			return;
		}

		XYSeriesCollection ds = new XYSeriesCollection();
		for (Map.Entry<String, Map<String, Item>> e : itemRanked.entrySet()) {
			final XYSeries series = new XYSeries(e.getKey());
			Map<Integer, Number> vals = new HashMap<>();
			Map<Integer, Integer> itemsPerLvl = new HashMap<>();

			for (Map.Entry<String, Item> entry : e.getValue().entrySet()) {
				Item item = entry.getValue();
				Number value = 0d;

				// this is pretty awful, but well. who cares
				switch (e.getKey()) {
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

				// store the value and amount of items
				vals.put(item.getLvl(), vals.getOrDefault(item.getLvl(), 0d).doubleValue() + value.doubleValue());
				itemsPerLvl.put(item.getLvl(), itemsPerLvl.getOrDefault(item.getLvl(), 0) + 1);
			}

			// iterate through values and check if there are enough items
			// see THRESHOLD
			for (Map.Entry<Integer, Number> entry : vals.entrySet()) {
				final int amount = itemsPerLvl.getOrDefault(entry.getKey(), Integer.MAX_VALUE);
				if (amount >= THRESHOLD) {
					final Number curVal = (entry.getValue().doubleValue() / amount);
					series.add(entry.getKey(), curVal);
				}
			}
			ds.addSeries(series);
		}

		// not series found means no items -> don't dump it
		if (ds.getSeriesCount() < 1) {
			L.info("No items loaded, cannot create item charts");
			return;
		}

		final XYSeries avgSeries = ds.getSeries(1);
		final XYSeries powRegression = new XYSeries("avg%");
		for (int i = 1; i < avgSeries.getItemCount(); i++) {
			final Number prev = avgSeries.getY(i - 1);

			final Number curr = avgSeries.getY(i);
			powRegression.add(avgSeries.getX(i), (curr.doubleValue() / prev.doubleValue()) * 100 - 100);
		}
		powRegression.add(avgSeries.getX(0), 0);

		ds.addSeries(powRegression);

		final JFreeChart chart = ChartFactory.createXYLineChart("Item stats / level chart", "Level requirement",
				"Item stats", ds);
		Main.dumpChart(fileName, chart);
	}

	private static void dumpItems(final String file, final Object itemsRanked)
			throws IOException {
		try (FileWriter fw = new FileWriter(new File(Main.getOutDir(), file), StandardCharsets.UTF_8)) {
			Main.getYaml().dump(itemsRanked, fw);
		}
	}

	private static File getConfig() throws IOException {
		final File f = new File(Main.getCwd(), CONFIG_FILE);
		if (!f.exists()) {
			f.createNewFile();
		}
		return f;
	}

	private static Collection<Item> parseItems(Map<String, ?> map,
	                                           final Collection<String> blacklist) {
		final Collection<Item> items = new ArrayList<>();

		loop:
		for (Map.Entry<String, ?> entry : map.entrySet()) {
			final String itemId = entry.getKey();

			// first check if the item is in the blacklist
			// it contains a set of blacklisted prefixes
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
				final Matcher attribMatcher = Attribute.ATTRIBUTE_PATTERN.matcher(s);
				final Matcher classAttribMatcher = Attribute.CLASS_PATTERN.matcher(s);

				// check for both attribute matchers
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

				// the attribute is level requirement, set the level to it
				if (attribute.equalsIgnoreCase("Potřebný Lvl")) {
					lvl = min;
				}
				attributes.add(new Attribute(attribute, min, max));
			}

			// add a new attribute once the attributes and lvl is parsed
			items.add(new Item(itemId, attributes, lvl));
		}

		return items;
	}

	private static void addToConfig(File f, Map<String, Double> map) throws IOException {
		try (FileWriter fw = new FileWriter(f, StandardCharsets.UTF_8)) {
			Main.getYaml().dump(map, fw);
		}
	}

}
