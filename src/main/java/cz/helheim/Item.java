package cz.helheim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * @author Jakub Šmrha
 * @version 1.0
 * @since 1.0
 */
public class Item implements Comparable<Item> {
	private Collection<Attribute> attributes = new ArrayList<>();
	private transient String itemId = "";
	private double avgRank = 0d, minRank = 0d, maxRank = 0d;

	public Item(final String itemId, final Collection<Attribute> attributes) {
		this.itemId = itemId;
		this.attributes.addAll(attributes);
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
		sb.append("itemId='").append(itemId).append('\'');
		sb.append(", attributes=").append(attributes);
		sb.append(", rank=").append(avgRank);
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

	@Override
	public int compareTo(final Item o) {
		return Double.compare(getAvgRank(), o.getAvgRank());
	}

	public double getAvgRank() {
		return avgRank;
	}

	public void setAvgRank(final double avgRank) {
		this.avgRank = avgRank;
	}
}
