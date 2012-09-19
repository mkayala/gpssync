package net.ruthandtodd.gpssync;

import org.apache.commons.configuration.*;

public class GpssyncConfig {

    public final String baseDirectory;
    public static final String ACCOUNTS_FILE = "gpssync_people.csv";
    public static final String ACTIVITIES_FILE = "gpssync_activities.csv";
    public static final String GPX_DIRECTORY = "gpx/";

    public String gantPath;
    public String gantAuthPath;
    public String gpsbabelPath;

    private final CompositeConfiguration config;

    private GpssyncConfig() {
        config = new CompositeConfiguration();
        config.addConfiguration(new SystemConfiguration());

        String baseDirectoryArg = config.getString("gpssync.basedir", "");
        if (!baseDirectoryArg.endsWith("/")) {
            baseDirectoryArg += "/";
        }
        baseDirectory = baseDirectoryArg;
        try {
            PropertiesConfiguration prop = new PropertiesConfiguration(baseDirectory + "gpssync.properties");
            config.addConfiguration(prop);
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }

        gantPath = config.getString("gpssync.gantpath", "gant");
        gantAuthPath = config.getString("gpssync.gantauth", "auth405");
        gpsbabelPath = config.getString("gpssync.gpsbabelpath", "gpsbabel");
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
