/*
 * Based in part on DesktopClient, which was:
 *
 * Copyright 2009 John Kristian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ruthandtodd.gpssync.rkoauth;

import com.centerkey.utils.BareBonesBrowserLaunch;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ruthandtodd.gpssync.services.RunkeeperService;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;

/**
 * @author John Kristian
 */
public class RunkeeperOAuthClient {
    private static final String CALLBACK_PATH = "/";

    private Object lock = new Object();
    private String code;

    public String getAccessToken()
            throws Exception {
        Server server = null;
        try {
            synchronized (lock) {
                final int callbackPort = getEphemeralPort();
                String callbackUri =
                        "http://localhost:" + callbackPort + CALLBACK_PATH;
                if (server == null) {
                    // Start an HTTP rkoauth:

                    server = new Server(callbackPort);
                    for (Connector c : server.getConnectors()) {
                        c.setHost("localhost"); // local clients only
                    }
                    server.setHandler(newCallback());
                    server.start();
                }
                String authorizationURL = RunkeeperService.getAuthorizationUri(callbackUri);
                BareBonesBrowserLaunch.browse(authorizationURL);
                // wait on the lock. callback to the handler on the rkoauth will clear the lock.
                lock.wait();

                // something must have hit the lock, so we hope that 'code' has been set
                String content = RunkeeperService.getTokenUri(code, callbackUri);

                HttpClient httpclient = new DefaultHttpClient();
                HttpPost post = new HttpPost(content);
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                String responseBody = httpclient.execute(post, responseHandler);

                JsonNode rootNode = new ObjectMapper().readValue(responseBody, JsonNode.class);
                String accessToken = rootNode.get("access_token").asText();
                return accessToken;
            }
        } catch (Exception e) {
        } finally {
            if (server != null) {
                try {
                    server.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return "";

    }

    private static int getEphemeralPort() throws IOException {
        Socket s = new Socket();
        s.bind(null);
        try {
            return s.getLocalPort();
        } finally {
            s.close();
        }
    }

    protected void proceed() {
        synchronized (lock) {
            lock.notifyAll();
            return;
        }
    }

    protected Handler newCallback() {
        return new Callback(this);
    }

    protected void setCode(String newCode){
        this.code = newCode;
    }

    protected static class Callback extends AbstractHandler {

        protected Callback(RunkeeperOAuthClient client) {
            this.client = client;
        }

        protected final RunkeeperOAuthClient client;

        public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch)
                throws IOException, ServletException {
            client.setCode(request.getParameter("code"));
            conclude(response);
            client.proceed();
        }

        protected void conclude(HttpServletResponse response) throws IOException {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/html");
            final PrintWriter doc = response.getWriter();
            doc.println("<HTML>");
            doc.println("<body onLoad=\"window.close();\">");
            doc.println("Thank you.  You can close this window now.");
            doc.println("</body>");
            doc.println("</HTML>");
        }

    }

    static { // suppress log output from Jetty
        try {
            Logger.getLogger("org.mortbay.log").setLevel(Level.WARNING);
        } catch (Exception ignored) {
        }
        try {
            System.setProperty("org.apache.commons.logging.simplelog.log.org.mortbay.log", "warn");
        } catch (Exception ignored) {
        }
    }

}