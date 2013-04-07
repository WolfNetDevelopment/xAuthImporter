package de.luricos.xauth.importer.importers.xAuth;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.luricos.xauth.importer.data.Account;
import de.luricos.xauth.importer.importers.BaseImporter;
import org.ini4j.Profile.Section;

public class FlatFileToMySQL extends BaseImporter {
	protected void populateConfig() {
		Section section = ini.add("MySQL");
		section.putComment("Server", " These values should match the MySQL configuration in the NEW xAuth configuration.");
		section.add("Server", "localhost");
		section.add("Port", 3306);
		section.add("Database", "xAuth");
		section.add("User", "user");
		section.add("Password", "password");

		section = ini.add("Table");
		section.putComment("Account", " This should match the table name specified in the new xAuth configuration.");
		section.add("Account", "accounts");
	}

	public void printSteps() {
		System.out.println(" 1. Download, install, and configure MySQL. Create a MySQL database for xAuth.");
		System.out.println(" 2. Delete the old xAuth plugin, config.yml, and strings.yml files.");
		System.out.println(" 3. Download the latest version of xAuth, configure the included configuration");
		System.out.println("    file, and start your server so the MySQL tables are generated. Stop the");
		System.out.println("    server now.");
		System.out.println(" 4. Place the old auths.txt xAuth file in the same directory as the importer.");
	}

	public void doImport() {
		HashMap<String, String> mysql = config.get("MySQL");
		String table = config.get("Table").get("Account");

		File authFile = new File("auths.txt");
		if (!authFile.exists()) {
			System.out.println("auths.txt file not found! Place auths.txt in the same directory as this Jar.");
			System.exit(0);
		}

		System.out.println("Loading accounts from file..");
		List<Account> accounts = new ArrayList<Account>();
		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new FileReader(authFile));
			String line;

			while ((line = reader.readLine()) != null) {
				String[] split = line.split(":");
				Account a = new Account();
				a.setPlayerName(split[0]);
				a.setPassword(split[1]);
				accounts.add(a);
			}
		} catch (IOException e) {
			System.out.println("Failed to load accounts from auths.txt!");
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
			String sql = "INSERT INTO `" + table + "` (`playername`, `password`, `pwtype`) VALUES (?, ?, ?)";
			ps = conn.prepareStatement(sql);

			for (Account a : accounts) {
				ps.setString(1, a.getPlayerName());
				ps.setString(2, a.getPassword());
				ps.setInt(3, getHashType(a.getPassword()));
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