package xacml.server;

import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class Server {

    private static SamplePolicy samplePolicy;

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/request", new Handler());
        server.setExecutor(null); // creates a default executor
        server.start();
        samplePolicy = new SamplePolicy();
    }

    static class Handler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) {
            // Get the name of the person requesting access
            String personRequestingAccess = null;
            String parameters = t.getRequestURI().getQuery();
            if (parameters != null) {
                String[] keyValue = parameters.split("=");
                if ((keyValue.length > 0) && (keyValue[0].equals("who"))) {
                    personRequestingAccess = keyValue[1];
                    System.out.println("Requesting access for " + personRequestingAccess);
                }
            }
            final String response = samplePolicy.processRequest(
                "target/classes/IIA001Request.xml", personRequestingAccess
            ).toString();
            try {
                System.out.println("Sending response\n");
                t.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
            catch(Exception ex) {
                System.out.println("Caught Exception " + ex.getMessage());
            }
        }
    }
}

