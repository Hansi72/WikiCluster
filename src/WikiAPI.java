import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedList;

public class WikiAPI {

    public LinkedList<String> getWikiLinkTitles(String article) {
        LinkedList<String> titles = new LinkedList<>();
        try{
        String url = "https://en.wikipedia.org/w/api.php?action=query&format=json&prop=linkshere&formatversion=2&lhprop=title&lhshow=!redirect&lhlimit=max&titles=" + URLEncoder.encode(article, "UTF-8");
            String requestAnswer = httpRequest(url);
            while (requestAnswer.contains("lhcontinue")) {
                pushLinkTitles(requestAnswer, titles);
                requestAnswer = httpRequest(url + "&lhcontinue=" + getContinueCode(requestAnswer));
            }
            pushLinkTitles(requestAnswer, titles);
        } catch (IOException e) {
            System.err.println("HTTP request failed for title " + article + " ignoring this title.");
        }
        return titles;
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

    String getContinueCode(String json) {
        int startIndex;
        int endIndex;
        startIndex = json.indexOf("lhcontinue") + "lhcontinue\": ".length();
        endIndex = json.indexOf('"' + ",", startIndex);
        return json.substring(startIndex, endIndex);
    }

    //remove unwanted wikipedia links (talks, users etc..)
    boolean isArticle(String title) {
        if (title.startsWith("Talk") || title.startsWith("User") || title.startsWith("Wikipedia") || title.startsWith("Portal") || title.startsWith("List") || title.startsWith("Draft")) {
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
