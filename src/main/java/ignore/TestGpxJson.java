package ignore;


import com.fasterxml.jackson.databind.ObjectMapper;
import net.divbyzero.gpx.GPX;
import net.divbyzero.gpx.parser.JDOM;
import net.divbyzero.gpx.parser.ParsingException;
import net.ruthandtodd.gpssync.services.RunkeeperService;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

public class TestGpxJson {

    public static void main(String... args) throws ParsingException {
        GPX gpx = new JDOM().parse(new File("/home/todd/runs/gpx/2012-09-08-213742.gpx"));
        RunkeeperService.GpxToJsonThing thing = RunkeeperService.gpxToWsg84(gpx);
        thing.setType("Running");

        ObjectMapper mapper = new ObjectMapper();
        StringWriter stringWriter = new StringWriter();
        try {
            mapper.writeValue(stringWriter, thing);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String jsonText = stringWriter.toString();
        System.out.println(jsonText);
    }

}
