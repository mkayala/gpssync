package net.ruthandtodd.gpssync.io;

import net.ruthandtodd.gpssync.GpssyncConfig;
import net.ruthandtodd.gpssync.model.Activity;
import net.ruthandtodd.gpssync.model.Model;
import net.ruthandtodd.gpssync.model.User;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.*;
import java.util.*;

public class Storage {

    public static final String FIELD_SPLIT = ",";
    public static final String WITHIN_SPLIT_REG = "\\|";
    public static final String WITHIN_SPLIT = "|";

    public static final String UNKNOWN = "**&FlkeaafLKNNNadontusethisstring;ljajjfuf87a666666";

    private static final DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy/MM/dd HH:mm:ss");

    public static Map<String, User> readUsers() {
        Map<String, User> users = new HashMap<String, User>();
        try {
            BufferedReader fileReader = new BufferedReader(
                    new FileReader(GpssyncConfig.getConfig().getAccountsFilePath()));
            String sRead = null;
            do {
                sRead = fileReader.readLine();

                String[] split = pad(splitSafely(sRead), 4);
                if (split != null) {
                    String name = split[0];
                    String rkauth = split[1];
                    String stu = split[2];
                    String stp = split[3];
                    users.put(name, new User(name, rkauth, stu, stp));
                }
            }
            while (sRead != null);
        } catch (Exception e) {
            System.err.println("Error reading users.\n");
            e.printStackTrace();
        }
        return users;
    }

    public static void writeUsers(Collection<User> users) {
        Writer outFile = null;
        try {
            outFile = new BufferedWriter(new FileWriter(new File(GpssyncConfig.getConfig().getAccountsFilePath())));
            for (User user : users) {
                outFile.append(csvLine(user.getName(), user.getRunkeeperAuth(),
                        user.getStravaEmail(), user.getStravaPass()));
            }
        } catch (Exception e) {
            System.err.println("Error writing users.\n");
            e.printStackTrace();
        } finally {
            if (outFile != null)
                try {
                    outFile.close();
                } catch (IOException e) {

                }
        }
    }

    public static Set<Activity> readActivities(Model model) {
        Set<Activity> activities = new HashSet<Activity>();
        try {
            BufferedReader fileReader = new BufferedReader(
                    new FileReader(GpssyncConfig.getConfig().getActivitiesFilePath()));
            String sRead = null;
            do {
                sRead = fileReader.readLine();
                String[] split = pad(splitSafely(sRead), 7);
                if (split != null) {
                    String filename = split[1];
                    String type = split[2];
                    Model.ActivityType realType = Model.ActivityType.getType(type);
                    Set<Model.Service> services = new HashSet<Model.Service>();
                    if (!split[3].isEmpty()) {
                        String[] servicesList = split[3].split(WITHIN_SPLIT_REG);
                        for (String s : servicesList) {
                            try {
                                services.add(Model.Service.valueOf(s));
                            } catch (EnumConstantNotPresentException e) {
                                // eh, whatever.
                            }
                        }
                    }
                    Set<User> users = new HashSet<User>();
                    if (!split[4].isEmpty()) {
                        for (String s : split[4].split(WITHIN_SPLIT_REG)) {
                            User userByName = model.getUserByName(s);
                            if (userByName != null)
                                users.add(userByName);
                            else {
                                userByName = new User(s);
                                model.addUser(userByName);
                                users.add(userByName);
                            }
                        }
                    }
                    boolean rkflag = parseBoolean(split[5]);
                    boolean stflag = parseBoolean(split[6]);
                    Activity activity = new Activity(realType, filename, rkflag, stflag);
                    activity.addParticipants(users);
                    activity.addServices(services);
                    activities.add(activity);
                }
            }
            while (sRead != null);
        } catch (Exception e) {
            System.err.println("Error reading activities.\n");
            e.printStackTrace();
        }
        return activities;
    }

    private static String[] splitSafely(String sRead) {
        if (sRead == null || sRead.isEmpty())
            return null;
        while (sRead.contains(FIELD_SPLIT + FIELD_SPLIT))
            sRead = sRead.replaceAll(FIELD_SPLIT + FIELD_SPLIT, FIELD_SPLIT + UNKNOWN + FIELD_SPLIT);
        String[] split = sRead.split(FIELD_SPLIT);
        for (int i = 0; i < split.length; i++) {
            if (split[i].equals(UNKNOWN))
                split[i] = "";
        }
        return split;
    }

    private static String[] pad(String[] in, int need) {
        if (in != null && in.length < need) {
            String[] out = new String[need];
            for (int i = 0; i < need; i++) {
                if (i < in.length)
                    out[i] = in[i];
                else
                    out[i] = "";
            }
            return out;
        } else
            return in;
    }


    public static void writeActivities(Collection<Activity> activitiesIn) {
        List<Activity> activities = new ArrayList<Activity>(activitiesIn.size());
        activities.addAll(activitiesIn);
        Collections.sort(activities, new Comparator<Activity>() {
            @Override
            public int compare(Activity o1, Activity o2) {
                return o1.getStartTime().compareTo(o2.getStartTime());
            }
        });
        Writer outFile = null;
        try {
            outFile = new BufferedWriter(new FileWriter(new File(GpssyncConfig.getConfig().getActivitiesFilePath())));
            for (Activity activity : activities) {
                Set<String> userNames = new HashSet<String>();
                for (User u : activity.getParticipants())
                    userNames.add(u.getName());
                outFile.write(csvLine(
                        activity.getStartTime().toString(fmt),
                        // want to figure out how to set it
                        activity.getGpxFilename(),
                        activity.getType(),
                        sepList(WITHIN_SPLIT, activity.getExistsAt().toArray()),
                        sepList(WITHIN_SPLIT, userNames.toArray()),
                        formatBoolean(activity.markedForRunkeeper()),
                        formatBoolean(activity.markedForStrava())));
            }
        } catch (Exception e) {
            System.err.println("Error writing activities.\n");
            e.printStackTrace();
        } finally {
            if (outFile != null)
                try {
                    outFile.close();
                } catch (IOException e) {

                }
        }
    }

    private static String sepList(String sep, Object[] stuff) {
        if (stuff == null || stuff.length == 0)
            return "";
        String r = stuff[0].toString();
        if (stuff.length > 1) {
            for (Object o : Arrays.asList(stuff).subList(1, stuff.length)) {
                r += sep + o.toString();
            }
        }
        return r;
    }

    private static String formatBoolean(boolean b) {
        return b ? "1" : "";
    }

    private static String csvLine(Object... stuff) {
        return sepList(FIELD_SPLIT, stuff) + "\n";
    }

    private static boolean parseBoolean(String s) {
        if (s == null || s.isEmpty() || s.equals("0")) {
            return false;
        }
        if (s.equals("1")) {
            return true;
        }
        return Boolean.parseBoolean(s);
    }

}
