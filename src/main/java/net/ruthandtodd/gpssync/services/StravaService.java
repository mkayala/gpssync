package net.ruthandtodd.gpssync.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StravaService {

    public static final String baseUrl = "http://www.strava.com/api/v2";
    public static final String loginUrl = "/authentication/login";
    public static final String token = "token";
    public static final String upload = "/upload";

    private static final String ENCODING = "UTF-8";

    private static Map<Model.ActivityType, String> typeMap = new HashMap<Model.ActivityType, String>();

    static {
        typeMap.put(Model.ActivityType.RUN, "run");
        typeMap.put(Model.ActivityType.BIKE, "ride");
        typeMap.put(Model.ActivityType.HIKE, "hike");
        typeMap.put(Model.ActivityType.NONE, "other");
    }

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
