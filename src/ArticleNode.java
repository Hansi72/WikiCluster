import java.io.Serializable;
import java.util.LinkedList;


class ArticleNode implements Serializable {
    String title;
    LinkedList<ArticleNode> edgesTo = new LinkedList<>();

    public ArticleNode(String title) {
        this.title = title;
    }

    public void addEdge(ArticleNode node) {
        edgesTo.add(node);
    }
}
