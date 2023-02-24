import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/db", new DBHandler());
            server.createContext("/getSVG", new SVGHandler());
            server.createContext("/help", new HelpHandler());
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

            OutputStream os = exchange.getResponseBody();
            String[] nodeQuery = exchange.getRequestURI().getQuery().split("\\+");
            int pathCount = 10;

            //if single query, only get direct adjacents of input node.
            if (nodeQuery.length == 1) {
                if (!db.articlesByName.containsKey(nodeQuery[0])) {
                    exchange.sendResponseHeaders(400, 0);
                    os.write(badRequestErrorText(nodeQuery[0]).getBytes("UTF-8"));
                    os.close();
                } else {
                    int adjacentSize = db.adjLists.get(db.articlesByName.get(nodeQuery[0].replace("_", " "))).size();
                    pathCount = Math.min(adjacentSize + 1, pathCount);
                }
            } else {
                //get pathCount argument if present
                try {
                    pathCount = Integer.parseInt(nodeQuery[nodeQuery.length - 1]) + nodeQuery.length - 1;
                    assert (1 < pathCount && pathCount < 100);
                    nodeQuery = Arrays.copyOfRange(nodeQuery, 0, nodeQuery.length - 1);
                } catch (Exception e) {
                    System.out.println("No nodeCount given, using default: " + pathCount);
                }
                for (int i = 0; i < nodeQuery.length; i++) {
                    nodeQuery[i] = nodeQuery[i].replace("_", " ");
                    if (!db.articlesByName.containsKey(nodeQuery[i])) {
                        exchange.sendResponseHeaders(400, 0);
                        os.write(badRequestErrorText(nodeQuery[i]).getBytes("UTF-8"));
                        os.close();
                        return;
                    }
                }
            }
            exchange.sendResponseHeaders(200, 0);

            HashMap<String, LinkedList<String[]>> adjLists = getShortestPathAdjList(nodeQuery, pathCount, true);

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
        //add solution paths to result
        HashMap<String, LinkedList<String[]>> adjLists = new HashMap<String, LinkedList<String[]>>();
        for (ArticleNode solution : solutions) {
            adjLists.put(solution.name, new LinkedList<>());

            int pathLength = 0;
            for (int i = 0; i < nodeQuery.length; i++) {
                pathLength = Math.min(pathLength + solution.pathLengths[i], 5);
            }

            for (int i = 0; i < nodeQuery.length; i++) {
                ArticleNode currentNode = solution;
                while (currentNode.name != nodeQuery[i]) {
                    String[] edgeNStyle = {currentNode.sources[i].name, "[color=black]"};
                    if (!adjLists.containsKey(currentNode.name)) {
                        adjLists.put(currentNode.name, new LinkedList<>());
                    }
                    adjLists.get(currentNode.name).add(edgeNStyle);
                    currentNode = currentNode.sources[i];
                }
            }
        }
        return adjLists;
    }

    //returns a list of articleNodes that reach all nodes given in nodeQuery[]
    static ArrayList<ArticleNode> getBFSPaths(String[] nodeQuery, int solutionSize, boolean hasDB) {
        HashMap<String, ArticleNode> nodeInfo = new HashMap<>();
        ArrayList<ArticleNode> solutions = new ArrayList<>(solutionSize);
        LinkedList<ArticleNode>[] queues = new LinkedList[nodeQuery.length];

        for (int i = 0; i < nodeQuery.length; i++) {
            nodeInfo.put(nodeQuery[i], new ArticleNode(nodeQuery[i], nodeQuery.length));
            queues[i] = new LinkedList<ArticleNode>();
            queues[i].add(nodeInfo.get(nodeQuery[i]));
        }

        int pathLength = 0;
        int currentNodeQueSize;
        LinkedList<String> adjList;
        while (solutions.size() < solutionSize - nodeQuery.length) {
            pathLength++;
            //do one BFS jump per nodeQuery
            for (int i = 0; i < nodeQuery.length; i++) {
                currentNodeQueSize = queues[i].size();
                //todo refactor this loop to a method
                for (int k = 0; k < currentNodeQueSize; k++) {
                    ArticleNode currentArticle = queues[i].remove();
                    adjList = getLinksHere(currentArticle.name, hasDB, "no");
                    System.out.println("BFS in progress.. current article =" + currentArticle.name);
                    System.out.println("adjlist size " + adjList.size());
                    for (String edgeName : adjList) {
                        if (solutions.size() >= solutionSize - nodeQuery.length) {
                            break;
                        }
                        nodeInfo.putIfAbsent(edgeName, new ArticleNode(edgeName, nodeQuery.length));
                        //if there is not already a shorter path, add edge to queue.
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
                    }
                }
            }
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

    //serves the whole database as a CVS-ish string
    static class DBHandler implements HttpHandler {
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

    static class HelpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, 0);
            OutputStream os = exchange.getResponseBody();
            try {
                os.write((
                        "Copy/paste examples into browsers url field to view graph: \n"
                                + "http://trygven.no:7200/getSVG?Bergen \n"
                                + "http://trygven.no:7200/getSVG?Bergen+Arbeidsgiver+Intervju \n\n"

                                + "Different amount of paths: \n"
                                + "http://trygven.no:7200/getSVG?Bergen+Arbeidsgiver+Intervju+1 \n"
                                + "http://trygven.no:7200/getSVG?Bergen+Arbeidsgiver+Intervju+20\n\n"

                                + "Create your own graph with any number of articles and path amount(optional last argument, 1-100).\n"
                                + "http://trygven.no:7200/getSVG?article1+article2+article3\n"
                ).getBytes("UTF-8"));
            } catch (Exception e) {
                System.err.println("bad request query: " + exchange.getRequestURI().getQuery());
                System.err.println(e);
            }
            os.close();
        }

    }

    static String badRequestErrorText(String badArticle) {
        return "Bad Request: Article name " + badArticle + " not found in database.\nArticle names are case-sensitive.\n" +
                "Try to match article names from Wikipedia. e.g: Article 'Bergen' is procured from https://no.wikipedia.org/wiki/Bergen\n";
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