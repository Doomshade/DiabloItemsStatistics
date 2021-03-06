package cz.helheim.items;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Represents an item, i.e. a collection of attributes, an item ID, and level requirement
 *
 * @author Jakub Šmrha
 * @version 1.0
 * @since 1.0
 */
public class Item {
	private Collection<Attribute> attributes = new ArrayList<>();
	private transient String itemId = "";
	private double avgRank = 0d, minRank = 0d, maxRank = 0d;
	private int lvl;

	/**
	 * @param itemId the item ID
	 * @param attributes the item attributes
	 * @param lvl the item level req
	 */
	public Item(final String itemId, final Collection<Attribute> attributes, int lvl) {
		this.itemId = itemId;
		this.attributes.addAll(attributes);
		this.lvl = lvl;
	}

	public int getLvl() {
		return lvl;
	}

	public void setLvl(final int lvl) {
		this.lvl = lvl;
	}

	public String getItemId() {
		return itemId;
	}

	public void setItemId(final String itemId) {
		this.itemId = itemId;
	}

	public Collection<Attribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(final Collection<Attribute> attributes) {
		this.attributes = attributes;
	}

	@Override
	public int hashCode() {
		return Objects.hash(itemId, attributes, avgRank);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final Item item = (Item) o;
		return Double.compare(item.avgRank, avgRank) == 0 && itemId.equals(item.itemId) &&
				attributes.equals(item.attributes);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Item{");
		sb.append("attributes=").append(attributes);
		sb.append(", itemId='").append(itemId).append('\'');
		sb.append(", avgRank=").append(avgRank);
		sb.append(", minRank=").append(minRank);
		sb.append(", maxRank=").append(maxRank);
		sb.append(", lvl=").append(lvl);
		sb.append('}');
		return sb.toString();
	}

	public double getMinRank() {
		return minRank;
	}

	public void setMinRank(final double minRank) {
		this.minRank = minRank;
	}

	public double getMaxRank() {
		return maxRank;
	}

	public void setMaxRank(final double maxRank) {
		this.maxRank = maxRank;
	}

	public double getAvgRank() {
		return avgRank;
	}

	public void setAvgRank(final double avgRank) {
		this.avgRank = avgRank;
	}
}
