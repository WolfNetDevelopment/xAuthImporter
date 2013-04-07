package de.luricos.xauth.importer.importers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import org.ini4j.Ini;
import org.ini4j.Profile.Section;

public abstract class BaseImporter {
	protected final File confFile = new File("importer.ini");
	protected final Ini ini = new Ini();
	protected final HashMap<String, HashMap<String, String>> config = new HashMap<String, HashMap<String, String>>();
	protected boolean useConfig, newConfig = false;

	public BaseImporter() {
		this(true);
	}

	public BaseImporter(boolean useConfig) {
		this.useConfig = useConfig;

		if (useConfig) {
			try {
				if (confFile.exists()) {
					ini.load(confFile);
					if (!checkConfig()) {
						ini.clear();
						confFile.delete();
						createConfig();
					}
				} else
					createConfig();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(0);
			}
		}
	}

	public boolean useConfig() { return useConfig; }
	public boolean isNewConfig() { return newConfig; }

	private boolean checkConfig() {
		try {
			Section section = ini.get("Internal");
			String version = section.get("Version");
			return getClass().toString().endsWith(version);
		} catch (Exception e) {
			return false;
		}
	}

	public void loadConfig() {
		for (String sectionKey : ini.keySet()) {
			HashMap<String, String> keyData = new HashMap<String, String>();

			Section section = ini.get(sectionKey);
			for (String key : section.keySet())
				keyData.put(key, section.get(key));

			config.put(sectionKey, keyData);
		}
	}

	private void createConfig() {
		newConfig = true;

		try {
			confFile.createNewFile();
			ini.load(confFile);
			populateConfig();

			String[] split = getClass().toString().split("\\.");
			Section section = ini.add("Internal");
			section.putComment("Version", " Internal use only, do not touch!");
			section.add("Version", split[split.length - 2] + "." + split[split.length - 1]);

			saveConfig();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	private void saveConfig() {
		try {
			ini.store(confFile);
		} catch (IOException e) {
			System.out.println("Failed to save configuration file!");
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void reloadConfig() {
		try {
			ini.load(confFile);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	protected abstract void populateConfig();
	public abstract void printSteps();
	public abstract void doImport();

	protected int getHashType(String hash) {
		int length = hash.length();
		if (length == 32) // MD5
			return 2;
		else if (length == 40) // SHA1
			return 3;
		else if (length == 128) // WHIRLPOOL
			return 1;
		else if (length == 86) // AuthMe SHA256
			return 5;
		else if (length == 140) // xAuth
			return 0;
		else
			return 0;
	}
}