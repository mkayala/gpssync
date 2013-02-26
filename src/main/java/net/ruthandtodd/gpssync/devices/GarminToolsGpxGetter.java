package net.ruthandtodd.gpssync.devices;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.joda.time.chrono.CopticChronology;

import net.ruthandtodd.gpssync.GpssyncConfig;
import net.ruthandtodd.gpssync.io.FileUtils;

public class GarminToolsGpxGetter implements DeviceInterface {

	private String gmnDir;
	
	private static int MAX_FILE_DEPTH = 3;
	
	/*
	 * Just set up some useful things
	 */
	public GarminToolsGpxGetter(){
		gmnDir = GpssyncConfig.getConfig().baseDirectory + GpssyncConfig.GMN_DIRECTORY;
	}
	
	public List<String> getNewestActivities() {
		// First grab the existing files,
		List<String> beforeFiles = getGmnFiles(new File(gmnDir));
		Set<String> beforeFileSet = new HashSet<String>();
		beforeFileSet.addAll(beforeFiles);
		
		// Get from the watch
		runGarminSaveRuns();
		
		// Find out what is new.
		List<String> afterFiles = getGmnFiles(new File(gmnDir));
		List<String> newFiles = new LinkedList<String>();
		for (String f: afterFiles ){
			if (!beforeFileSet.contains(f)){
				newFiles.add(f);
			}
		}
		
		// Then convert to gpx, then move to gpx directory.
		LinkedList<String> convertedAndCopiedGpxFiles = new LinkedList<String>();
		for (String s: newFiles){
			System.out.println(s);
			s = convertGmnToGpx(s);
			String t = copyGpxToDirectory(s);
			convertedAndCopiedGpxFiles.add(t);
			System.out.println(t);
		}
		
		return convertedAndCopiedGpxFiles;
	}
	
	/*
	 * As it says
	 */
	public void runGarminSaveRuns(){
		File gmnbasedir = new File(gmnDir);
		
		Runtime r = Runtime.getRuntime();
        Process p = null;
        try {
            p = r.exec(GpssyncConfig.getConfig().getGsaverunsPath(), null, gmnbasedir);
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

	/*
	 * Get the list of the current gmn files recursively
	 */
	public List<String> getGmnFiles(File dir){
		return getGmnFiles(dir, MAX_FILE_DEPTH);
	}
	private List<String> getGmnFiles(File dir, int depth){
		LinkedList<String> list = new LinkedList<String>();
		File[] children = dir.listFiles();
		if (children == null || depth <= 0){
			return list;
		}
		
		for (File f : children){
			if(f.isDirectory()){
				list.addAll(getGmnFiles(f, depth - 1));
			} else {
				if (f.getName().toLowerCase().endsWith(".gmn")){
					list.add(f.getAbsolutePath());
				}
			}
		}
		return list;
	}
	
	/*
	 * Given a list of the gmn files, convert them to gpx files, returning
	 * the corresponding list of gpx files 
	 */
	public String convertGmnToGpx(String gmnFile){
		String gpxFile = gmnFile.trim().replaceAll("gmn$", "gpx");
		System.out.println("Converting to " + gpxFile);
		
		FileWriter fstream;
		BufferedWriter out;
		BufferedReader in;
		Runtime r = Runtime.getRuntime();
        Process p = null;
        String[] args = {GpssyncConfig.getConfig().getGgpxPath(), gmnFile};
        try {
        	fstream = new FileWriter(gpxFile);
			out =  new BufferedWriter(fstream);
        	p = r.exec(args, null);        	
        	in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while((line = in.readLine()) != null){
            	out.write(line);
            	out.newLine();
            }
            p.waitFor();
            p.destroy();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (InterruptedException e) {
			e.printStackTrace();
			Thread.interrupted();
		}
		
		return gpxFile;
	}
	
	/*
	 * Copy the fully formed string 
	 */
	public String copyGpxToDirectory(String gpxFile){
		File file = new File(gpxFile);
		String name = file.getName();

        // Destination directory
        File gpxDir = new File(GpssyncConfig.getConfig().getGpxDirectoryPath());

        try {
            FileUtils.copyFile(file, new File(gpxDir, name));
        } catch (IOException e) {
            System.err.println("Error copying gpx to desired directory.");
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        
        return name;
	}
	
}
