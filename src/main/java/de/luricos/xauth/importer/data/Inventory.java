package de.luricos.xauth.importer.data;

public class Inventory {
	private String playerName;
	private String items;
	private String armor;

	public Inventory(String playerName, String itemid, String amount, String durability, String enchants) {
		this.playerName = playerName;
		fix(itemid.split(","), amount.split(","), durability.split(","), enchants == null ? null : enchants.split(","));
	}

	public String getPlayerName() { return playerName; }
	public String getItems() { return items; }
	public String getArmor() { return armor; }

	private void fix(String[] itemid, String[] amount, String[] durability, String[] enchants) {
		StringBuilder sbItems = new StringBuilder();
		StringBuilder sbArmor = new StringBuilder();

		StringBuilder sbItemIds = new StringBuilder();
		StringBuilder sbAmount = new StringBuilder();
		StringBuilder sbDurability = new StringBuilder();
		StringBuilder sbEnchants = new StringBuilder();

		for (int i = 0; i < itemid.length - 4; i++) {
			sbItemIds.append(itemid[i]).append(",");
			sbAmount.append(amount[i]).append(",");
			sbDurability.append(durability[i]).append(",");

			if (enchants != null)
				sbEnchants.append(enchants[i].replace("=", ":"));
			sbEnchants.append(",");
		}

		sbItemIds.deleteCharAt(sbItemIds.lastIndexOf(","));
		sbAmount.deleteCharAt(sbAmount.lastIndexOf(","));
		sbDurability.deleteCharAt(sbDurability.lastIndexOf(","));
		sbEnchants.deleteCharAt(sbEnchants.lastIndexOf(","));
		sbItems.append(sbItemIds).append(";").append(sbAmount).append(";").append(sbDurability).append(";").append(sbEnchants);

		sbItemIds = new StringBuilder();
		sbAmount = new StringBuilder();
		sbDurability = new StringBuilder();
		sbEnchants = new StringBuilder();

		for (int i = itemid.length - 4; i < itemid.length; i++) {
			sbItemIds.append(itemid[i]).append(",");
			sbAmount.append(amount[i]).append(",");
			sbDurability.append(durability[i]).append(",");

			if (enchants != null)
				sbEnchants.append(enchants[i].replace("=", ":"));
			sbEnchants.append(",");
		}

		sbItemIds.deleteCharAt(sbItemIds.lastIndexOf(","));
		sbAmount.deleteCharAt(sbAmount.lastIndexOf(","));
		sbDurability.deleteCharAt(sbDurability.lastIndexOf(","));
		sbEnchants.deleteCharAt(sbEnchants.lastIndexOf(","));
		sbArmor.append(sbItemIds).append(";").append(sbAmount).append(";").append(sbDurability).append(";").append(sbEnchants);

		items = sbItems.toString();
		armor = sbArmor.toString();
	}
}