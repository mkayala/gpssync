package net.ruthandtodd.gpssync;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

public class GpssyncConfig {

    public final String baseDirectory;
    public static final String ACCOUNTS_FILE = "gpssync_people.csv";
    public static final String ACTIVITIES_FILE = "gpssync_activities.csv";
    public static final String GPX_DIRECTORY = "gpx/";

    public String gantPath;
    public String gantAuthPath;
    public String gpsbabelPath;

    private final Configuration systemConfig;

    private GpssyncConfig(Configuration configuration) {
        systemConfig = configuration;
        String baseDirectoryArg = systemConfig.getString("gpssync.basedir", "");
        if (!baseDirectoryArg.endsWith("/")) {
            baseDirectoryArg += "/";
        }
        baseDirectory = baseDirectoryArg;
        try {
            SystemConfiguration.setSystemProperties(new PropertiesConfiguration(baseDirectory + "gpssync.properties"));
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
        gantPath = systemConfig.getString("gpssync.gantpath");
        gantAuthPath = systemConfig.getString("gpssync.gantauth");
        gpsbabelPath = systemConfig.getString("gpssync.gpsbabelpath");
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
            instance = new GpssyncConfig(new SystemConfiguration());
        }
        return instance;
    }
}
