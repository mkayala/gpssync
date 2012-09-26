package net.ruthandtodd.gpssync.services;

import net.ruthandtodd.gpssync.GpssyncConfig;
import net.ruthandtodd.gpssync.model.Activity;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class HeatMapMaker {

    public void createDirectoryOfShapeFiles(List<Activity> activities, String dirName) {
        if (activities != null && activities.size() > 0) {
            Iterator<Activity> iterator = activities.iterator();
            Activity activity = iterator.next();
            gpxFileToShapeFile(dirName, GpssyncConfig.getConfig().getGpxDirectoryPath() + activity.getGpxFilename(), false);
            while (iterator.hasNext()) {
                activity = iterator.next();
                gpxFileToShapeFile(dirName, GpssyncConfig.getConfig().getGpxDirectoryPath() + activity.getGpxFilename(), true);
            }
        }
    }

    private void gpxFileToShapeFile(String dirname, String gpxFile, boolean append) {

        Runtime r = Runtime.getRuntime();
        Process p = null;
        try {
            p = r.exec("ogr2ogr "
                    + (append ? " -append " : "")
                    + dirname + " " + gpxFile);
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
    }

}
