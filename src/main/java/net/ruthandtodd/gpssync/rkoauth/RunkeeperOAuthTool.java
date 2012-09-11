package net.ruthandtodd.gpssync.rkoauth;

import net.ruthandtodd.gpssync.model.Model;
import net.ruthandtodd.gpssync.model.User;

public class RunkeeperOAuthTool {

    public static String retrieveOAuthToken() throws Exception {
        RunkeeperOAuthClient client = new RunkeeperOAuthClient();
        String accessToken = client.getAccessToken();
        return accessToken;
    }

    public static void main(String... args) throws Exception {

        String username = args[0];
        User userByName = Model.getModel().getUserByName(username);
        if(userByName == null){
            userByName = new User(username);
            boolean success = Model.getModel().addUser(userByName);
            if(!success){
                System.out.println("Usernames cannot contain , or |");
                System.exit(0);
            }
        }

        int seconds = 7;
        System.out.println("Going to wait " + seconds + " seconds and then get a Runkeeper authorization for user " + username);
        System.out.println(username + " needs to be the user currently logged in to Runkeeper. \n"
                + "If he/she is not, press Ctrl+C, fix that, and run this again.");
        for (int i = seconds; i > 0; i--) {
            System.out.println(i + " seconds until we do this.");
            Thread.sleep(1000);
        }
        String accessToken = retrieveOAuthToken();

        userByName.setRunKeeperAuth(accessToken);

        Model.getModel().save();
    }
}
