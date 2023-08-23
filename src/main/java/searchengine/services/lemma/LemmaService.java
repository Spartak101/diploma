package searchengine.services.lemma;

import com.github.demidko.aot.WordformMeaning;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public interface LemmaService {
    HashMap<String, Integer> lemmas();

    HashMap<String, Integer> countLemmas(ArrayList<String> arrayList, HashMap<String, Integer> lemmas);

    ArrayList<String> stringsForLemmas(String string);

    boolean superfluousStrings(List<WordformMeaning> lemmas);

    ArrayList<Element> elementsInDoc(Document doc);
}
