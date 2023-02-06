import 'dart:math';
import 'dart:convert';
import 'dart:html';
import 'package:flutter/material.dart';
import 'package:graphview/GraphView.dart';
import 'package:http/http.dart' as http;

main() async {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        // This is the theme of your application.
        //
        // Try running your application with "flutter run". You'll see the
        // application has a blue toolbar. Then, without quitting the app, try
        // changing the primarySwatch below to Colors.green and then invoke
        // "hot reload" (press "r" in the console where you ran "flutter run",
        // or simply save your changes to "hot reload" in a Flutter IDE).
        // Notice that the counter didn't reset back to zero; the application
        // is not restarted.

        primarySwatch: Colors.blue,
      ),
      home: const MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});

  // This widget is the home page of your application. It is stateful, meaning
  // that it has a State object (defined below) that contains fields that affect
  // how it looks.

  // This class is the configuration for the state. It holds the values (in this
  // case the title) provided by the parent (in this case the App widget) and
  // used by the build method of the State. Fields in a Widget subclass are
  // always marked "final".

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  late Future<Graph> futureGraph;

  @override
  Widget build(BuildContext context) {
    // This method is rerun every time setState is called, for instance as done
    // by the _incrementCounter method above.
    //
    // The Flutter framework has been optimized to make rerunning build methods
    // fast, so that you can just rebuild anything that needs updating rather
    // than having to individually change instances of widgets.


    return Scaffold(
        body: Column(
      mainAxisSize: MainAxisSize.max,
      children: [
        Expanded(
          child: InteractiveViewer(
              constrained: false,
              boundaryMargin: EdgeInsets.all(100),
              minScale: 0.01,
              maxScale: 5.6,
              child: GraphView(
                graph: graph,
                algorithm: FruchtermanReingoldAlgorithm(),
                paint: Paint()
                  ..color = Colors.green
                  ..strokeWidth = 1
                  ..style = PaintingStyle.stroke,
                builder: (Node node) {
                  // I can decide what widget should be shown here based on the id
                  var a = node.key?.value as int;
                  return rectangleWidget(a);
                },
              )),
        ),
      ],
    ));
  }



  Random r = Random();

  Widget rectangleWidget(int a) {
    return InkWell(
      onTap: () {
        print('clicked');
      },
      child: Container(
          padding: EdgeInsets.all(16),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(4),
            boxShadow: [
              BoxShadow(color: Colors.blue, spreadRadius: 1),
            ],
          ),
          child: Text('Node ${a}')),
    );
  }

  final Graph graph = Graph()..isTree = true;
  FruchtermanReingoldAlgorithm builder = FruchtermanReingoldAlgorithm();

  @override
  void initState() {

    futureGraph = fetchGraph(graph);
  }
}

Future<Graph> fetchGraph(Graph graph) async {
  List<String> articles = List.empty(growable: true);
  List<List<int>> edges = List.empty(growable: true);
  var data;
  List<String> articleStrings;
  List<String> edgeStrings;
  int articleTotal = 50;

  //fetch and process data
  for (int i = 0; i < articleTotal / 50; i++) {
    data = await fetchDBData(i);
    data = data.split('^');
    articleTotal = int.parse(data[0]);

    //parse articleNames
    articleStrings = data[1].split(';');
    for (var article in articleStrings) {
      articles.add(article);
    }
    articles.removeLast();

    //parse edges
    edgeStrings = data[2].split(";");
    List<String> articleEdgeList;
    for (var edgeLists in edgeStrings) {
      if (edgeLists.length > 0) {
        edgeLists = edgeLists.substring(1, edgeLists.length - 1);
      }
      articleEdgeList = edgeLists.split(',');
      List<int> edgeListInt = List.empty(growable: true);
      for (int i = 0; i < articleEdgeList.length; i++) {
        try {
          edgeListInt.add(int.parse(articleEdgeList[i]));
        } catch (e) {
          //ignores empty lists caused by .split()
        }
      }
      edges.add(edgeListInt);
    }
    edges.removeLast();
  }
  //create graph
  for (var i = 0; i < articles.length; i++) {
    graph.addNode(Node.Id(articles[i]));
  }
  var edgeList;
  for (var articleID = 0; articleID < articles.length; articleID++) {
    edgeList = edges[articleID];
    for (int i = 0; i < edgeList.length; i++) {
      graph.addEdge(
          Node.Id(articles[articleID]), Node.Id(articles[edgeList[i]]));
    }
  }
  return graph;
}

Future<String> fetchDBData(int page) async {
  var response = await http.get(Uri.parse('http://localhost:7200/test?$page'));
  if (response.statusCode == 200) {
    return response.body;
  } else {
    throw Exception('Failed to load data');
  }
}
