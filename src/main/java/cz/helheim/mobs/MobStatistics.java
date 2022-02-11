package cz.helheim.mobs;

import cz.helheim.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.svg.SVGGraphics2D;
import org.jfree.svg.SVGUtils;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;

/**
 * @author Jakub Šmrha
 * @version 1.0
 * @since 1.0
 */
public class MobStatistics {
	private static final String MOBS_PATH = "mobs";
	private static final String ITEMS_PATH = "items";
	private static final String ACTUAL_MOBS_PATH = "actual-mobs";
	private static final String CONFIG_FILE = "mob-config.yml";
	private static final Logger L = LogManager.getLogger(MobStatistics.class);

	public static void parseMobs() throws IOException {
		// get weights from config
		final Map<String, Double> weights = Main.readFile(getConfig(), new LinkedHashMap<>());

		// parse and sort mobs by level
		final List<Mob> mobs = parseMobFiles(parseItemFiles(weights));
		mobs.sort(Mob::compareTo);

		// dump it to a map
		final Map<String, Mob> dumpMap = new LinkedHashMap<>();
		for (Mob m : mobs) {
			for (final Equipment eq : m.getEquipment()) {
				// if the weight does not exist, add it to the map with weight of 1
				// the new weight will later be written to the config file
				weights.putIfAbsent(String.valueOf(eq.getItemId()), 1d);
				for (final Enchantment ench : eq.getEnchantments()) {
					weights.putIfAbsent(ench.getEnchantmentId(), 1d);
				}
			}
			dumpMap.put(m.getMobId(), m);
		}

		// write the new weights to the config
		addToConfig(getConfig(), weights);

		// dump the mobs to a file
		Main.getYaml().dump(dumpMap, new FileWriter(new File(Main.getOutDir(),
				"mobs-" + System.currentTimeMillis() + ".yml"), StandardCharsets.UTF_8));

		// create a chart out of the file
		createChart("mobs.svg", dumpMap);
	}

	/**
	 * Dumps the weight map to a config file
	 *
	 * @param f   the config file
	 * @param map the weight map
	 * @throws IOException if the file is invalid
	 */
	private static void addToConfig(File f, Map<String, Double> map) throws IOException {
		try (FileWriter fw = new FileWriter(f, StandardCharsets.UTF_8)) {
			Main.getYaml().dump(map, fw);
		}
	}

	/**
	 * Parses items and associates the weights with them based on the given map
	 *
	 * @param weights the weight map
	 * @return a map of key=equipmentId, value=equipment
	 * @throws FileNotFoundException if a file could not be read
	 */
	private static Map<String, Equipment> parseItemFiles(
			final Map<String, Double> weights) throws FileNotFoundException {
		final Map<String, Equipment> equipment = new HashMap<>();

		// get all files in the directory
		File[] itemFiles = getItemsFolder().listFiles(x -> x.getName().endsWith(".yml"));
		if (itemFiles == null) {
			return equipment;
		}

		// read all files in that directory
		for (File itemFile : itemFiles) {
			// key = itemId, value = map of key-value pairs (metadata)
			final Map<String, ?> map = Main.readFile(itemFile);
			for (Map.Entry<String, ?> entry : map.entrySet()) {
				Map<String, ?> subMap = (Map<String, ?>) entry.getValue();

				// item id
				final Object id = subMap.get("Id");
				if (!(id instanceof Integer)) {
					continue;
				}
				final Object enchantments = subMap.get("Enchantments");
				final Collection<Enchantment> enchs = new ArrayList<>();
				if (enchantments instanceof List) {
					for (String s : (List<String>) enchantments) {
						Matcher m = Enchantment.ENCHANTMENT_PATTERN.matcher(s);
						if (!m.find()) {
							L.debug("Could not find an enchantment pattern in " + s);
							continue;
						}
						// TODO add weighting
						enchs.add(new Enchantment(m.group(1), Integer.parseInt(m.group(2)),
								weights.getOrDefault(m.group(1), 0d)));
					}
				}

				// TODO add weighting
				equipment.put(entry.getKey(), new Equipment(entry.getKey(), (int) id,
						weights.getOrDefault(String.valueOf(id), 0d),
						enchs));
			}

		}
		return equipment;
	}

	private static File getItemsFolder() {
		return Main.getDirectory(getMobsFolder(), ITEMS_PATH);
	}

	private static List<Mob> parseMobFiles(Map<String, Equipment> equipment) throws FileNotFoundException {
		List<Mob> mobs = new ArrayList<>();
		File[] mobFiles = getActualMobsFolder().listFiles(x -> x.getName().endsWith(".yml"));
		if (mobFiles == null) {
			return mobs;
		}
		for (File mobFile : mobFiles) {
			Map<String, ?> map = Main.readFile(mobFile);
			for (Map.Entry<String, ?> entry : map.entrySet()) {
				final Map<String, ?> subMap = (Map<String, ?>) entry.getValue();
				final Collection<Equipment> eq = new ArrayList<>();
				if (subMap.containsKey("Equipment") && subMap.get("Equipment") instanceof Collection) {
					for (String s : (Collection<String>) subMap.get("Equipment")) {
						final Matcher m = Equipment.EQUIPMENT_PATTERN.matcher(s);
						if (!m.find()) {
							L.debug(String.format("Could not find an item for %s (invalid pattern for mob id %s)", s,
									entry.getKey()));
							continue;
						}

						final String eqId = m.group(1);
						if (!equipment.containsKey(eqId)) {
							L.debug(String.format("Could not find an item for equipment ID %s for mob %s", eqId, entry.getKey()));
							continue;
						}
						eq.add(equipment.get(eqId));
					}
				}

				if (!subMap.containsKey("Health") || !subMap.containsKey("Damage")) {
					continue;
				}
				final int health = (int) subMap.get("Health");
				final int damage = (int) subMap.get("Damage");

				final String display;
				if (subMap.containsKey("Display")) {
					display = (String) subMap.get("Display");
				} else {
					display = "No mob name";
				}

				mobs.add(new Mob(entry.getKey(), display, health, damage, eq));
			}
		}
		return mobs;
	}

	private static File getActualMobsFolder() {
		return Main.getDirectory(getMobsFolder(), ACTUAL_MOBS_PATH);
	}

	private static File getMobsFolder() {
		return Main.getDirectory(Main.getCwd(), MOBS_PATH);
	}

	private static void createChart(final String fileName, final Map<String, Mob> rankedMobs) throws IOException {
		if (rankedMobs.isEmpty()) {
			L.info("No mobs loaded, cannot create mob charts");
			return;
		}
		XYSeriesCollection ds = new XYSeriesCollection();
		Map<Integer, Number> vals = new HashMap<>();
		Map<Integer, Integer> mobsPerLvl = new HashMap<>();
		final XYSeries series = new XYSeries("mobs");

		for (Map.Entry<String, Mob> e : rankedMobs.entrySet()) {
			Mob mob = e.getValue();
			if (mob.getLvl() <= 0 || mob.getLvl() > 60) {
				continue;
			}
			vals.put(mob.getLvl(), vals.getOrDefault(mob.getLvl(), 0d).doubleValue() + mob.getWeight());
			mobsPerLvl.put(mob.getLvl(), mobsPerLvl.getOrDefault(mob.getLvl(), 0) + 1);
		}
		for (Map.Entry<Integer, Number> entry : vals.entrySet()) {
			final int amount = mobsPerLvl.getOrDefault(entry.getKey(), Integer.MAX_VALUE);
			final Number curVal = (entry.getValue().doubleValue() / amount);
			series.add(entry.getKey(), curVal);
		}

		ds.addSeries(series);
		final JFreeChart chart = ChartFactory.createXYLineChart("Mob stats / level chart", "Mob level",
				"Mob stats", ds);
		XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) chart.getXYPlot().getRenderer();
		renderer.setDefaultItemLabelsVisible(true);
		renderer.setDefaultItemLabelGenerator(new StandardXYItemLabelGenerator("{2}"));

		File file = new File(Main.getOutDir(), fileName);

		SVGGraphics2D g2 = new SVGGraphics2D(Main.WIDTH, Main.HEIGHT);
		Rectangle r = new Rectangle(0, 0, Main.WIDTH, Main.HEIGHT);
		chart.draw(g2, r);
		SVGUtils.writeToSVG(file, g2.getSVGElement());

	}

	private static File getConfig() throws IOException {
		final File f = new File(Main.getCwd(), CONFIG_FILE);
		if (!f.exists()) {
			f.createNewFile();
		}
		return f;
	}

}
