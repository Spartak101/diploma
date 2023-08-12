package searchengine.dto.parsing;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class SiteName {
    private static Document doc;

    public SiteName(Document doc) {
        this.doc = doc;
    }

    public static String siteName(Document doc){
        Elements name = doc.select("title");
        String string = name.text();
        return string;
    }
}
