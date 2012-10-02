package net.ruthandtodd.gpssync.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.HttpClient;
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
        HttpGet get = new HttpGet("http://where.yahooapis.com/geocode?location=" + lat + "," + lon + "&flags=TJ&gflags=R");
        HttpClient httpclient = new DefaultHttpClient();
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String userGetResult = httpclient.execute(get, responseHandler);
        JsonNode rootNode = new ObjectMapper().readValue(userGetResult, JsonNode.class);
        JsonNode resultSetNode = rootNode.get("ResultSet");
        JsonNode resultNode;
        if (resultSetNode.has("Results")) {
            resultNode = resultSetNode.get("Results").get(0);
        } else if (resultSetNode.has("Result")) {
            resultNode = resultSetNode.get("Result");
        } else {
            System.out.println("you're about to get an npe, homie.");
            resultNode = null;
        }
        return resultNode.get("timezone").asText();
    }

}
