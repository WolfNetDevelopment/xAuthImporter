package de.luricos.xauth.importer;

import de.luricos.xauth.importer.importers.BaseImporter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class Importer {

    private static Logger logger = Logger.getLogger("xAuthImporter");

	public static void main(String args[]) {
		DataSource from = null;
        DataSource to = null;

		List<String> pluginItems = new ArrayList<String>();
		pluginItems.add("xAuth v1.2.5");
		pluginItems.add("xAuth v2.0b4.1+");
		pluginItems.add("AuthMe v2.6.2+");
        pluginItems.add("LockIP v0.8");
		printMenu("What plugin and version are you importing from?", pluginItems);

		int selection = getSelection(pluginItems.size());
		String plugin = pluginItems.get(selection - 1).split(" ")[0];

        switch (selection) {
            case 1:
            case 4:
                from = DataSource.FlatFile;
                break;
        }


        if (from == null) {
			List<DataSource> dsItems = new ArrayList<DataSource>();
			switch (selection) {
			case 2:
				dsItems.add(DataSource.H2);
				dsItems.add(DataSource.MySQL);
				break;
			case 3:
				dsItems.add(DataSource.FlatFile);
				dsItems.add(DataSource.MySQL);
				break;
			}

			printLine();
			printMenu("What datasource did this plugin use?", dsItems);
			from = dsItems.get(getSelection(dsItems.size()) - 1);
        }

		List<DataSource> dsItems = new ArrayList<DataSource>();
		dsItems.add(DataSource.H2);
		dsItems.add(DataSource.MySQL);

		printLine();

		printMenu("What datasource will xAuth be using?", dsItems);
		to = dsItems.get(getSelection(dsItems.size()) - 1);

		printLine();

		BaseImporter importer = resolveClass(plugin, from, to);

        printEmptyLine();

		importer.printSteps();

		System.out.println(System.getProperty("line.separator") + " When you have completed these steps, press enter to continue.");

        printEmptyLine();

		printLine();
		pause();

		if (importer.useConfig() && importer.isNewConfig()) {
			System.out.println(" The file \"importer.ini\" has been created in the same folder as this jar.");
			System.out.println(" Open this file in a text editor and complete it before continuing.");
			System.out.println(System.getProperty("line.separator") + " When finished, press enter to continue." + System.getProperty("line.separator"));
			printLine();
			pause();

			importer.reloadConfig();
		}

		importer.loadConfig();
		importer.doImport();
		System.out.println("Import complete!");
		System.exit(0);
	}

	private static void printMenu(String header, List<?> items) {
		String[] arr = new String[items.size()];

		for (int i = 0; i < items.size(); i++)
			arr[i] = items.get(i).toString();

		printMenu(header, arr);
	}

	private static void printMenu(String header, String... items) {
		String newLine = System.getProperty("line.separator");
		System.out.println(newLine + " " + header + newLine);
		for (int i = 0; i < items.length; i++)
			System.out.println(" " + (i + 1) + ". " + items[i]);
		System.out.println();
	}

	private static int getSelection(int max) {
		int selection = 0;
		Scanner sc = new Scanner(System.in);
		while (selection < 1 || selection > max) {
			System.out.print(" Selection: ");
			try {
				selection = sc.nextInt();
			} catch (InputMismatchException e) {
				sc.nextLine();
			}
		}

		return selection;
	}

	private static BaseImporter resolveClass(String plugin, DataSource from, DataSource to) {
		try {
            String importerClass = "de.luricos.xauth.importer.importers." + plugin + "." + from.toString() + "To" + to.toString();
            System.out.println("Loading Importer.class - " + importerClass);
			Class<?> importer = Class.forName(importerClass);
			return (BaseImporter) importer.newInstance();
		} catch (Exception e) {
			System.out.println("This is not a supported import type.");
			System.exit(0);
			return null;
		}
	}

    private static void printEmptyLine() {
        System.out.println();
    }

	private static void printLine() {
		for (int i = 0; i < 72; i++)
			System.out.print("-");

		System.out.print(System.getProperty("line.separator"));
	}

	private static void pause() {
		try {
			System.in.read();
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	private enum DataSource {
		FlatFile,
		H2,
		MySQL
	}

    public static Logger getLogger() {
        return logger;
    }
}