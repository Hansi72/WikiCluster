import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedList;

public class WikiAPI {


    public LinkedList<String> getAllPosts(String countryCode){
        LinkedList<String> articles = new LinkedList<>();
        try{
            String url = "https://" + countryCode + ".wikipedia.org/w/api.php?action=query&format=json&list=allpages&formatversion=2&aplimit=max";
            String requestAnswer = httpRequest(url);
            while (requestAnswer.contains("apcontinue")) {
                pushLinkTitles(requestAnswer, articles);
                requestAnswer = httpRequest(url + "&apcontinue=" + URLEncoder.encode(getContinueCode(requestAnswer, "apcontinue"),"UTF-8"));
                float percentageCompleted = (float)articles.size()/7000000*100;
                System.out.println("Finding all posts. " + articles.size() + "  " + String.format("%.01f", percentageCompleted) + "% Complete");
            }
            pushLinkTitles(requestAnswer, articles);
        } catch (IOException e) {
            System.err.println(e);
        }
        return articles;
    }

    public LinkedList<String> getLinksHere(String article, String countryCode) {
        LinkedList<String> articles = new LinkedList<>();
        try{
        String url = "https://" + countryCode + ".wikipedia.org/w/api.php?action=query&format=json&prop=linkshere&formatversion=2&lhprop=title&lhshow=!redirect&lhlimit=max&titles=" + URLEncoder.encode(article, "UTF-8");
            String requestAnswer = httpRequest(url);
            while (requestAnswer.contains("lhcontinue")) {
                pushLinkTitles(requestAnswer, articles);
                requestAnswer = httpRequest(url + "&lhcontinue=" + getContinueCode(requestAnswer, "lhcontinue"));
            }
            pushLinkTitles(requestAnswer, articles);
        } catch (IOException e) {
            System.err.println("HTTP request failed for title " + article + " ignoring this title.");
        }
        return articles;
    }

    //extract the url titles from a string of JSON.
    void pushLinkTitles(String json, LinkedList<String> titles) {
        int startIndex = json.indexOf("links");
        int endIndex = 0;
        int oldStartIndex = 0;
        String title;

        for (int i = 0; i < 500; i++) {
            oldStartIndex = startIndex;
            startIndex = json.indexOf("\"title\":", startIndex) + "\"title\": ".length();
            endIndex = json.indexOf("}", startIndex) - 1;
            if (startIndex < oldStartIndex) {
                break;
            }
            title = json.substring(startIndex, endIndex);
            if (isArticle(title)) {
                titles.push(title);
            }
        }
    }

    String getContinueCode(String json, String continueTag) {
        int startIndex;
        int endIndex;
        startIndex = json.indexOf(continueTag) + continueTag.length() + "\": ".length();
        endIndex = json.indexOf('"' + ",", startIndex);
        return json.substring(startIndex, endIndex);
    }

    //remove unwanted wikipedia links (talks, users etc..) todo: make this faster (with a trie(?), if it reaches endNode, ignore)
    boolean isArticle(String title) {
        if (title.startsWith("Wikipedia")
                || title.startsWith("Talk") || title.startsWith("User")
                || title.startsWith("Portal") || title.startsWith("List") || title.startsWith("Category") || title.startsWith("Draft") || title.startsWith("Template")
                || title.startsWith("Diskusjon") || title.startsWith("Bruker")
                || title.startsWith("Lenke") || title.startsWith("Liste") || title.startsWith("Kategori") || title.startsWith("Mal")) {
            return false;
        }
        return true;
    }

    public String httpRequest(String site) throws IOException {
        URL url = new URL(site);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);

        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
        StringBuilder response = new StringBuilder();
        String responseLine = null;
        while ((responseLine = br.readLine()) != null) {
            response.append(responseLine.trim());
        }
        return response.toString();
    }
}
