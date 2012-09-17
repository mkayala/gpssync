package net.ruthandtodd.gpssync.runner;

import com.google.common.base.Optional;
import net.divbyzero.gpx.GPX;
import net.divbyzero.gpx.parser.JDOM;
import net.divbyzero.gpx.parser.ParsingException;
import net.ruthandtodd.gpssync.io.FileUtils;
import net.ruthandtodd.gpssync.io.GPXWriter;
import net.ruthandtodd.gpssync.model.Activity;
import net.ruthandtodd.gpssync.model.GPXTools;
import net.ruthandtodd.gpssync.model.Model;
import net.ruthandtodd.gpssync.model.User;
import net.ruthandtodd.gpssync.services.GantGpxGetter;
import net.ruthandtodd.gpssync.services.StravaService;
import net.ruthandtodd.gpssync.services.rk.RunkeeperService;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Runner {


    public static void main(String... args) throws InterruptedException {
        String command = args[0];
        if (command.equals("addAllToUser")) {
            String user = args[1];
            String type = args.length > 2 ? args[2] : "";
            Model.ActivityType aType = getType(type);
            addAllNewGantActivitiesToUser(user, aType);
        } else if (command.equals("addLatestToUser")) {
            String user = args[1];
            String type = args.length > 2 ? args[2] : "";
            Model.ActivityType aType = getType(type);
            Activity activity = addAllNewGantActivitiesAndLastToUser(user, aType);
            activity.setRkUploadFlag(true);
            activity.setStravaUploadFlag(true);
            uploadMarked();
        } else if (command.equals("addFromDirectory")) {
            String directory = args[1];
            File dFile = new File(directory);
            String directoryArg = dFile.getAbsolutePath();
            if (!directoryArg.endsWith("/")) {
                directoryArg += "/";
            }
            List<String> newFiles = FileUtils.unknownGpxInDirectory(directoryArg);
            for (String thing : newFiles) {
                GPX gpx = null;
                try {
                    gpx = new JDOM().parse(new File(directoryArg + thing));
                    if (!Model.getModel().haveActivityWithin(GPXTools.getStartTime(gpx),
                            Model.noTwoWithin)) {
                        Optional<String> newFilename = GPXWriter.writeGpxDateBasedName(gpx, "file");
                        if (newFilename.isPresent()) {


                            Activity activity = Model.getModel().addActivityNoUser(newFilename.get(), Model.ActivityType.NONE);

                        } else {
                            System.out.println("Error writing to file. :(");
                        }

                    } else {

                    }
                } catch (ParsingException e) {
                    e.printStackTrace();
                }

            }

        } else if (command.equals("uploadMarked"))

        {
            uploadMarked();
        } else if (command.equals("downloadFromRunKeeper"))

        {
            String user = args[1];
            downloadFromRunkeeper(user);
        } else

        {
            System.out.println("Not sure what to do with command " + command);
            System.out.println("valid options include: ");
            System.out.println("addAllToUser user [type]");
            System.out.println("addLatestToUser user [type]");
            System.out.println("addFromDirectory path");
        }
        // just in case.
        Model.getModel().

                save();

    }

    private static Model.ActivityType getType(String type) {
        Model.ActivityType aType;
        try {
            aType = Model.ActivityType.valueOf(type.toUpperCase());
        } catch (Exception e) {
            System.out.println("failed to parse type, using run");
            aType = Model.ActivityType.RUN;
        }
        return aType;
    }

    private static void addAllNewGantActivitiesToUser(String username, Model.ActivityType type) {
        List<String> newestActivities = new GantGpxGetter().getNewestActivities();
        addActivitiesToUserAsType(newestActivities, username, type);

    }

    private static Activity addAllNewGantActivitiesAndLastToUser(String username, Model.ActivityType type) {
        Model model = Model.getModel();
        List<String> newestActivities = new GantGpxGetter().getNewestActivities();
        if (newestActivities.size() > 1) {
            Collections.sort(newestActivities);
            List<String> olderActivities = newestActivities.subList(0, newestActivities.size() - 1);
            addActivities(olderActivities, type);
        }
        if (newestActivities.size() > 0) {
            return addActivitiesToUserAsType(
                    newestActivities.subList(newestActivities.size() - 1, newestActivities.size()),
                    username, type).get(0);
        }
        return null;
    }

    private static List<Activity> addActivitiesToUserAsType(List<String> filenames, String username, Model.ActivityType type) {
        Model model = Model.getModel();
        User todd = model.getUserByName(username);
        List<Activity> ret = new LinkedList<Activity>();
        for (String newestActivity : filenames) {
            System.out.println(newestActivity);
            Activity activity = model.addActivityForUser(todd, newestActivity, type);
            ret.add(activity);
        }
        model.save();
        return ret;
    }

    private static List<Activity> addActivities(List<String> filenames, Model.ActivityType type) {
        Model model = Model.getModel();
        List<Activity> ret = new LinkedList<Activity>();
        for (String anactivity : filenames) {
            System.out.println(anactivity);
            Activity activity = model.addActivityNoUser(anactivity, type);
            ret.add(activity);
        }
        model.save();
        return ret;
    }

    private static void uploadMarked() {
        Model model = Model.getModel();
        Set<Activity> allActivities = model.getAllActivities();
        RunkeeperService rks = new RunkeeperService();
        StravaService ss = new StravaService();
        for (Activity activity : allActivities) {
            if (activity.markedForRunkeeper()) {
                for (User user : activity.getParticipants())
                    rks.uploadTo(user, activity);
            }
            if (activity.markedForStrava()) {
                for (User user : activity.getParticipants())
                    ss.uploadTo(user, activity);
            }
        }
        model.save();
    }

    private static void downloadFromRunkeeper(String user) {
        Model model = Model.getModel();
        User me = model.getUserByName(user);
        new RunkeeperService().downloadGpxFromRunkeeper(me);
        model.save();
    }

}
