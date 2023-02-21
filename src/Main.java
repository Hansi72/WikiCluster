import java.io.*;
import java.util.LinkedList;

public class Main {
    static DataBase db = new DataBase();

    public static void main(String[] args) {
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdir();
        }


        //todo fiks threading of requests

        db = db.loadFromFile("norwayDB");
        System.out.println("DB loaded. Size: " + db.articlesByIndex.size());

        WebServer web = new WebServer(db, 7200);
        web.start();

/*      //Example of creating a simple dotFile
        int[] nodes = new int[1];
        nodes[0] = db.articlesByName.get("Sparebanken_Vest");
        LinkedList<Integer>[] adjacencyLists = new LinkedList[1];
        adjacencyLists[0] = db.adjLists.get(nodes[0]);

        writeDotFile("test", nodes, adjacencyLists);*/
    }

    //returns a database containing all wikipedia articles
    public static DataBase createDb(String countryCode) {
        DataBase db = new DataBase();
        WikiAPI wikiAPI = new WikiAPI();

        LinkedList<String> allPosts = wikiAPI.getAllPosts(countryCode);
        while (!allPosts.isEmpty()) {
            try {
                db.addArticle(allPosts.pop());
            } catch (Exception e) {
                System.err.println("Error adding article to DB:\n" + e);
            }
        }

        int articleCount = 0;
        for (String articleSource : db.articlesByIndex) {
            LinkedList<String> articlePointers = wikiAPI.getLinksHere(articleSource, countryCode);
            while (!articlePointers.isEmpty()) {
                try {
                    db.addEdge(articleSource, articlePointers.pop());
                } catch (Exception e) {
                    System.err.println("Error adding articleLink to DB:\n" + e);
                }
            }
            articleCount++;
            float percentageCompleted = (float) articleCount / db.articlesByIndex.size() * 100;
            System.out.println("Connecting links. " + articleCount + "  " + String.format("%.01f", percentageCompleted) + "% Complete");
        }
        return db;
    }

    //Returns a breath first database growing from a given startArticle.
    public static DataBase createDbBFS(String startArticle, int maxArticles, String countryCode) {
        DataBase db = new DataBase();
        WikiAPI wikiAPI = new WikiAPI();
        LinkedList<String> articleQueue = new LinkedList<>();
        articleQueue.add(startArticle);
        db.addArticle(startArticle);

        LinkedList<String> links;
        String currentArticle;
        String currentLink;

        int count = 1;
        while (!articleQueue.isEmpty() && count < maxArticles) {
            currentArticle = articleQueue.pop();
            links = wikiAPI.getLinksHere(currentArticle, countryCode);
            while (!links.isEmpty() && count < maxArticles) {
                currentLink = links.pop();
                if (!db.articlesByName.containsKey(currentLink)) {
                    db.addArticle(currentLink);
                    articleQueue.add(currentLink);
                    count++;
                }
                db.addEdge(currentArticle, currentLink);
            }
            System.out.println("Creating DB.. Articles currently in DB: " + db.articlesByName.size());
            System.out.println("Current article: " + currentArticle);
        }
        return db;
    }

}
