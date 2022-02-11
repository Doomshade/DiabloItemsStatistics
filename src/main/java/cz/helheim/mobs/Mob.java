package cz.helheim.mobs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A mob, i.e an ID, a name, health, damage, level, and equipment
 *
 * @author Jakub Šmrha
 * @version 1.0
 * @since 1.0
 */
public class Mob implements Comparable<Mob> {
	// &8[&4Lv. 25&8]&c Efrít
	/**
	 * the pattern looks for {colour}[{colour}Lv. {level}{colour}]{colour} {name}
	 */
	public static final Pattern NAME_PATTERN = Pattern.compile("..\\[&.Lv\\. (\\d+)&.][ ]*&.[ ]*(.*)");
	private String mobId;
	private String mobName;
	private int health;
	private int damage;
	private int lvl;
	private double weight;
	private Collection<MobItem> mobItems = new ArrayList<>();

	/**
	 * @param mobId    the mob ID
	 * @param mobName  the mob name
	 * @param health   the mob health
	 * @param damage   the mob damage
	 * @param mobItems the mob equipment
	 */
	public Mob(final String mobId, final String mobName, final int health, final int damage,
	           final Collection<MobItem> mobItems) {
		this.weight = 0;
		this.mobId = mobId;
		this.mobName = mobName;
		this.health = health;
		this.damage = damage;
		final Matcher m = NAME_PATTERN.matcher(mobName);
		if (!m.find()) {
			this.lvl = 0;
		} else {
			this.lvl = Integer.parseInt(m.group(1));
		}
		this.mobItems.addAll(mobItems);
	}

	public int getLvl() {
		return lvl;
	}

	public void setLvl(final int lvl) {
		this.lvl = lvl;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Mob{");
		sb.append("mobId='").append(mobId).append('\'');
		sb.append(", mobName='").append(mobName).append('\'');
		sb.append(", health=").append(health);
		sb.append(", damage=").append(damage);
		sb.append(", equipment=").append(mobItems);
		sb.append('}');
		return sb.toString();
	}

	public String getMobId() {
		return mobId;
	}

	public void setMobId(final String mobId) {
		this.mobId = mobId;
	}

	public String getMobName() {
		return mobName;
	}

	public void setMobName(final String mobName) {
		this.mobName = mobName;
	}

	public int getHealth() {
		return health;
	}

	public void setHealth(final int health) {
		this.health = health;
	}

	public int getDamage() {
		return damage;
	}

	public void setDamage(final int damage) {
		this.damage = damage;
	}

	public Collection<MobItem> getEquipment() {
		return mobItems;
	}

	public void setEquipment(final Collection<MobItem> mobItems) {
		this.mobItems = mobItems;
	}

	@Override
	public int compareTo(final Mob o) {
		return Integer.compare(lvl, o.lvl);
	}

	/**
	 * Calculates the weight and returns it. This is a getter because of serialization!
	 *
	 * @return the weight
	 */
	public double getWeight() {
		this.weight = 0;
		for (MobItem eq : mobItems) {
			weight += eq.getWeight();
			for (Enchantment ench : eq.getEnchantments()) {
				weight += ench.getWeight() * ench.getLevel();
			}
		}
		weight += health;
		weight += damage;
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}
}
