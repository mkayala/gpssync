package net.ruthandtodd.gpssync.io;

import net.divbyzero.gpx.GPX;
import net.divbyzero.gpx.Track;
import net.divbyzero.gpx.TrackSegment;
import net.divbyzero.gpx.Waypoint;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class GPXWriter {

    private static DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public static void writeGpx(String path, GPX gpx) {
        Element root = new Element("");
        Document doc = new Document(root);

        doc.setRootElement(root);
        Namespace ns = Namespace.getNamespace("http://www.topografix.com/GPX/1/1");
        root.setNamespace(ns);

        for (Track track : gpx.getTracks()) {
            Element currentTrack = new Element("trk", ns);
            for (TrackSegment segment : track.getSegments()) {
                Element currentSegment = new Element("trkseg", ns);
                for (Waypoint waypoint : segment.getWaypoints()) {
                    Element currentWaypoint = new Element("trkpt", ns);
                    currentWaypoint.setAttribute("lat", "" + waypoint.getCoordinate().getLatitude()); // lazy string-casting
                    currentWaypoint.setAttribute("lon", "" + waypoint.getCoordinate().getLongitude());
                    currentWaypoint.setAttribute("ele", "" + waypoint.getElevation());
                    Date time = waypoint.getTime();
                    if (time != null) {
                        DateTime dt = new DateTime(time);
                        currentWaypoint.setAttribute("time", dt.toString(fmt));
                    }
                    currentSegment.addContent(currentWaypoint);
                }
                currentTrack.addContent(currentSegment);
            }
            doc.getRootElement().addContent(currentTrack);
        }

        // new XMLOutputter().output(doc, System.out);
        XMLOutputter xmlOutput = new XMLOutputter();

        // display nice nice
        xmlOutput.setFormat(Format.getPrettyFormat());
        try {
            xmlOutput.output(doc, new FileWriter(path));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
