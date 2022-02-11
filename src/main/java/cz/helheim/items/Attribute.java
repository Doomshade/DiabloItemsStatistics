package cz.helheim.items;

import java.util.regex.Pattern;

/**
 * Represents either a class attribute (Vitalita, Víra) or an item attribute (Poškození, Vyhýbání)
 *
 * @author Jakub Šmrha
 * @version 1.0
 * @since 1.0
 */
public class Attribute {
	/**
	 * The item attribute pattern, looks for "{attribute}: {min}-{max}"
	 */
	public static transient final Pattern ATTRIBUTE_PATTERN = Pattern.compile("&.\\+(\\d+)%?(-(\\d+))?%? (.*)");

	/**
	 * The class attribute pattern, looks for "+{min}-{max} {attribute}"
	 */
	public static transient final Pattern CLASS_PATTERN = Pattern.compile("&.(.*): (\\d+)%?(-(\\d+))?%?");
	private String attribute;
	private int min, max;

	/**
	 * @param attribute the attribute key
	 * @param min the min value
	 * @param max the max value
	 */
	public Attribute(String attribute, int min, int max) {
		this.attribute = attribute;
		this.min = min;
		this.max = max;
	}

	public int getMin() {
		return min;
	}

	public void setMin(final int min) {
		this.min = min;
	}

	public int getMax() {
		return max;
	}

	public void setMax(final int max) {
		this.max = max;
	}

	public String getAttribute() {
		return attribute;
	}

	public void setAttribute(final String attribute) {
		this.attribute = attribute;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Attribute{");
		sb.append("attribute='").append(attribute).append('\'');
		sb.append(", min=").append(min);
		sb.append(", max=").append(max);
		sb.append('}');
		return sb.toString();
	}
}
