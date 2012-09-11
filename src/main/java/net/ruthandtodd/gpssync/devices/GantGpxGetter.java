package net.ruthandtodd.gpssync.devices;

import net.ruthandtodd.gpssync.GpssyncConfig;
import net.ruthandtodd.gpssync.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class GantGpxGetter implements DeviceInterface {

    public List<String> getNewestActivities() {
        Set<String> beforeFileNames = new HashSet<String>();
        // get a list of files.
        File dir = new File(".");

        String[] children = dir.list();
        if (children != null) {
            for (String filename : children) {
                if (filename.toLowerCase().trim().endsWith("tcx")) {
                    beforeFileNames.add(filename);
                }
            }
        }

        // make the gant call
        Runtime r = Runtime.getRuntime();
        Process p = null;
        try {
            p = r.exec(GpssyncConfig.getConfig().getGantPath() + " -nza " + GpssyncConfig.getConfig().getGantAuthPath());
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

        // get a new list of files / find the new one
        List<String> newFiles = new LinkedList<String>();
        children = dir.list();
        if (children != null) {
            for (String filename : children) {
                if (filename.toLowerCase().trim().endsWith("tcx")) {
                    if (!beforeFileNames.contains(filename)) {
                        newFiles.add(filename);
                    }
                }
            }
        }

        List<String> gpxNames = new LinkedList<String>();
        for (String newFileNameTCX : newFiles) {
            String newFileNameGPX = newFileNameTCX.toLowerCase().replaceAll("tcx", "gpx");
            gpxNames.add(newFileNameGPX);
            // use gpsbabel to make a gpx
            try {
                p = r.exec(GpssyncConfig.getConfig().getGpsbabelPath() + " -i gtrnctr -f "
                        + newFileNameTCX + " -o gpx -F " + newFileNameGPX);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                p.waitFor();
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
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        // retun the filenames
        return gpxNames;
    }


}
