package net.ruthandtodd.gpssync.model;

import net.divbyzero.gpx.GPX;
import net.ruthandtodd.gpssync.io.GPXWriter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GPXTools {

    public static Date getDateForGpx(DateTime time) {
        try {
            // fuck Date and fuck jgpx for depending on it.
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(time.toString(GPXWriter.gpxTimeFmt));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static DateTime getUtcDateTimeFromGpx(Date date){
        return new DateTime(date).withZoneRetainFields(DateTimeZone.UTC);
    }

    public static DateTime getStartTime(GPX gpx){
        return getUtcDateTimeFromGpx(gpx.getTracks().get(0).startingTime());
    }

}
