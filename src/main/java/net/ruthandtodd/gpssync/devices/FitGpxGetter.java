package net.ruthandtodd.gpssync.devices;

import net.ruthandtodd.gpssync.GpssyncConfig;
import net.ruthandtodd.gpssync.io.FileUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * For now, we're going to implement this as basically a wrapper that calls the existing
 * perl script. I would like to redo it using the legit Fit SDK, but I don't want to make
 * it too hard to build this package for people who don't need Fit functionality.
 */
public class FitGpxGetter implements DeviceInterface {


    @Override
    public List<String> getNewestActivities() {
        File fitDirectory = new File(GpssyncConfig.getConfig().getFitDirectory());

        List<String> newFitFiles = new LinkedList<String>();

        String[] children = fitDirectory.list();
        if (children != null) {
            for (String filename : children) {
                if (filename.toLowerCase().trim().endsWith("fit")) {
                    File fitFile = new File(GpssyncConfig.getConfig().getFitDirectory() + filename);
                    DateTime lastModified = new DateTime(fitFile.lastModified());
                    if (lastModified.isAfter(GpssyncConfig.getConfig().getLastFitTime())) {
                        newFitFiles.add(filename);
                    }
                }
            }
        }
        List<String> gpxFiles = new LinkedList<String>();
        for (String s : newFitFiles) {
            gpxFiles.add(fitToGpx(s));
        }
        try {
            GpssyncConfig.getConfig().setLastFitTime(new DateTime(DateTimeZone.UTC));
        } catch (ConfigurationException e) {
            e.printStackTrace();
            System.err.println("Probably failed to save the time of our most recent Fit reading, so the next run may reparse some .fit files.");
        }
        return gpxFiles;
    }

    private String fitToGpx(String fitFile) {
        // make the perl call
        Runtime r = Runtime.getRuntime();
        Process p = null;
        try {
            p = r.exec("perl " + GpssyncConfig.getConfig().getFit2csvPath() + " " + GpssyncConfig.getConfig().getFitDirectory() + fitFile + " > fitcsvtmp.csv");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            p.waitFor();
            p.destroy();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.interrupted();
        }

        String newFileNameGPX = fitFile.toLowerCase().replaceAll("fit", "gpx");
        try {
            p = r.exec(GpssyncConfig.getConfig().getGpsbabelPath() + " -i unicsv -f fitcsvtmp.csv"
                    + " -x transform,trk=wpt,del -o gpx -F " + newFileNameGPX);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            p.waitFor();
            p.destroy();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.interrupted();
        }

        File file = new File(newFileNameGPX);
        // Destination directory
        File gpxDir = new File(GpssyncConfig.getConfig().getGpxDirectoryPath());

        try {
            FileUtils.copyFile(file, new File(gpxDir, newFileNameGPX));
        } catch (IOException e) {
            System.err.println("Error copying gpx to desired directory.");
            e.printStackTrace();
        }

        return newFileNameGPX;
    }

}
