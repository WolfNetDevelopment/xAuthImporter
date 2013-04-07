package de.luricos.xauth.importer.importers.AuthMe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import de.luricos.xauth.importer.data.Account;
import de.luricos.xauth.importer.importers.BaseImporter;

public class FlatFileToH2 extends BaseImporter {
	public FlatFileToH2() {
		super(false);
	}

	protected void populateConfig() {}

	public void printSteps() {
		System.out.println(" 1. Download the latest version of xAuth, configure the included configuration");
		System.out.println("    file, and start your server so the H2 database is generated. Stop the");
		System.out.println("    server now.");
		System.out.println(" 2. Place the AuthMe auths.db file and xAuth xAuth.h2.db file in the same");
		System.out.println("    directory as this importer.");
		System.out.println();
		System.out.println(" 3. WHEN THE IMPORT IS FINISHED: move xAuth.h2.db into the xAuth plugins folder.");
	}

	public void doImport() {
		File authFile = new File("auths.db");
		if (!authFile.exists()) {
			System.out.println("auths.db file not found! Place auths.db in the same directory as this Jar.");
			System.exit(0);
		}

		File h2File = new File("xAuth.h2.db");
		if (!h2File.exists()) {
			System.out.println("xAuth.h2.db file not found! Place xAuth.h2.db in the same directory as this Jar.");
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
		System.out.println("Importing accounts into H2 database..");

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