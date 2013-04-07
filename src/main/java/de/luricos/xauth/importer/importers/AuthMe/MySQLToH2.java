package de.luricos.xauth.importer.importers.AuthMe;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.luricos.xauth.importer.importers.BaseImporter;
import org.ini4j.Profile.Section;

import de.luricos.xauth.importer.data.Account;

public class MySQLToH2 extends BaseImporter {
	protected void populateConfig() {
		Section section = ini.add("MySQL");
		section.putComment("Server", " These values should match the AuthMe MySQL configuration.");
		section.add("Server", "localhost");
		section.add("Port", 3306);
		section.add("Database", "authme");
		section.add("User", "user");
		section.add("Password", "password");

		section = ini.add("Table");
		section.putComment("Name", "This value should match the AuthMe MySQL table name.");
		section.add("Name", "authme");

		section = ini.add("Columns");
		section.putComment("Name", "These values should match the AuthMe MySQL column names.");
		section.add("Name", "username");
		section.add("Password", "password");
		section.add("IP", "ip");
		section.add("LastLogin", "lastlogin");
	}

	public void printSteps() {
		System.out.println(" 1. Download the latest version of xAuth, configure the included configuration");
		System.out.println("    file, and start your server so the H2 database is generated. Stop the");
		System.out.println("    server now.");
		System.out.println(" 2. Place the xAuth.h2.db file in the same directory as this importer.");
		System.out.println();
		System.out.println(" 3. WHEN THE IMPORT IS FINISHED: move xAuth.h2.db into the xAuth plugins folder.");
	}

	public void doImport() {
		HashMap<String, String> mysql = config.get("MySQL");
		HashMap<String, String> columns = config.get("Columns");
		String table = config.get("Table").get("Name");

		File authFile = new File("xAuth.h2.db");
		if (!authFile.exists()) {
			System.out.println("xAuth.h2.db file not found! Place xAuth.h2.db in the same directory as this Jar.");
			System.exit(0);
		}

		List<Account> accounts = new ArrayList<Account>();
		Connection conn = null;
		Statement s = null;
		ResultSet rs = null;
		System.out.println("Loading data from AuthMe MySQL database..");
		try {
			Class.forName("com.mysql.jdbc.Driver");
			String url = String.format("jdbc:mysql://%s:%s/%s", mysql.get("Server"), mysql.get("Port"), mysql.get("Database"));
			conn = DriverManager.getConnection(url, mysql.get("User"), mysql.get("Password"));
			String sql = "SELECT * FROM `" + table + "`";
			s = conn.createStatement();
			rs = s.executeQuery(sql);

			while (rs.next()) {
				Account a = new Account();
				a.setPlayerName(rs.getString(columns.get("Name")));
				a.setPassword(rs.getString(columns.get("Password")));
				a.setRegisterHost(rs.getString(columns.get("IP")));
				a.setLastLoginDate(new Timestamp(rs.getLong(columns.get("LastLogin"))));
				accounts.add(a);
			}
		} catch (Exception e) {
			System.out.println("Something went wrong while loading data from the MySQL database!");
			e.printStackTrace();
			System.exit(0);
		} finally {
			try {
				if (conn != null)
					conn.close();
			} catch (SQLException e) {}
		}

		PreparedStatement ps = null;
		System.out.println("Importing data into H2 database..");
		try {
			Class.forName("org.h2.Driver");
			String url = "jdbc:h2:xAuth;MODE=MySQL;IGNORECASE=TRUE";
			conn = DriverManager.getConnection(url, "sa", "");
			conn.setAutoCommit(false);
			String sql = "INSERT INTO `accounts` (`playername`, `password`, `pwtype`, `registerip`, `lastlogindate`) VALUES (?, ?, ?, ?, ?)";
			ps = conn.prepareStatement(sql);

			for (Account a : accounts) {
				ps.setString(1, a.getPlayerName());
				ps.setString(2, a.getPassword());
				ps.setInt(3, getHashType(a.getPassword()));
				ps.setString(4, a.getRegisterHost());
				ps.setTimestamp(5, a.getLastLoginDate());
				ps.addBatch();
			}

			ps.executeBatch();
			conn.commit();
		} catch (Exception e) {
			System.out.println("Something went wrong while importing data into the H2 database!");
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