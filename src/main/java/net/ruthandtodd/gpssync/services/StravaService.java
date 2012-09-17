package net.ruthandtodd.gpssync.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import net.ruthandtodd.gpssync.io.FileUtils;
import net.ruthandtodd.gpssync.model.Activity;
import net.ruthandtodd.gpssync.model.Model;
import net.ruthandtodd.gpssync.model.User;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class StravaService {

    public static final String baseUrl = "http://www.strava.com/api/v2";
    public static final String loginUrl = "/authentication/login";
    public static final String token = "token";
    public static final String upload = "/upload";

    private static final String ENCODING = "UTF-8";

    private static BiMap<Model.ActivityType, String> typeMap =
            ImmutableBiMap.of(
                    Model.ActivityType.RUN, "run",
                    Model.ActivityType.BIKE, "ride",
                    Model.ActivityType.HIKE, "hike",
                    Model.ActivityType.NONE, "other",
                    Model.ActivityType.WALK, "walk"
            );

    public boolean uploadTo(User user, Activity activity) {
        boolean success = false;
        String email = user.getStravaEmail();
        if (email != null && !email.isEmpty()) {
            String token = getAuthToken(user);
            String fileText = FileUtils.readFileToString(activity);
            HttpClient httpclient = new DefaultHttpClient();
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            HttpPost post = new HttpPost(baseUrl + upload);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("token", token));
            try {
                nameValuePairs.add(new BasicNameValuePair("data", fileText));
                nameValuePairs.add(new BasicNameValuePair("type", "gpx"));
                nameValuePairs.add(new BasicNameValuePair("activity_type", typeMap.get(activity.getType())));
                post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                String responseBody = httpclient.execute(post, responseHandler);

                System.out.println("Response from Strava: \n" + responseBody);

                if (!activity.hasParticipant(user))
                    activity.addParticipant(user);
                activity.addServiceKnows(Model.Service.STRAVA);
                activity.setStravaUploadFlag(false);
                success = true;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return success;
    }

    private String getAuthToken(User user) {
        HttpClient httpclient = new DefaultHttpClient();
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        HttpPost post = new HttpPost(baseUrl + loginUrl);
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("email", user.getStravaEmail()));
        nameValuePairs.add(new BasicNameValuePair("password", user.getStravaPass()));
        try {
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            String responseBody = httpclient.execute(post, responseHandler);
            JsonNode rootNode = new ObjectMapper().readValue(responseBody, JsonNode.class);
            return rootNode.get(token).asText();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
