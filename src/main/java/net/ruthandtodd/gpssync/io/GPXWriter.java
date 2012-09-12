package net.ruthandtodd.gpssync.io;

import net.divbyzero.gpx.GPX;
import net.divbyzero.gpx.Track;
import net.divbyzero.gpx.TrackSegment;
import net.divbyzero.gpx.Waypoint;
import net.ruthandtodd.gpssync.GpssyncConfig;
import net.ruthandtodd.gpssync.model.GPXTools;
import org.jdom.Attribute;
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

    public static DateTimeFormatter gpxTimeFmt = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    public static DateTimeFormatter fileNameFmt = DateTimeFormat.forPattern("yyyyMMdd.HH.mm.ss");

    public static void writeGpxDateBasedName(GPX gpx){
        DateTime time = GPXTools.getUtcDateTimeFromGpx(gpx.getTracks().get(0).getSegments().get(0).getWaypoints().get(0).getTime());
        String fileName = time.toString(fileNameFmt);
        String filePath = GpssyncConfig.getConfig().getGpxDirectoryPath() + fileName;
        writeGpx(filePath, gpx);
    }

    public static void writeGpx(String path, GPX gpx) {
        Element root = new Element("gpx");
        Document doc = new Document(root);

        doc.setRootElement(root);
        Namespace ns = Namespace.getNamespace("http://www.topografix.com/GPX/1/1");
        root.setNamespace(ns);
        root.setAttribute("version", "1.1");
        root.setAttribute("creator", "gpssync");

        Namespace xsiNS =
                Namespace.getNamespace(       "xsi",
                        "http://www.w3.org/2001/XMLSchema-instance");

        root.setAttribute(
                new Attribute("schemaLocation",
                        "http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd",
                        xsiNS));


        for (Track track : gpx.getTracks()) {
            Element currentTrack = new Element("trk", ns);
            for (TrackSegment segment : track.getSegments()) {
                Element currentSegment = new Element("trkseg", ns);
                for (Waypoint waypoint : segment.getWaypoints()) {
                    Element currentWaypoint = new Element("trkpt", ns);
                    currentWaypoint.setAttribute("lat", "" + waypoint.getCoordinate().getLatitude()); // lazy string-casting
                    currentWaypoint.setAttribute("lon", "" + waypoint.getCoordinate().getLongitude());
                    Element ele = new Element("ele", ns);
                    ele.addContent("" + waypoint.getElevation());
                    currentWaypoint.addContent(ele);
                    Date time = waypoint.getTime();
                    if (time != null) {
                        DateTime dt = GPXTools.getUtcDateTimeFromGpx(time);
                        Element timeEl = new Element("time", ns);
                        timeEl.addContent(dt.toString(gpxTimeFmt));
                        currentWaypoint.addContent(timeEl);
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
