package net.ruthandtodd.gpssync.devices;


import java.util.List;

public interface DeviceInterface {

    /**
     * Interface with some device, get recent activities, convert them to GPX if necessary, move them
     * to the GPX directory, and return a list of their names.
     * @return
     */
      public List<String> getNewestActivities();
}
