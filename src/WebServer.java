import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedList;

public class WebServer {
    static DataBase db;
    static int port;

    WebServer(DataBase db, int port) {
        WebServer.db = db;
        WebServer.port = port;
    }

    public static void start() {
        try {
            //HttpServer server = null;
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/test", new TestHandler());
            server.createContext("/getSVG", new SVGHandler());
            server.setExecutor(null);
            server.start();
        } catch (IOException e) {
            System.out.println(e);
        }
        System.out.println("Webserver Started");
    }

    static class SVGHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            System.out.println("got query: " + exchange.getRequestURI().getQuery());
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, 0);
            OutputStream os = exchange.getResponseBody();
            String[] nodeQuery = exchange.getRequestURI().getQuery().split("\\+");
            HashMap<String, LinkedList<Integer>> adjLists = new HashMap<String, LinkedList<Integer>>();
            System.out.println("nodeQuery length " + nodeQuery.length);
            if(nodeQuery.length == 1) {
                for (int i = 0; i < nodeQuery.length; i++) {
                    adjLists.put(nodeQuery[i], db.adjLists.get(db.articlesByName.get(nodeQuery[i])));
                }
            }
            try {
                byte[] bytes = SVGUtils.createSVG(SVGUtils.createDotFile(adjLists, db));
                os.write(SVGUtils.createSVG(SVGUtils.createDotFile(adjLists, db)));
            } catch (Exception e) {
                System.err.println("null value SVG or bad request query for query = " + exchange.getRequestURI().getQuery());
                System.err.println(e);
            }
            os.close();
        }
    }

    //returns a part of db data as a csv-ish string
    static String dbPartToCSV(int part) {
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

    static class TestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, 0);
            OutputStream os = exchange.getResponseBody();
            try {
                os.write(dbPartToCSV(Integer.parseInt(exchange.getRequestURI().getQuery())).getBytes("UTF-8"));
            } catch (Exception e) {
                System.err.println("bad request query: " + exchange.getRequestURI().getQuery());
                System.err.println(e);
            }
            os.close();
        }
    }


}