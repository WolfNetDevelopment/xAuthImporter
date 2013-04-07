package de.luricos.xauth.importer.importers.LockIP;

import de.luricos.xauth.importer.data.Account;
import de.luricos.xauth.importer.importers.BaseImporter;
import org.ini4j.Profile.Section;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FlatFileToMySQL extends BaseImporter {
	protected void populateConfig() {
		Section section = ini.add("MySQL");
		section.putComment("Server", " These values should match the xAuth MySQL configuration.");
		section.add("Server", "localhost");
		section.add("Port", 3306);
		section.add("Database", "xAuth");
		section.add("User", "user");
		section.add("Password", "password");

		section = ini.add("Table");
		section.putComment("Account", " This should match the table name specified in the xAuth configuration.");
		section.add("Account", "accounts");
	}

	public void printSteps() {
		System.out.println(" 1. Download, install, and configure MySQL. Create a MySQL database for xAuth.");
		System.out.println(" 2. Download the latest version of xAuth, configure the included configuration");
		System.out.println("    file, and start your server so the MySQL tables are generated. Stop the");
		System.out.println("    server now.");
		System.out.println(" 3. Place the AuthMe auths.db file in the same directory as this importer.");
	}

	public void doImport() {
		HashMap<String, String> mysql = config.get("MySQL");
		String table = config.get("Table").get("Account");

		File authFile = new File("ips.yml");
		if (!authFile.exists()) {
			System.out.println("ips.yml file not found! Place ips.yml in the same directory as this Jar.");
			System.exit(0);
		}

		List<Account> accounts = new ArrayList<Account>();
		BufferedReader reader = null;
		System.out.println("Loading accounts from file..");

		try {
			reader = new BufferedReader(new FileReader(authFile));
			String line;

			while ((line = reader.readLine()) != null) {
				String[] split = line.split(":");
				Account a = new Account();
				a.setPlayerName(split[0]);
				a.setPassword(split[1]);

				if (split.length > 2)
					a.setRegisterHost(split[2]);

				if (split.length > 3)
					a.setLastLoginDate(new Timestamp(Long.parseLong(split[3])));

				accounts.add(a);
			}
		} catch (IOException e) {
			System.out.println("Something went wrong while loading accounts from file!");
			e.printStackTrace();
			System.exit(0);
		} finally {
			try {
				if (reader != null)
					reader.close();
			} catch (IOException e) {}
		}

		Connection conn = null;
		PreparedStatement ps = null;
		System.out.println("Importing accounts into MySQL database..");

		try {
			Class.forName("com.mysql.jdbc.Driver");
			String url = String.format("jdbc:mysql://%s:%s/%s", mysql.get("Server"), mysql.get("Port"), mysql.get("Database"));
			conn = DriverManager.getConnection(url, mysql.get("User"), mysql.get("Password"));
			conn.setAutoCommit(false);
			String sql = "INSERT INTO `" + table + "` (`playername`, `password`, `pwtype`, `registerip`, `lastlogindate`) VALUES (?, ?, ?, ?, ?)";
			ps = conn.prepareStatement(sql);

			for (Account a : accounts) {
				ps.setString(1, a.getPlayerName());
				ps.setString(2, a.getPassword());
				ps.setInt(3, 6); // pwtype LockIP
				ps.setString(4, a.getRegisterHost());
				ps.setTimestamp(5, a.getLastLoginDate());
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