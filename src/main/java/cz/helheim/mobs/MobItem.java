package cz.helheim.mobs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A collection of enchantments, an ID and weight
 *
 * @author Jakub Šmrha
 * @version 1.0
 * @since 1.0
 */
public class MobItem {
	/**
	 * the pattern that looks for equipment:slot
	 */
	public static final Pattern EQUIPMENT_PATTERN = Pattern.compile("(.+):\\d+");
	private String id;
	private int itemId;
	private double weight;
	private Collection<Enchantment> enchantments = new ArrayList<>();

	/**
	 * @param id           the config ID
	 * @param itemId       the in-game item ID
	 * @param weight       the weight
	 * @param enchantments the enchantments
	 */
	public MobItem(final String id, final int itemId, final double weight,
	               final Collection<Enchantment> enchantments) {
		this.id = id;
		this.itemId = itemId;
		this.weight = weight;
		this.enchantments.addAll(enchantments);
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public int getItemId() {
		return itemId;
	}

	public void setItemId(final int itemId) {
		this.itemId = itemId;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(final double weight) {
		this.weight = weight;
	}

	public Collection<Enchantment> getEnchantments() {
		return enchantments;
	}

	public void setEnchantments(final Collection<Enchantment> enchantments) {
		this.enchantments = enchantments;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, itemId, weight, enchantments);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final MobItem mobItem = (MobItem) o;
		return itemId == mobItem.itemId && Double.compare(mobItem.weight, weight) == 0 &&
				id.equals(mobItem.id) && enchantments.equals(mobItem.enchantments);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Equipment{");
		sb.append("id='").append(id).append('\'');
		sb.append(", itemId=").append(itemId);
		sb.append(", weight=").append(weight);
		sb.append(", enchantments=").append(enchantments);
		sb.append('}');
		return sb.toString();
	}
}
