package net.ruthandtodd.gpssync.services.rk;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import net.divbyzero.gpx.GPX;
import net.divbyzero.gpx.parser.ParsingException;
import net.ruthandtodd.gpssync.io.GPXWriter;
import net.ruthandtodd.gpssync.model.Activity;
import net.ruthandtodd.gpssync.model.GPXTools;
import net.ruthandtodd.gpssync.model.Model;
import net.ruthandtodd.gpssync.model.User;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;

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

    private static BiMap<Model.ActivityType, String> typeMap =
            ImmutableBiMap.of(
                    Model.ActivityType.RUN, "Running",
                    Model.ActivityType.BIKE, "Cycling",
                    Model.ActivityType.HIKE, "Hiking",
                    Model.ActivityType.NONE, "Other"
            );

    public static class RunkeeperConfig {
        // app-specific bits
        public static final String clientId = "bb4a4bba709b473d831fc0b35ef13747";
        public static final String secret = "d41618944e414c6e8fc4b00331cf2dd4";
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

    public void downloadGpxFromRunkeeper(User user) {
        List<GpxToJsonThing> activitiesForUser = getActivitiesForUser(user, -1);
        for (GpxToJsonThing thing : activitiesForUser) {
            GPX gpx = GpxToJsonThing.toGpx(thing);
            if (!Model.getModel().haveActivityWithin(GPXTools.getStartTime(gpx),
                    noTwoWithin)) {
                System.out.println(GPXTools.getStartTime(gpx));
                Optional<String> newFilename = GPXWriter.writeGpxDateBasedName(gpx);
                if (newFilename.isPresent()) {
                    String runKeeperType = thing.getType();
                    Model.ActivityType type = Model.ActivityType.NONE;
                    if (typeMap.inverse().containsKey(runKeeperType)) {
                        type = typeMap.inverse().get(runKeeperType);
                    }
                    Activity activity = Model.getModel().addActivityForUser(user, newFilename.get(), type);
                    activity.addServiceKnows(Model.Service.RUNKEEPER);
                } else {
                    System.out.println("Error writing to file. :(");
                }
                System.out.println("...");
            }
        }
    }

    public List<GpxToJsonThing> getActivitiesForUser(User user, int max) {
        List<GpxToJsonThing> retVal = new LinkedList<GpxToJsonThing>();
        try {
            String fitnessUri = getFirstPageOfFitnessActivities(user);


            HttpClient httpclient = new DefaultHttpClient();
            ResponseHandler<String> responseHandler = new BasicResponseHandler();

            String responseBody = getFromRunkeeperApi(fitnessUri, user, httpclient, responseHandler);

            System.out.println(responseBody);

            ObjectMapper mapper = new ObjectMapper();
            RunkeeperFitnessPage runkeeperFitnessPage = mapper.readValue(responseBody, RunkeeperFitnessPage.class);
            do {
                for (FitnessItem item : runkeeperFitnessPage.items) {
                    responseBody = getFromRunkeeperApi(item.getUri(), user, httpclient, responseHandler);
                    if (responseBody == null) {
                        System.out.println("We seem to be unable to retrieve " + item.toString());
                    } else if (max <= 0 || retVal.size() < max) {
                        retVal.add(mapper.readValue(responseBody, GpxToJsonThing.class));
                    }
                }
                if (max > 0 && retVal.size() >= max) {
                    break;
                }
                // get the next page
                responseBody = getFromRunkeeperApi(runkeeperFitnessPage.next, user, httpclient, responseHandler);
                runkeeperFitnessPage = mapper.readValue(responseBody, RunkeeperFitnessPage.class);
            } while (runkeeperFitnessPage != null && runkeeperFitnessPage.next != null);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return retVal;
    }

    private String getFromRunkeeperApi(String path, User user, HttpClient httpclient, ResponseHandler<String> responseHandler) throws IOException, InterruptedException {
        HttpGet get = new HttpGet(apiBasePath + path);
        System.out.println(apiBasePath + path);
        get.setHeader("Authorization", "Bearer " + user.getRunkeeperAuth());
        get.setHeader("Accept", "*/*");
        boolean done = false;
        int retries = 0;
        String responseBody = null;
        while (!done) {
            try {
                responseBody = httpclient.execute(get, responseHandler);
                done = true;
            } catch (HttpResponseException hpe) {
                int statusCode = hpe.getStatusCode();
                if (!(statusCode == 500)) {
                    done = true;
                    hpe.printStackTrace();
                } else {
                    System.out.println("going to retry in three seconds ... ");
                    Thread.sleep(3000);
                }
                if (retries++ >= 3)
                    done = true;
                else System.out.println(".");
            }
        }
        return responseBody;
    }

    public boolean uploadTo(User user, Activity activity) {
        boolean success = false;
        try {
            GPX gpx = activity.getAsGpx();
            GpxToJsonThing thing = GpxToJsonThing.fromGpx(gpx);
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

    public static final DateTimeFormatter RUNKEEPER_TIME_FORMAT = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss");

}
