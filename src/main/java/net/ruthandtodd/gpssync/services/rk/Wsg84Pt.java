package net.ruthandtodd.gpssync.services.rk;

public class Wsg84Pt {
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
