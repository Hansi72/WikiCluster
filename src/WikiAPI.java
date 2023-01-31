import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class WikiAPI {




    //extract the url titles from a string of JSON.
    String[] getLinkTitles(String json){
        String[] linkTitles = new String[500];
        int startIndex = json.indexOf("links");
        int endIndex = 0;
        int oldStartIndex = 0;

        for(int i = 0; i < 500; i++) {
            oldStartIndex = startIndex;
            startIndex = json.indexOf("title", startIndex) + "title".length() + 3;
            endIndex = json.indexOf("}", startIndex) - 1;
            if(startIndex < oldStartIndex){break;}
            linkTitles[i] = json.substring(startIndex, endIndex);
        }
        return linkTitles;
    }

    String getContinueCode(String json){
        int startIndex;
        int endIndex;
        startIndex = json.indexOf("plcontinue") + "plcontinue".length() + 3;
        endIndex = json.indexOf('"' + ",", startIndex);
        return json.substring(startIndex, endIndex);
    }

    public void getWikiLinkTitles(String article){
        String url = "https://en.wikipedia.org/w/api.php?action=query&format=json&prop=links&pllimit=max&titles=" + article;
        try {
            String requestAnswer = httpRequest(url);
            String[] test = getLinkTitles(requestAnswer); //todo find out what structure to use and merge the results here.
            while(requestAnswer.contains("plcontinue")) {
                requestAnswer = httpRequest(url + "&plcontinue=" + getContinueCode(requestAnswer));
            }
            test = getLinkTitles(requestAnswer); //todo find out what structure to use and merge the results here.
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String httpRequest(String site) throws IOException {
        URL url = new URL(site);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
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
