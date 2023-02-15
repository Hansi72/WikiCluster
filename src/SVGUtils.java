import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;

public final class SVGUtils {

    //Run a python command to create a SVG using GraphViz. Requires python in working directory or PATH variables
    public static byte[] createSVG(String dotFile) {
        try {
            System.out.println("creating SVG");
            ProcessBuilder processBuilder = new ProcessBuilder("python", "GraphViz/GraphViz.py");
            processBuilder.redirectErrorStream(true); //todo remove
            Process process = processBuilder.start();
            //OutputStream outStream =
            process.getOutputStream().write(dotFile.getBytes(StandardCharsets.UTF_8));
            process.getOutputStream().close();
            //outStream.write(dotFile);
            byte[] svgBytes = process.getInputStream().readAllBytes();
            process.waitFor();
            System.out.println("createSVG complete");
            return svgBytes;
        } catch (Exception e) {
            System.out.println("createSVG SVG failed");
            System.out.println(e);
            return null;
        }
    }

    //saves a dotfile to working directory
    public static void saveDotFile(String filename, HashMap<String, LinkedList<Integer>> adjLists, DataBase db) {
        try {
            File dotFile = new File(filename + ".dot");
            dotFile.createNewFile();
            FileWriter fWriter = new FileWriter(filename + ".dot");
            //fWriter.write(createDotFile(adjLists, db));
            fWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //creates a dot file with the given nodes and edges.
    public static String createDotFile(HashMap<String, LinkedList<String[]>> adjLists) {
        //graph settings
        StringBuilder dotString = new StringBuilder("strict graph {\n"
                + "layout=twopi;\n"  //twopi, circo..
               //+ "overlap = scale;\n"
              // + "concentrate=true\n"
               // + "outputorder=\"edgesfirst\" \n"
               + "overlap = false;\n"
               + "splines = true;\n"
               // + "scale = 0.1;\n"
                );
        //add edges
        for (HashMap.Entry<String, LinkedList<String[]>> adjList : adjLists.entrySet()) {
            for (String[] edgeNStyle : adjList.getValue()){
                dotString.append("\"" + edgeNStyle[0] + "\"" + " -- " + "\"" + adjList.getKey() + "\" ");
                if(edgeNStyle[1] != null){
                    dotString.append(edgeNStyle[1]);
                }
                dotString.append("\n");
            }
        }
        dotString.append("}");
        return dotString.toString();
    }

}
