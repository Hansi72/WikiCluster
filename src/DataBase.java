import java.io.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

class DataBase implements Serializable {

    HashMap<String, Integer> articlesByName = new HashMap<>();
    //todo save only the two below for less storage use
    ArrayList<String> articlesByIndex = new ArrayList<>();
    ArrayList<LinkedList<Integer>> adjLists = new ArrayList<>();

    Long creationDateUnix;
    int id;

    public DataBase(int id) {
        this.creationDateUnix = Instant.now().getEpochSecond();
        this.id = id;
    }

    public void addArticle(String title) {
        int nextIndex = articlesByName.size();
        articlesByName.put(title, nextIndex);
        articlesByIndex.add(title);
        adjLists.add(nextIndex, new LinkedList<>());
    }

    public void addEdge(String source, String target) {
        int sourceID = articlesByName.get(source);
        int targetID = articlesByName.get(target);
        adjLists.get(sourceID).add(targetID);
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
