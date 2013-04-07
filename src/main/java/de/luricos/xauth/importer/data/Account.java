package de.luricos.xauth.importer.data;

import java.sql.Timestamp;

public class Account {
	private String playerName;
	private String password;
	private String email;
	private Timestamp registerDate;
	private String registerHost;
	private Timestamp lastLoginDate;
	private String lastLoginHost;
	private int active;

	public Account() {}

	public void setPlayerName(String playerName) { this.playerName = playerName; }
	public String getPlayerName() { return playerName; }

	public void setPassword(String password) { this.password = password; }
	public String getPassword() { return password; }

	public void setEmail(String email) { this.email = email; }
	public String getEmail() { return email; }

	public void setRegisterDate(Timestamp registerDate) { this.registerDate = registerDate; }
	public Timestamp getRegisterDate() { return registerDate; }

	public void setRegisterHost(String registerHost) { this.registerHost = registerHost; }
	public String getRegisterHost() { return registerHost; }

	public void setLastLoginDate(Timestamp lastLoginDate) { this.lastLoginDate = lastLoginDate; }
	public Timestamp getLastLoginDate() { return lastLoginDate; }

	public void setLastLoginHost(String lastLoginHost) { this.lastLoginHost = lastLoginHost; }
	public String getLastLoginHost() { return lastLoginHost; }

	public void setActive(int active) { this.active = active; }
	public int getActive() { return active; }
}