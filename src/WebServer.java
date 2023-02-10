import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
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
            System.out.println("got query: getSVG/?" + exchange.getRequestURI().getQuery());
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, 0);
            OutputStream os = exchange.getResponseBody();
            String[] nodeQuery = exchange.getRequestURI().getQuery().split("\\+");
            HashMap<String, LinkedList<String[]>> adjLists = new HashMap<String, LinkedList<String[]>>();
            if (nodeQuery.length == 1) {
                for (int i = 0; i < nodeQuery.length; i++) {
                    LinkedList<String[]> adjacents = new LinkedList<String[]>();
                    for (Integer edgeID : db.adjLists.get(db.articlesByName.get(nodeQuery[i]))) {
                        String[] edgeNStyle = {db.articlesByIndex.get(edgeID), null};
                        adjacents.push(edgeNStyle);
                    }
                    adjLists.put(nodeQuery[i], adjacents);
                }
            } else {
                adjLists = smartThing(nodeQuery, 5, false);
            }
            try {
                String SVGFile = new String(SVGUtils.createSVG(SVGUtils.createDotFile(adjLists)));
                os.write(SVGFile.getBytes());
            } catch (Exception e) {
                System.err.println("null value SVG or bad request query for query = " + exchange.getRequestURI().getQuery());
                System.err.println(e);
            }
            os.close();
        }
    }

    //Does nodeQuery.length amount of BFS' until they all meet.
    // todo make it find second(and third?) best route aswell. rename. refactor. add different colours per nodeQuery
    static HashMap<String, LinkedList<String[]>> smartThing(String[] nodeQuery, int extraNodes, boolean hasDB) { //todo sparseness = how many irrelevant articles per node?
        WikiAPI wikiAPI = new WikiAPI();
        LinkedList<String> adjList = new LinkedList<String>();
        LinkedList<String>[] queues = new LinkedList[nodeQuery.length];
        HashMap<String, String[]> pathLists = new HashMap<String, String[]>();
        HashSet<String> solutions = new HashSet<String>();
        boolean foundSolution = false;
        boolean isSolution;

        for (int i = 0; i < nodeQuery.length; i++) {
            queues[i] = new LinkedList<String>();
            queues[i].add(nodeQuery[i]);
            //pathLists.put(nodeQuery[i], new String[nodeQuery.length]);
            //pathLists.get(nodeQuery[i])[i] = nodeQuery[i];
        }

        while (!foundSolution) {
            //do one jump per BFS
            for (int i = 0; i < nodeQuery.length; i++) {
                if (!queues[i].isEmpty()) {
                    String currentArticle = queues[i].remove();
                    if (hasDB) {
                        for (int articleID : db.adjLists.get(db.articlesByName.get(currentArticle))) {
                            adjList.add(db.articlesByIndex.get(articleID));
                        }
                    } else {
                        adjList = wikiAPI.getLinksHere(currentArticle, "no");
                    }
                    System.out.println("BFS in progress.. current article =" + currentArticle);
                    for (String edgeName : adjList) {
                        //System.out.println("edgeName = " + edgeName);
                        pathLists.putIfAbsent(edgeName, new String[nodeQuery.length]);
                        //if there is not already a shorter path from this source.
                        if (pathLists.get(edgeName)[i] == null) {
                            queues[i].add(edgeName);
                            pathLists.get(edgeName)[i] = currentArticle;
                        }
                        //check if all sources reach
                        isSolution = true;
                        for (int j = 0; j < nodeQuery.length; j++) {
                            if (pathLists.get(edgeName)[j] == null) {
                                isSolution = false;
                            }
                        }
                        if (isSolution) {
                            foundSolution = true;
                            solutions.add(edgeName);
                        }
                    }
                }
            }
        }
        System.out.println("adding solutions to result");
        //add solution paths to result
        HashMap<String, LinkedList<String[]>> adjLists = new HashMap<String, LinkedList<String[]>>();
            for (String solution : solutions) {
                String currentNode;
                for (int i = 0; i < nodeQuery.length; i++) {
                    currentNode = solution;
                    while (!currentNode.equals(nodeQuery[i])) {
                        String[] edgeNStyle = {currentNode, "[color=red]"};
                        if (!adjLists.containsKey(pathLists.get(currentNode)[i])) {
                            adjLists.put(pathLists.get(currentNode)[i], new LinkedList<String[]>());
                        }
                        adjLists.get(pathLists.get(currentNode)[i]).push(edgeNStyle);

                        if (extraNodes != 0) {
                            String[] edgeNStyleSparse = new String[2];
                            edgeNStyleSparse[1] = "[color=gray]";
                            LinkedList<String> adjacents = new LinkedList<String>();
                            adjacents = wikiAPI.getLinksHere(currentNode, "no");
                            int addCount = 0;
                            for (String adjant : adjacents) {
                                if (addCount > extraNodes) {
                                    break;
                                }
                                edgeNStyleSparse[0] = currentNode;
                                if (!solutions.contains(adjant)) {
                                    if (!adjLists.containsKey(adjant)) {
                                        adjLists.put(adjant, new LinkedList<String[]>());
                                    }
                                    adjLists.get(adjant).push(edgeNStyleSparse);
                                    addCount++;
                                }
                            }

                        }
                        currentNode = pathLists.get(currentNode)[i];
                    }
                }
            }
        //todo add some extra nodes for nodeQuery nodes
        System.out.println("smartThing ended with solutioncount " + solutions.size());
        System.out.println("returning size " + adjLists.size());
        return adjLists;
    }


    static class TestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, 0);
            OutputStream os = exchange.getResponseBody();
            try {
                os.write(dbPartToCSV(Integer.parseInt(exchange.getRequestURI().getQuery())).getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                System.err.println("bad request query: " + exchange.getRequestURI().getQuery());
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

    //faster than replace().replace()
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