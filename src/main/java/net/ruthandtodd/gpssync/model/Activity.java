package net.ruthandtodd.gpssync.model;

import net.divbyzero.gpx.GPX;
import net.divbyzero.gpx.parser.JDOM;
import net.divbyzero.gpx.parser.ParsingException;
import net.ruthandtodd.gpssync.GpssyncConfig;
import org.joda.time.DateTime;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Activity {
    private DateTime startTime;
    private Model.ActivityType type;
    private Set<User> participants;
    private Set<Model.Service> existsAt;
    private String filenameOfGpx;
    private boolean flaggedForUpToRunkeeper;
    private boolean flaggedForUpToStrava;

    public Activity(Model.ActivityType type,
                    String filenameOfGpx, boolean flaggedForUpToRunkeeper, boolean flaggedForUpToStrava) {

        this.type = type;
        this.participants = new HashSet<User>();
        this.existsAt = new HashSet<Model.Service>();
        this.filenameOfGpx = filenameOfGpx;
        this.flaggedForUpToRunkeeper = flaggedForUpToRunkeeper;
        this.flaggedForUpToStrava = flaggedForUpToStrava;
        try {
            this.startTime = GPXTools.getStartTime(getAsGpx());
        } catch (ParsingException e) {
            e.printStackTrace();
        }
    }

    public DateTime getStartTime() {
        return startTime;
    }

    public Model.ActivityType getType() {
        return type;
    }

    public void addParticipants(Collection<User> someUsers) {
        participants.addAll(someUsers);
    }

    public void addServices(Collection<Model.Service> s) {
        existsAt.addAll(s);
    }

    public boolean hasParticipant(User user) {
        return false;
    }

    public void addParticipant(User user) {
        participants.add(user);
    }

    public void addServiceKnows(Model.Service b) {
        existsAt.add(b);
    }

    public void setRkUploadFlag(boolean b) {
        flaggedForUpToRunkeeper = b;
    }

    public void setStravaUploadFlag(boolean b) {
        flaggedForUpToStrava = b;
    }

    public String getGpxFilename() {
        return filenameOfGpx;
    }

    public boolean markedForRunkeeper() {
        return flaggedForUpToRunkeeper;
    }

    public boolean markedForStrava() {
        return flaggedForUpToStrava;
    }

    public Set<User> getParticipants() {
        return participants;
    }

    public Set<Model.Service> getExistsAt() {
        return existsAt;
    }

    public GPX getAsGpx() throws ParsingException {
        String fullname = GpssyncConfig.getConfig().getGpxDirectoryPath() + filenameOfGpx;
        return new JDOM().parse(new File(fullname));
    }
}
