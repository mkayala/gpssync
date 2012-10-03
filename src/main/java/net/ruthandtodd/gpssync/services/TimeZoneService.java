package net.ruthandtodd.gpssync.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ruthandtodd.gpssync.GpssyncConfig;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.joda.time.DateTimeZone;

import java.io.IOException;

public class TimeZoneService {

    public static DateTimeZone getDateTimeZone(double lat, double lon) {
        try {
            String tz = getTimeZone(lat, lon);
            return DateTimeZone.forID(tz);
        } catch (IOException e) {
            e.printStackTrace();
            return null; // maybe should default to UTC?
        }
    }

    public static String getTimeZone(double lat, double lon) throws IOException {
        String request = "http://where.yahooapis.com/geocode?location=" + lat + "," + lon + "&flags=TJ&gflags=R";
        System.out.println(request);
        HttpGet get = new HttpGet(request);
        HttpClient httpclient = new DefaultHttpClient();
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        JsonNode resultNode;
        try {
            String userGetResult = httpclient.execute(get, responseHandler);
            JsonNode rootNode = new ObjectMapper().readValue(userGetResult, JsonNode.class);
            JsonNode resultSetNode = rootNode.get("ResultSet");

            if (resultSetNode.has("Results")) {
                resultNode = resultSetNode.get("Results").get(0);
            } else if (resultSetNode.has("Result")) {
                resultNode = resultSetNode.get("Result");
            } else {
                System.out.println("you're about to get an npe, homie.");
                resultNode = null;
            }
        } catch (HttpResponseException e) {
            resultNode = null;
        }
        if (resultNode != null && resultNode.has("timezone"))
            return resultNode.get("timezone").asText();
        else
            return GpssyncConfig.getConfig().getDefaultTimezone();

    }

    public static void main(String... args) {
        System.out.println(getDateTimeZone(45.51, -122.70));
        System.out.println(getDateTimeZone(33, -125));
    }

}
