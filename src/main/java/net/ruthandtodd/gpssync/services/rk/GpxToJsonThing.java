package net.ruthandtodd.gpssync.services.rk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.divbyzero.gpx.*;
import net.ruthandtodd.gpssync.model.GPXTools;
import net.ruthandtodd.gpssync.services.TimeZoneService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GpxToJsonThing {
    Wsg84Pt[] path;
    String start_time;
    double duration;
    String type;
    String notes = "";

    public GpxToJsonThing() {
    }

    public GpxToJsonThing(Wsg84Pt[] pathString, DateTime startTime, double duration) {
        this.path = pathString;
        this.start_time = RunkeeperService.RUNKEEPER_TIME_FORMAT.print(startTime);
        this.duration = duration;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Wsg84Pt[] getPath() {
        return path;
    }

    public void setPath(Wsg84Pt[] path) {
        this.path = path;
    }

    public String getStart_time() {
        return start_time;
    }

    public void setStart_time(String start_time) {
        this.start_time = start_time;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public static GpxToJsonThing fromGpx(GPX gpx) {
        DateTime startTime = null;
        List<Wsg84Pt> pathString = new LinkedList<Wsg84Pt>();

        boolean started = false;
        boolean paused = false;

        boolean lastSegment = false;
        boolean lastTrack = false;

        ArrayList<Track> tracks = gpx.getTracks();
        double secondsSinceStart = 0;
        for (int trackCounter = 0; trackCounter < tracks.size(); trackCounter++) {
            Track track = tracks.get(trackCounter);
            if (trackCounter == tracks.size() - 1) {
                lastTrack = true;
            }
            ArrayList<TrackSegment> segments = track.getSegments();
            for (int segmentCount = 0; segmentCount < segments.size(); segmentCount++) {
                TrackSegment segment = segments.get(segmentCount);
                if (lastTrack && segmentCount == segments.size() - 1) {
                    lastSegment = true;
                }
                ArrayList<Waypoint> waypoints = segment.getWaypoints();
                for (int pointCount = 0; pointCount < waypoints.size(); pointCount++) {
                    Waypoint point = waypoints.get(pointCount);
                    boolean lastPoint = pointCount == waypoints.size();
                    double ele = point.getElevation();
                    double lat = point.getCoordinate().getLatitude();
                    double lon = point.getCoordinate().getLongitude();

                    if (startTime != null)
                        secondsSinceStart = new Duration(startTime, GPXTools.getUtcDateTimeFromGpx(point.getTime())).getMillis() / 1000d;
                    if (!started) {
                        started = true;
                        startTime = GPXTools.getUtcDateTimeFromGpx(point.getTime());
                        pathString.add(new Wsg84Pt(secondsSinceStart, "start", lat, lon, ele));
                    } else if (paused) {
                        pathString.add(new Wsg84Pt(secondsSinceStart, "resume", lat, lon, ele));
                        paused = false;
                    }
                    pathString.add(new Wsg84Pt(secondsSinceStart, "gps", lat, lon, ele));
                    if (lastPoint && !lastSegment) {
                        pathString.add(new Wsg84Pt(secondsSinceStart, "pause", lat, lon, ele));
                        paused = true;
                    } else if (lastPoint) {
                        pathString.add(new Wsg84Pt(secondsSinceStart, "end", lat, lon, ele));
                    }
                }
            }

        }
        DateTimeZone localTimeZone = TimeZoneService.getDateTimeZone(pathString.get(0).getLatitude(), pathString.get(0).getLongitude());
        startTime = startTime.withZone(localTimeZone);
        return new GpxToJsonThing(pathString.toArray(new Wsg84Pt[pathString.size()]), startTime, secondsSinceStart);
    }

    public static GPX toGpx(GpxToJsonThing jsonThing) {
        GPX gpx = new GPX();
        Track track = new Track();
        TrackSegment currentSegment = new TrackSegment();
        track.addSegment(currentSegment);
        DateTime start = DateTime.parse(jsonThing.start_time, RunkeeperService.RUNKEEPER_TIME_FORMAT);
        DateTimeZone timeZone = TimeZoneService.getDateTimeZone(jsonThing.getPath()[0].getLatitude(), jsonThing.getPath()[0].getLongitude());
        start = start.withZoneRetainFields(timeZone);
        start = start.withZone(DateTimeZone.UTC);

        for (Wsg84Pt pt : jsonThing.getPath()) {
            Waypoint waypoint = new Waypoint();
            Coordinate coordinate = new Coordinate();
            coordinate.setLatitude(pt.latitude);
            coordinate.setLongitude(pt.longitude);
            waypoint.setCoordinate(coordinate);
            waypoint.setElevation(pt.getAltitude());
            waypoint.setTime(GPXTools.getDateForGpx(start.plusMillis((int) Math.round(1000 * pt.getTimestamp()))));
            currentSegment.addWaypoint(waypoint);
            if (pt.getType().equals("pause")) {
                currentSegment = new TrackSegment();
                track.addSegment(currentSegment);
            }
        }
        gpx.addTrack(track);
        return gpx;
    }
}
