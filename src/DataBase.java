import java.io.*;
import java.time.Instant;
import java.util.HashMap;

class DataBase implements Serializable {
    public HashMap<String, ArticleNode> articles = new HashMap<>();
    Long creationDateUnix;
    int id;

    public DataBase(int id) {
        this.creationDateUnix = Instant.now().getEpochSecond();
        this.id = id;
    }

    public void addArticle(String title, String source) {
        if (articles.containsKey(title)) {
            articles.get(title).addEdge(articles.get(source));
        } else {
            articles.put(title, new ArticleNode(title));
            articles.get(title).addEdge(new ArticleNode(source));
        }
    }

    public void saveToFile() {
        String dir = System.getProperty("user.dir");
        try {
            FileOutputStream fileOut = new FileOutputStream(dir + "\\DataBases\\" + id); //todo create folder if not existing
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(this);
            objectOut.close();
            System.out.println("Successfully saved database with id " + id + " to " + dir + "\\DataBases\\");
        } catch (Exception e) {
            System.err.println("Error saving database with id " + id);
            System.err.println(e);
        }
    }

    public DataBase loadFromFile(int id) {
        File file = new File(System.getProperty("user.dir") + "\\DataBases\\" + id);
        Object db = null;
        try {
            FileInputStream fileStream = new FileInputStream(file);
            ObjectInputStream objectStream = new ObjectInputStream(fileStream);
            db = objectStream.readObject();
            System.out.println("Successfully loaded database with id " + id);
        } catch (ClassNotFoundException | IOException e) {
            System.err.println("Error loading database with id " + id + " from " + System.getProperty("user.dir") + "\\DataBases\\");
            System.err.println(e);
        }
        return (DataBase) db;
    }
}
