package de.luricos.xauth.importer.importers.AuthMe;

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

import de.luricos.xauth.importer.data.Account;
import org.ini4j.Profile.Section;

import de.luricos.xauth.importer.importers.BaseImporter;

public class MySQLToMySQL extends BaseImporter {
	protected void populateConfig() {
		Section section = ini.add("AuthMe MySQL");
		section.putComment("Server", " These values should match the AuthMe MySQL configuration.");
		section.add("Server", "localhost");
		section.add("Port", 3306);
		section.add("Database", "authme");
		section.add("User", "user");
		section.add("Password", "password");
		section.add("Table", "authme");

		section = ini.add("AuthMe Columns");
		section.putComment("Name", "These values should match the AuthMe MySQL column names.");
		section.add("Name", "username");
		section.add("Password", "password");
		section.add("IP", "ip");
		section.add("LastLogin", "lastlogin");

		section = ini.add("xAuth MySQL");
		section.putComment("Server", " These values should match the xAuth MySQL configuration.");
		section.add("Server", "localhost");
		section.add("Port", 3306);
		section.add("Database", "xAuth");
		section.add("User", "user");
		section.add("Password", "password");

		section = ini.add("xAuth Table");
		section.putComment("Account", " This should match the table name specified in the xAuth configuration.");
		section.add("Account", "accounts");
	}

	public void printSteps() {
		System.out.println(" 1. Download the latest version of xAuth, configure the included configuration");
		System.out.println("    file, and start your server so the MySQL tables are generated. Stop the");
		System.out.println("    server now.");
	}

	public void doImport() {
		HashMap<String, String> oldMysql = config.get("AuthMe MySQL");
		HashMap<String, String> columns = config.get("AuthMe Columns");
		HashMap<String, String> newMysql = config.get("xAuth MySQL");
		String table = config.get("xAuth Table").get("Account");

		List<Account> accounts = new ArrayList<Account>();
		Connection conn = null;
		Statement s = null;
		ResultSet rs = null;
		System.out.println("Loading accounts from AuthMe MySQL database..");

		try {
			Class.forName("com.mysql.jdbc.Driver");
			String url = String.format("jdbc:mysql://%s:%s/%s", oldMysql.get("Server"), oldMysql.get("Port"), oldMysql.get("Database"));
			conn = DriverManager.getConnection(url, oldMysql.get("User"), oldMysql.get("Password"));
			String sql = "SELECT * FROM `" + oldMysql.get("Table") + "`";
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
		System.out.println("Importing data into xAuth MySQL database..");

		try {
			String url = String.format("jdbc:mysql://%s:%s/%s", newMysql.get("Server"), newMysql.get("Port"), newMysql.get("Database"));
			conn = DriverManager.getConnection(url, newMysql.get("User"), newMysql.get("Password"));
			conn.setAutoCommit(false);
			String sql = "INSERT INTO `" + table + "` (`playername`, `password`, `pwtype`, `registerip`, `lastlogindate`) VALUES (?, ?, ?, ?, ?)";
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
		} catch (SQLException e) {
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