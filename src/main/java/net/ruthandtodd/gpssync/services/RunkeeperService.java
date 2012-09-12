package net.ruthandtodd.gpssync.services;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.divbyzero.gpx.*;
import net.divbyzero.gpx.parser.ParsingException;
import net.ruthandtodd.gpssync.io.GPXWriter;
import net.ruthandtodd.gpssync.model.Activity;
import net.ruthandtodd.gpssync.model.GPXTools;
import net.ruthandtodd.gpssync.model.Model;
import net.ruthandtodd.gpssync.model.User;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

public class RunkeeperService {

    // some constant arguments.
    public static final String responseType = "code";
    public static final String grantType = "authorization_code";

    // url bits
    public static final String authBaseUrl = "https://runkeeper.com/apps/";
    public static final String tokenPath = "token";
    public static final String authorizePath = "authorize";

    public static final String apiBasePath = "http://api.runkeeper.com";
    public static final String userPath = "/user";
    public static final String fitness_activities = "fitness_activities";

    private static final String ENCODING = "UTF-8";

    private static Map<Model.ActivityType, String> typeMap = new HashMap<Model.ActivityType, String>();

    static {
        typeMap.put(Model.ActivityType.RUN, "Running");
        typeMap.put(Model.ActivityType.BIKE, "Cycling");
        typeMap.put(Model.ActivityType.HIKE, "Hiking");
        typeMap.put(Model.ActivityType.NONE, "Other");
    }

    public class RunkeeperAccount {
        private String username;
        private String password;
    }

    public static class RunkeeperConfig {
        // app-specific bits
        public static final String clientId = "bb4a4bba709b473d831fc0b35ef13747";
        public static final String secret = "d41618944e414c6e8fc4b00331cf2dd4";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RunkeeperFitnessPage {
        int size;
        FitnessItem[] items;
        String previous;
        String next;

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public FitnessItem[] getItems() {
            return items;
        }

        public void setItems(FitnessItem[] items) {
            this.items = items;
        }

        public String getPrevious() {
            return previous;
        }

        public void setPrevious(String previous) {
            this.previous = previous;
        }

        public String getNext() {
            return next;
        }

        public void setNext(String next) {
            this.next = next;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class FitnessItem {
        String uri;

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }
    }


    public static String getTokenUri(String code, String callbackUri) {
        String encodedUrl = callbackUri;
        try {
            encodedUrl = URLEncoder.encode(callbackUri, ENCODING);
        } catch (UnsupportedEncodingException e) {
            // noop, go with what we got and hope for the best.
        }
        return authBaseUrl + tokenPath
                + "?grant_type=" + RunkeeperService.grantType
                + "&code=" + code
                + "&client_id=" + RunkeeperService.RunkeeperConfig.clientId
                + "&client_secret=" + RunkeeperService.RunkeeperConfig.secret
                + "&redirect_uri=" + encodedUrl;
    }

    public static String getAuthorizationUri(String callbackUri) {
        String encodedUrl = callbackUri;
        try {
            encodedUrl = URLEncoder.encode(callbackUri, ENCODING);
        } catch (UnsupportedEncodingException e) {
            // noop, go with what we got and hope for the best.
        }
        return authBaseUrl + authorizePath
                + "?redirect_uri=" + encodedUrl
                + "&client_id=" + RunkeeperService.RunkeeperConfig.clientId
                + "&response_type=" + RunkeeperService.responseType;
    }

    public String getFirstPageOfFitnessActivities(User user) throws IOException {
        HttpGet get = new HttpGet(apiBasePath + userPath);
        get.setHeader("Authorization", "Bearer " + user.getRunkeeperAuth());
        HttpClient httpclient = new DefaultHttpClient();
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String userGetResult = httpclient.execute(get, responseHandler);
        JsonNode rootNode = new ObjectMapper().readValue(userGetResult, JsonNode.class);
        return rootNode.get(fitness_activities).asText();
    }

    public void downloadGpxFromRunkeeper(User user){
        List<GpxToJsonThing> activitiesForUser = getActivitiesForUser(user, -1);
        for(GpxToJsonThing thing : activitiesForUser){
            GPX gpx = wsg84ToGPS(thing);
            GPXWriter.writeGpxDateBasedName(gpx);
        }

    }

    public List<GpxToJsonThing> getActivitiesForUser(User user, int max) {
        List<GpxToJsonThing> retVal = new LinkedList<GpxToJsonThing>();
        try {
            String fitnessUri = getFirstPageOfFitnessActivities(user);
            HttpGet get = new HttpGet(apiBasePath + fitnessUri);
            get.setHeader("Authorization", "Bearer " + user.getRunkeeperAuth());
            get.setHeader("Accept", "*/*");

            HttpClient httpclient = new DefaultHttpClient();
            ResponseHandler<String> responseHandler = new BasicResponseHandler();

            String responseBody = httpclient.execute(get, responseHandler);

            ObjectMapper mapper = new ObjectMapper();
            RunkeeperFitnessPage runkeeperFitnessPage = mapper.readValue(responseBody, RunkeeperFitnessPage.class);
            do {
                for (FitnessItem item : runkeeperFitnessPage.items) {
                    get = new HttpGet(apiBasePath + item.uri);
                    get.setHeader("Authorization", "Bearer " + user.getRunkeeperAuth());
                    get.setHeader("Accept", "*/*");

                    responseBody = httpclient.execute(get, responseHandler);
                    if (max <= 0 || retVal.size() < max) {
                        retVal.add(mapper.readValue(responseBody, GpxToJsonThing.class));
                    }
                }
                if (max > 0 && retVal.size() >= max) {
                    break;
                }
                // get the next page
                get = new HttpGet(apiBasePath + runkeeperFitnessPage.next);
                get.setHeader("Authorization", "Bearer " + user.getRunkeeperAuth());
                get.setHeader("Accept", "*/*");
                responseBody = httpclient.execute(get, responseHandler);
                runkeeperFitnessPage = mapper.readValue(responseBody, RunkeeperFitnessPage.class);
            } while (runkeeperFitnessPage != null && runkeeperFitnessPage.next != null);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return retVal;
    }

    public boolean uploadTo(User user, Activity activity) {
        boolean success = false;
        try {
            GPX gpx = activity.getAsGpx();
            GpxToJsonThing thing = gpxToWsg84(gpx);
            thing.setType(typeMap.get(activity.getType()));

            ObjectMapper mapper = new ObjectMapper();
            StringWriter stringWriter = new StringWriter();
            try {
                mapper.writeValue(stringWriter, thing);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String jsonText = stringWriter.toString();

            String fitnessUri = getFirstPageOfFitnessActivities(user);

            HttpPost post = new HttpPost(apiBasePath + fitnessUri); //userPath + fitnesActivities
            post.setEntity(new StringEntity(jsonText));

            post.setHeader("Content-Type", "application/vnd.com.runkeeper.NewFitnessActivity+json");
            post.setHeader("Authorization", "Bearer " + user.getRunkeeperAuth());
            post.setHeader("Accept", "*/*");

            HttpClient httpclient = new DefaultHttpClient();
            ResponseHandler<String> responseHandler = new BasicResponseHandler();

            String responseBody = httpclient.execute(post, responseHandler);
            System.out.println("Response from Runkeeper: \n" + responseBody);

            if (!activity.hasParticipant(user))
                activity.addParticipant(user);
            activity.addServiceKnows(Model.Service.RUNKEEPER);
            activity.setRkUploadFlag(false);
            success = true;
        } catch (ParsingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return success;
    }

    public static Duration noTwoWithin = new Duration(1000);

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GpxToJsonThing {
        Wsg84Pt[] path;
        String start_time;
        double duration;
        String type;
        String notes = "";

        public GpxToJsonThing() {
        }

        public GpxToJsonThing(Wsg84Pt[] pathString, DateTime startTime, double duration) {
            this.path = pathString;
            this.start_time = fmt.print(startTime);
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
    }

    public static class Wsg84Pt {
        double timestamp;
        String type;
        double latitude;
        double longitude;
        double altitude;

        public Wsg84Pt() {
        }

        public Wsg84Pt(double timestamp, String type, double latitude, double longitude, double altitude) {
            this.timestamp = timestamp;
            this.type = type;
            this.latitude = latitude;
            this.longitude = longitude;
            this.altitude = altitude;
        }

        public double getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(double timestamp) {
            this.timestamp = timestamp;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }

        public double getAltitude() {
            return altitude;
        }

        public void setAltitude(double altitude) {
            this.altitude = altitude;
        }
    }


    private static DateTimeFormatter fmt = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss");
    public static GPX wsg84ToGPS(GpxToJsonThing jsonThing) {
        GPX gpx = new GPX();
        Track track = new Track();
        TrackSegment currentSegment = new TrackSegment();
        track.addSegment(currentSegment);
        DateTime start = DateTime.parse(jsonThing.start_time, fmt);
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

    public static GpxToJsonThing gpxToWsg84(GPX gpx) {
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

}
