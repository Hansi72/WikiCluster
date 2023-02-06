import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLEncoder;

public class WebServer {
    static DataBase db;
    static int port;

    WebServer(DataBase db, int port) {
        WebServer.db = db;
        WebServer.port = port;
    }

    public static void start() {
        try {
            HttpServer server = null;
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/test", new MyHandler());
            server.setExecutor(null);
            server.start();
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            Headers headers = t.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            t.sendResponseHeaders(200, 0);
            OutputStream os = t.getResponseBody();
            try {
                os.write(createResponse(Integer.parseInt(t.getRequestURI().getQuery())).getBytes("UTF-8"));
            } catch (Exception e) {
                System.err.println("bad request query: " + t.getRequestURI().getQuery());
                System.err.println(e);
            }
            os.close();
        }
    }

    //converts db data to a csv string
    static String createResponse(int part) {
        StringBuilder response = new StringBuilder(db.articlesByIndex.size() + "^");
        String title;
        System.out.println("Serving articles " + part * 50 + " to " + Math.min(part * 50 + 50, db.articlesByIndex.size()));
        for (int article = part * 50; article < Math.min(part * 50 + 50, db.articlesByIndex.size()); article++) {
            title = replaceTwice(db.articlesByIndex.get(article), ';', ':', '^', '*');
            response.append(title);
            if (article != Math.min(part * 50 + 50, db.articlesByIndex.size())) {
                response.append(";");
            }
        }
        response.append("^");
        for (int article = part * 50; article < Math.min(part * 50 + 50, db.articlesByIndex.size()); article++) {
            response.append(db.adjLists.get(article));
            if (article != Math.min(part * 50 + 50, db.articlesByIndex.size())) {
                response.append(";");
            }
        }
        return response.toString();
    }

    static String replaceTwice(String string, char target1, char replacement1, char target2, char replacement2) {
        char[] chars = string.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == target1) {
                chars[i] = replacement1;
            }
            if (string.charAt(i) == target2) {
                chars[i] = replacement2;
            }
        }
        return String.valueOf(chars);
    }
}