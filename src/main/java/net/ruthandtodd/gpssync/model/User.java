package net.ruthandtodd.gpssync.model;

public class User {
    private String name;
    private String runKeeperAuth;
    private String stravaEmail;
    private String stravaPass;

    public void setRunKeeperAuth(String runKeeperAuth) {
        this.runKeeperAuth = runKeeperAuth;
    }

    public User(String name) {
        this.name = name;
        stravaPass = "";
        stravaEmail = "";
        runKeeperAuth = "";
    }

    public User(String name, String runKeeperAuth, String stravaEmail, String stravaPass) {
        this.name = name;
        this.runKeeperAuth = runKeeperAuth;
        this.stravaEmail = stravaEmail;
        this.stravaPass = stravaPass;
    }

    public String getStravaEmail() {
        return stravaEmail;
    }

    public String getStravaPass() {
        return stravaPass;
    }

    public String getRunkeeperAuth() {
        return runKeeperAuth;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (name != null ? !name.equals(user.name) : user.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
