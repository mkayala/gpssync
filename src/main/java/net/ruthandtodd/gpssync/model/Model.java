package net.ruthandtodd.gpssync.model;

import net.ruthandtodd.gpssync.io.Storage;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.*;

public class Model {

    public enum Service {
        RUNKEEPER, STRAVA
    }

    public enum ActivityType {
        RUN("RUN"), BIKE("BIKE"), HIKE("HIKE"), NONE("NONE");

        private final String name;

        private ActivityType(String name) {
            this.name = name;
        }

        public static ActivityType getType(String value) {
            if (value == null || value.isEmpty())
                return NONE;
            return ActivityType.valueOf(value);
        }
    }

    Map<String, User> users = new HashMap<String, User>();
    Map<User, List<Activity>> activitiesByUser;
    Set<Activity> allActivities;

    private Model() {
        loadUsers();
        loadActivities();
    }

    private void loadUsers() {
        users = Storage.readUsers();
    }

    private void loadActivities() {
        allActivities = Storage.readActivities(this);
        activitiesByUser = new HashMap<User, List<Activity>>();
        for (Activity activity : allActivities) {
            for (User user : activity.getParticipants()) {
                if (!activitiesByUser.containsKey(user)) {
                    activitiesByUser.put(user, new LinkedList<Activity>());
                }
                activitiesByUser.get(user).add(activity);
            }
        }
    }

    public void save() {
        Storage.writeUsers(users.values());
        Storage.writeActivities(allActivities);
    }

    private static Model instance;

    public static Model getModel() {
        if (instance == null) {
            instance = new Model();
        }
        return instance;
    }

    public User getUserByName(String name) {
        if (users.containsKey(name)) {
            return users.get(name);
        }
        return null;
    }

    public boolean addUser(User newUser) {
        if (users.containsKey(newUser.getName())
                || newUser.getName().contains(Storage.FIELD_SPLIT)
                || newUser.getName().contains(Storage.WITHIN_SPLIT)
                || newUser.getName().equals(Storage.UNKNOWN)) {
            return false;
        } else {
            users.put(newUser.getName(), newUser);
            return true;
        }
    }

    public Activity addActivityForUser(User user, String fileName, Model.ActivityType type) {
        Activity activity = addActivityNoUser(fileName, type);
        activity.addParticipant(user);
        if (!activitiesByUser.containsKey(user))
            activitiesByUser.put(user, new LinkedList<Activity>());
        activitiesByUser.get(user).add(activity);
        return activity;
    }

    public Activity addActivityNoUser(String fileName, Model.ActivityType type) {
        Activity activity = new Activity(type, fileName, false, false);
        allActivities.add(activity);
        return activity;
    }

    public boolean haveActivityWithFilename(String someFile) {
        boolean have = false;
        for (Activity a : allActivities) {
            if (a.getGpxFilename().equals(someFile)) {
                have = true;
                break;
            }
        }
        return have;
    }

    public boolean haveActivityWithin(DateTime time, Duration within) {
        for (Activity a : allActivities) {
            DateTime first = time.isBefore(a.getStartTime()) ? time : a.getStartTime();
            DateTime second = time.isBefore(a.getStartTime()) ? a.getStartTime() : time;
            if (within.isLongerThan(new Duration(first, second))) {
                System.out.println(first + " too close to " + second);
                return true;
            }
        }
        return false;
    }

    public Set<Activity> getAllActivities() {
        return allActivities;
    }
}
