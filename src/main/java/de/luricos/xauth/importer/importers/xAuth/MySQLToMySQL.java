package de.luricos.xauth.importer.importers.xAuth;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.luricos.xauth.importer.data.Account;
import de.luricos.xauth.importer.data.Inventory;
import de.luricos.xauth.importer.data.Location;
import de.luricos.xauth.importer.importers.BaseImporter;
import org.ini4j.Profile.Section;

public class MySQLToMySQL extends BaseImporter {
	protected void populateConfig() {
		Section section = ini.add("Old MySQL");
		section.putComment("Server", " These values should match the MySQL configuration in the OLD xAuth configuration.");
		section.add("Server", "localhost");
		section.add("Port", 3306);
		section.add("Database", "xAuth");
		section.add("User", "user");
		section.add("Password", "password");

		section = ini.add("Old Tables");
		section.putComment("Account", " These should match the table names specified in the OLD xAuth configuration.");
		section.add("Account", "accounts_old");
		section.add("Inventory", "inventory");
		section.add("Location", "tele_locations");

		section = ini.add("New MySQL");
		section.putComment("Server", " These values should match the MySQL configuration in the NEW xAuth configuration.");
		section.add("Server", "localhost");
		section.add("Port", 3306);
		section.add("Database", "xAuth");
		section.add("User", "user");
		section.add("Password", "password");

		section = ini.add("New Tables");
		section.putComment("Account", " These should match the table names specified in the NEW xAuth configuration");
		section.add("Account", "accounts");
		section.add("PlayerData", "playerdata");
		section.add("Location", "locations");
	}

	public void printSteps() {
		System.out.println(" 1. Delete the old xAuth plugin, config.yml, messages.yml, and DBVERSION files.");
		System.out.println(" 2. Rename the accounts and sessions tables in the database used by xAuth to");
		System.out.println("    anything other than what the new tables will be named.");
		System.out.println(" 3. Download the latest version of xAuth, configure the included configuration");
		System.out.println("    file, and start your server so the MySQL tables are generated. Stop the");
		System.out.println("    server now.");
	}

	public void doImport() {
		HashMap<String, String> oldMysql = config.get("Old MySQL");
		HashMap<String, String> oldTables = config.get("Old Tables");
		HashMap<String, String> newMysql = config.get("New MySQL");
		HashMap<String, String> newTables = config.get("New Tables");

		List<Account> accounts = new ArrayList<Account>();
		List<Inventory> inventories = new ArrayList<Inventory>();
		List<Location> locations = new ArrayList<Location>();
		Connection conn = null;
		Statement s = null;
		ResultSet rs = null;

		System.out.println("Loading data from old MySQL database..");
		try {
			Class.forName("com.mysql.jdbc.Driver");
			String url = String.format("jdbc:mysql://%s:%s/%s", oldMysql.get("Server"), oldMysql.get("Port"), oldMysql.get("Database"));
			conn = DriverManager.getConnection(url, oldMysql.get("User"), oldMysql.get("Password"));
			String sql = "SELECT * FROM `" + oldTables.get("Account") + "`";
			s = conn.createStatement();
			rs = s.executeQuery(sql);

			while (rs.next()) {
				Account a = new Account();
				a.setPlayerName(rs.getString("playername"));
				a.setPassword(rs.getString("password"));
				a.setEmail(rs.getString("email"));
				a.setRegisterDate(rs.getTimestamp("registerdate"));
				a.setRegisterHost(rs.getString("registerip"));
				a.setLastLoginDate(rs.getTimestamp("lastlogindate"));
				a.setLastLoginHost(rs.getString("lastloginip"));
				a.setActive(rs.getInt("active"));
				accounts.add(a);
			}

			s.close();
			sql = "SELECT * FROM `" + oldTables.get("Inventory") + "`";
			s = conn.createStatement();
			rs = s.executeQuery(sql);

			boolean hasEnchants = false;
			ResultSetMetaData rsMetaData = rs.getMetaData();
			for (int i = 1; i <= rsMetaData.getColumnCount(); i++) {
				if (rsMetaData.getColumnName(i).equals("enchantments"))
					hasEnchants = true;
			}

			while (rs.next()) {
				String player = rs.getString("playerName");
				String itemid = rs.getString("itemid");
				String amount = rs.getString("amount");
				String durability = rs.getString("durability");
				String enchants = null;
				if (hasEnchants)
					enchants = rs.getString("enchantments");

				Inventory inv = new Inventory(player, itemid, amount, durability, enchants);
				inventories.add(inv);
			}

			s.close();
			sql = "SELECT * FROM `" + oldTables.get("Location") + "`";
			s = conn.createStatement();
			rs = s.executeQuery(sql);

			while (rs.next()) {
				String uid = rs.getString("uid");
				double x = rs.getDouble("x");
				double y = rs.getDouble("y");
				double z = rs.getDouble("z");
				float yaw = rs.getFloat("yaw");
				float pitch = rs.getFloat("pitch");
				int global = rs.getInt("global");
				Location loc = new Location(uid, x, y, z, yaw, pitch, global);
				locations.add(loc);
			}

		} catch (Exception e) {
			System.out.println("Something went wrong while loading data from the MySQL database!");
			e.printStackTrace();
			System.exit(0);
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {}
		}

		PreparedStatement ps = null;
		System.out.println("Importing data into new MySQL database..");
		try {
			String url = String.format("jdbc:mysql://%s:%s/%s", newMysql.get("Server"), newMysql.get("Port"), newMysql.get("Database"));
			conn = DriverManager.getConnection(url, newMysql.get("User"), newMysql.get("Password"));
			conn.setAutoCommit(false);
			String sql = "INSERT INTO `" + newTables.get("Account") + "` (`playername`, `password`, `pwtype`, `email`, `registerdate`, `registerip`, `lastlogindate`, `lastloginip`, `active`)" +
					"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
			ps = conn.prepareStatement(sql);

			for (Account a : accounts) {
				ps.setString(1, a.getPlayerName());
				ps.setString(2, a.getPassword());
				ps.setInt(3, getHashType(a.getPassword()));
				ps.setString(4, a.getEmail());
				ps.setTimestamp(5, a.getRegisterDate());
				ps.setString(6, a.getRegisterHost());
				ps.setTimestamp(7, a.getLastLoginDate());
				ps.setString(8, a.getLastLoginHost());
				ps.setInt(9, a.getActive());
				ps.addBatch();
			}

			ps.executeBatch();
			ps.close();
	
			sql = "INSERT INTO `" + newTables.get("PlayerData") + "` (`playername`, `items`, `armor`, `location`) VALUES (?, ?, ?, ?)";
			ps = conn.prepareStatement(sql);

			for (Inventory inv : inventories) {
				ps.setString(1, inv.getPlayerName());
				ps.setString(2, inv.getItems());
				ps.setString(3, inv.getArmor());
				ps.setString(4, null);
				ps.addBatch();
			}

			ps.executeBatch();
			ps.close();

			sql = "INSERT INTO `" + newTables.get("Location") + "` VALUES (?, ?, ?, ?, ?, ?, ?)";
			ps = conn.prepareStatement(sql);

			for (Location loc : locations) {
				ps.setString(1, loc.getUID());
				ps.setDouble(2, loc.getX());
				ps.setDouble(3, loc.getY());
				ps.setDouble(4, loc.getZ());
				ps.setFloat(5, loc.getYaw());
				ps.setFloat(6, loc.getPitch());
				ps.setInt(7, loc.getGlobal());
				ps.addBatch();
			}

			ps.executeBatch();
			conn.commit();
		} catch (Exception e) {
			System.out.println("Something went wrong while importing data into the MySQL database!");
			e.printStackTrace();
			System.exit(0);
		} finally {
			try {
				if (conn != null) {
					conn.rollback();
					conn.close();
				}
			} catch (SQLException e) {}
		}
	}
}