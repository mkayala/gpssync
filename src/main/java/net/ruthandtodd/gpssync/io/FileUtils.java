package net.ruthandtodd.gpssync.io;

import net.ruthandtodd.gpssync.GpssyncConfig;
import net.ruthandtodd.gpssync.model.Activity;
import net.ruthandtodd.gpssync.model.Model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;

public class FileUtils {

    public static void copyFile(File from, File to) throws IOException {
        Files.copy(from.toPath(), to.toPath());
    }

    public static List<String> unknownGpxInDirectory(String directory) {
        List<String> newFiles = new LinkedList<String>();
        File dir = new File(directory);
        String[] children = dir.list();
        if (children != null) {
            for (String filename : children) {
                if (filename.toLowerCase().endsWith("gpx") &&
                        !Model.getModel().haveActivityWithFilename(filename)) {
                    try {
                        copyFile(new File(dir, filename), new File(GpssyncConfig.getConfig().getGpxDirectoryPath(), filename));
                        newFiles.add(filename);
                    } catch (IOException e) {
                        System.err.println("Error copying gpx to desired directory.");
                        e.printStackTrace();
                    }
                }
            }
        }
        return newFiles;
    }

    public static String readFileToString(Activity activity) {
        String str = "";
        try {
            BufferedReader fileReader = new BufferedReader(
                    new FileReader(GpssyncConfig.getConfig().getGpxDirectoryPath() + activity.getGpxFilename()));
            String sRead = null;
            do {
                sRead = fileReader.readLine();
                if (sRead != null) {
                    str += sRead;
                }
            }
            while (sRead != null);
        } catch (Exception e) {
            System.err.println("Error reading users.\n");
            e.printStackTrace();
        }
        return str;
    }

}
