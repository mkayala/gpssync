package net.ruthandtodd.gpssync;

import net.ruthandtodd.gpssync.devices.DeviceInterface;
import net.ruthandtodd.gpssync.devices.DeviceInterfaceFactory;
import org.apache.commons.configuration.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class GpssyncConfig {

    public String baseDirectory;
    public static final String ACCOUNTS_FILE = "gpssync_people.csv";
    public static final String ACTIVITIES_FILE = "gpssync_activities.csv";
    public static final String GPX_DIRECTORY = "gpx/";
    private String defaultTimezone;

    private String gantPath;
    private String gantAuthPath;
    private String gpsbabelPath;

    private String fitpath;
    private DateTime lastFitTime;
    private String fit2csvPath;

    private CompositeConfiguration config;
    private PropertiesConfiguration prop;

    private static final String FIT_TIME_KEY = "gpssync.lastfittime";

    private DeviceInterfaceFactory.Channel preferredChannel;

    private GpssyncConfig() {
        config = new CompositeConfiguration();
        config.addConfiguration(new SystemConfiguration());

        String baseDirectoryArg = config.getString("gpssync.basedir", ".");
        if (!baseDirectoryArg.endsWith("/")) {
            baseDirectoryArg += "/";
        }
        baseDirectory = baseDirectoryArg;
        try {
            prop = new PropertiesConfiguration(baseDirectory + "gpssync.properties");
        } catch (ConfigurationException e) {
            System.out.println("May have failed to load gpssync.properties file.");
            e.printStackTrace();
        }

        config.addConfiguration(prop);

        gantPath = config.getString("gpssync.gantpath", "gant");
        gantAuthPath = config.getString("gpssync.gantauth", "auth405");
        gpsbabelPath = config.getString("gpssync.gpsbabelpath", "gpsbabel");

        fitpath = config.getString("gpssync.fitpath", "/media/GARMIN/Activities/FIT/");
        if (!fitpath.endsWith("/")) {
            fitpath += "/";
        }
        fit2csvPath = config.getString("gpssync.fit2csvpath", "fit2csv.pl");

        long configLong = config.getLong(FIT_TIME_KEY, 0l);
        lastFitTime = new DateTime(configLong, DateTimeZone.UTC);

        String channelString = config.getString("gpssync.devicetype",
                DeviceInterfaceFactory.Channel.ANT.name());
        preferredChannel = DeviceInterfaceFactory.Channel.valueOf(channelString);

        defaultTimezone = config.getString("gpssync.defaulttimezone", "UTC");
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

    public String getFitDirectory() {
        return fitpath;
    }

    public DateTime getLastFitTime() {
        return new DateTime(lastFitTime, DateTimeZone.UTC);
    }

    public void setLastFitTime(DateTime time) throws ConfigurationException {
        lastFitTime = time;
        long timeMillis = time.withZone(DateTimeZone.UTC).getMillis();
        config.setProperty(FIT_TIME_KEY, timeMillis);
        prop.setProperty(FIT_TIME_KEY, timeMillis);
        prop.save();
    }

    public String getFit2csvPath() {
        return fit2csvPath;
    }

    public String getDefaultTimezone() {
        return defaultTimezone;
    }

    private static GpssyncConfig instance;

    public static GpssyncConfig getConfig() {
        if (instance == null) {
            instance = new GpssyncConfig();
        }
        return instance;
    }

    public DeviceInterfaceFactory.Channel getPreferredChannel() {
        return preferredChannel;
    }

}
