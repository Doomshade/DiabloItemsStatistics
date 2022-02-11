package cz.helheim.mobs;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * An enchantment on an item
 *
 * @author Jakub Šmrha
 * @version 1.0
 * @since 1.0
 */
public class Enchantment {
	/**
	 * this pattern looks for a key:value pair, value being a number
	 */
	public static final Pattern ENCHANTMENT_PATTERN = Pattern.compile("(.*):(\\d+)");
	private String enchantmentId;
	private int level;
	private double weight;

	/**
	 * @param enchantmentId the enchantment ID
	 * @param level the enchantment level
	 * @param weight the enchantment weight
	 */
	public Enchantment(final String enchantmentId, final int level, final double weight) {
		this.enchantmentId = enchantmentId;
		this.level = level;
		this.weight = weight;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final Enchantment that = (Enchantment) o;
		return level == that.level && Double.compare(that.weight, weight) == 0 &&
				enchantmentId.equals(that.enchantmentId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(enchantmentId, level, weight);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Enchantment{");
		sb.append("enchantmentId='").append(enchantmentId).append('\'');
		sb.append(", level=").append(level);
		sb.append(", weight=").append(weight);
		sb.append('}');
		return sb.toString();
	}

	public String getEnchantmentId() {
		return enchantmentId;
	}

	public void setEnchantmentId(final String enchantmentId) {
		this.enchantmentId = enchantmentId;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(final int level) {
		this.level = level;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(final double weight) {
		this.weight = weight;
	}
}
