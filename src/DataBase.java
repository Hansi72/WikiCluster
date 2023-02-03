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

    public ArticleNode addArticle(String title) {
        ArticleNode newArticle = new ArticleNode(title);
        articles.put(title, newArticle);
        return newArticle;
    }

    public void saveToFile(String fileName) {
        String userDir = System.getProperty("user.dir");
        try {
            FileOutputStream fileOut = new FileOutputStream(userDir + "\\data\\" + fileName);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(this);
            objectOut.close();
            System.out.println("Successfully saved database with id " + id + " to " + userDir + "\\data\\");
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    public DataBase loadFromFile(int id) {
        File file = new File(System.getProperty("user.dir") + "\\data\\" + id);
        Object db = null;
        try {
            FileInputStream fileStream = new FileInputStream(file);
            ObjectInputStream objectStream = new ObjectInputStream(fileStream);
            db = objectStream.readObject();
            System.out.println("Successfully loaded database with id " + id);
        } catch (ClassNotFoundException | IOException e) {
            System.err.println(e);
        }
        return (DataBase) db;
    }
}
