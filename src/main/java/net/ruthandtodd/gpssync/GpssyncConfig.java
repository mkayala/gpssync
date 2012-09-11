package net.ruthandtodd.gpssync;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class GpssyncConfig {

    public final String baseDirectory;
    public static final String ACCOUNTS_FILE = "gpssync_people.csv";
    public static final String ACTIVITIES_FILE = "gpssync_activities.csv";
    public static final String GPX_DIRECTORY = "gpx/";

    public String gantPath;
    public String gantAuthPath;
    public String gpsbabelPath;

    private GpssyncConfig() {
        String baseDirectoryArg = System.getProperty("gpssync.basedir", "");
        if (!baseDirectoryArg.endsWith("/")) {
            baseDirectoryArg += "/";
        }
        baseDirectory = baseDirectoryArg;

        try {
            loadPropertiesFile(baseDirectory + "gpssync.properties");
        } catch (IOException e) {
            System.out.println("Failed to load configuration, garmin stuff won't work.");
        }
    }

    private void loadPropertiesFile(String path) throws IOException {
        BufferedReader fileReader = new BufferedReader(
                new FileReader(path));
        String sRead = null;
        do {
            sRead = fileReader.readLine();
            if (sRead != null) {
                String[] split = sRead.split("=");
                if (split.length == 2) {
                    String name = split[0].trim();
                    String value = split[1].trim();
                    if (name.equals("gpssync.gantpath")) {
                        gantPath = value;
                    } else if (name.equals("gpssync.gantauth")) {
                        gantAuthPath = value;
                    } else if (name.equals("gpssync.gpsbabelpath")) {
                        gpsbabelPath = value;
                    }
                }
            }
        } while (sRead != null);
    }

    private String addpath(String file) {
        return baseDirectory + file;
    }

    public String getAccountsFilePath() {
        return addpath(ACCOUNTS_FILE);
    }

    public String getActivitiesFilePath() {
        return addpath(ACTIVITIES_FILE);
    }

    public String getGpxDirectoryPath() {
        return addpath(GPX_DIRECTORY);
    }

    public String getGantPath() {
        return gantPath;
    }

    public String getGantAuthPath() {
        return gantAuthPath;
    }

    public String getGpsbabelPath() {
        return gpsbabelPath;
    }

    private static GpssyncConfig instance;

    public static GpssyncConfig getConfig() {
        if (instance == null) {
            instance = new GpssyncConfig();
        }
        return instance;
    }
}
