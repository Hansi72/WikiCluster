import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;


public class WebServer {
    final static String[] colors = {"gray", "green", "blue", "yellow", "orange", "red"};
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
            if (nodeQuery.length == 1) { //todo add solutionSize
                for (int i = 0; i < nodeQuery.length; i++) {
                    LinkedList<String[]> adjacents = new LinkedList<String[]>();
                    for (Integer edgeID : db.adjLists.get(db.articlesByName.get(nodeQuery[i]))) {
                        String[] edgeNStyle = {db.articlesByIndex.get(edgeID), null};
                        adjacents.push(edgeNStyle);
                    }
                    adjLists.put(nodeQuery[i], adjacents);
                }
            } else {
                adjLists = getShortestPathAdjList(nodeQuery, 10, false);
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

    //returns adjacency lists of shortest paths between nodeQuery input
    static HashMap<String, LinkedList<String[]>> getShortestPathAdjList(String[] nodeQuery, int solutionSize, boolean hasDB) {
        ArrayList<ArticleNode> solutions = getBFSPaths(nodeQuery, solutionSize, hasDB);
        //todo sort solutions (already sorted?)
        System.out.println("adding solutions to result");
        //add solution paths to result
        HashMap<String, LinkedList<String[]>> adjLists = new HashMap<String, LinkedList<String[]>>();
        for (ArticleNode solution : solutions) {
            adjLists.put(solution.name, new LinkedList<>());

            int pathLength = 0;
            for (int i = 0; i < nodeQuery.length; i++) {
                pathLength = Math.min(pathLength + solution.pathLengths[i], 5);
            }
            String edgeColor = colors[pathLength];

            for (int i = 0; i < nodeQuery.length; i++) {
                ArticleNode currentNode = solution;
                while (currentNode.name != nodeQuery[i]) {
                    //todo counter and set colours after (?)
                    System.out.println("while loop comparing " + currentNode.name + "  :   " + nodeQuery[i] + " length: " + currentNode.pathLengths[i]);
                    String[] edgeNStyle = {currentNode.sources[i].name, "[color=" + edgeColor + "]"}; //todo make enum of colours
                    if (!adjLists.containsKey(currentNode.name)) {
                        adjLists.put(currentNode.name, new LinkedList<>());
                    }
                    adjLists.get(currentNode.name).add(edgeNStyle);
                    currentNode = currentNode.sources[i];
                }
            }
        }
        //todo add some extra nodes for nodeQuery nodes
        System.out.println("smarterThing ended with solutioncount " + solutions.size());
        System.out.println("returning size " + adjLists.size());
        return adjLists;
    }

    //returns a list of articleNodes that reach all nodes given in nodeQuery[]
    static ArrayList<ArticleNode> getBFSPaths(String[] nodeQuery, int solutionSize, boolean hasDB) {
        HashMap<String, ArticleNode> nodeInfo = new HashMap<>();
        ArrayList<ArticleNode> solutions = new ArrayList<>(solutionSize);
        LinkedList<ArticleNode>[] queues = new LinkedList[nodeQuery.length];

        for (int i = 0; i < nodeQuery.length; i++) {
            nodeInfo.put(nodeQuery[i], new ArticleNode(nodeQuery[i], nodeQuery.length));
            //solutions.add(nodeInfo.get(nodeQuery[i]));
            queues[i] = new LinkedList<ArticleNode>();
            queues[i].add(nodeInfo.get(nodeQuery[i]));
            //todo maybe connect sources to self.
        }

        int pathLength = 0;
        int currentQueSize;
        LinkedList<String> adjList;
        while (solutions.size() < solutionSize) {
            pathLength++;
            //do one BFS jump per nodeQuery
            for (int i = 0; i < nodeQuery.length; i++) {
                currentQueSize = queues[i].size();
                for (int k = 0; k < currentQueSize; k++) {
                    ArticleNode currentArticle = queues[i].remove();
                    adjList = getLinksHere(currentArticle.name, hasDB, "no");
                    System.out.println("BFS in progress.. current article =" + currentArticle.name);
                    System.out.println("adjlist size " + adjList.size());
                    for (String edgeName : adjList) {
                        nodeInfo.putIfAbsent(edgeName, new ArticleNode(edgeName, nodeQuery.length));
                        //if there is not already a shorter path, add edge to queue.
                        //todo change directions of this connection.
                        if (nodeInfo.get(edgeName).sources[i] == null) {
                            nodeInfo.get(edgeName).sources[i] = currentArticle;
                            nodeInfo.get(edgeName).pathLengths[i] = pathLength;
                            queues[i].add(nodeInfo.get(edgeName));
                        }
                        boolean isSolution = true;
                        //add to solution if all sources reach this node
                        for (int j = 0; j < nodeQuery.length; j++) {
                            if (nodeInfo.get(edgeName).sources[j] == null) {
                                isSolution = false;
                            }
                        }
                        if (isSolution) {
                            solutions.add(nodeInfo.get(edgeName));
                        }
                        if (solutions.size() > solutionSize) {
                            break;
                        }
                    }
                }
            }
        }
        System.out.println("solutions");
        for (ArticleNode solution : solutions) {
            System.out.println(solution.name);
        }
        return solutions;
    }

    static LinkedList<String> getLinksHere(String source, boolean hasDB, String countryCode) {
        if (hasDB) {
            LinkedList<String> adjList = new LinkedList<>();
            for (int articleID : db.adjLists.get(db.articlesByName.get(source))) {
                adjList.add(db.articlesByIndex.get(articleID));
            }
            return adjList;
        } else {
            WikiAPI wikiAPI = new WikiAPI();
            return wikiAPI.getLinksHere(source, countryCode);
        }
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

class ArticleNode {
    public String name;
    public int[] pathLengths;
    public ArticleNode[] sources;

    public ArticleNode(String name, int querySize) {
        this.name = name;
        this.pathLengths = new int[querySize];
        this.sources = new ArticleNode[querySize];
    }
}