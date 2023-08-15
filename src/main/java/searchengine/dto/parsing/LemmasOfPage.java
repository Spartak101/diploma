package searchengine.dto.parsing;

import com.github.demidko.aot.WordformMeaning;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.github.demidko.aot.WordformMeaning.lookupForMeanings;

public class LemmasOfPage {
    private Document doc;

    public LemmasOfPage(Document doc) {
        this.doc = doc;
    }

    public HashMap<String, Integer> lemmas() {
        Elements elementLinks = doc.select("a");
        Elements elementSpan = doc.select("span");
        Elements elementParagraph = doc.select("p");
        HashMap<String, Integer> lemmas = new HashMap<>();
        for (int i = 1; i < 7; i++) {
            Elements headings = doc.select("h" + i);
            lemmas.putAll(countLemmas(stringsForLemmas(headings), lemmas));
        }
        lemmas.putAll(countLemmas(stringsForLemmas(elementLinks), lemmas));
        lemmas.putAll(countLemmas(stringsForLemmas(elementSpan), lemmas));
        lemmas.putAll(countLemmas(stringsForLemmas(elementParagraph), lemmas));
//        lemmas.forEach((key, value) -> System.out.println(key + " " + value));
        return lemmas;
    }

    public HashMap<String, Integer> countLemmas(ArrayList<String> arrayList, HashMap<String, Integer> lemmas) {
        for (String s : arrayList) {
            if (!lemmas.containsKey(s)) {
                lemmas.put(s, 1);
            } else {
                Integer countLemmas = lemmas.get(s);
                lemmas.put(s, countLemmas + 1);
            }
        }
//        lemmas.forEach((key, value) -> System.out.println("countLemmas  " + key + " " + value));
        return lemmas;
    }

    public ArrayList<String> stringsForLemmas(Elements elements) {
        String string = elements.text();
        String[] strings = string.split("[^а-яА-Я]+");
        ArrayList<String> arrayList = new ArrayList<>();
        for (String str : strings) {
            List<WordformMeaning> lemmas = lookupForMeanings(str);
            if (superfluousStrings(lemmas)) {
                arrayList.add(String.valueOf(lemmas.get(0).getLemma()));
//                System.out.println("stringsForLemmas  " + String.valueOf(lemmas.get(0).getLemma()));
            }
        }
//        arrayList.forEach(System.out::println);
        return arrayList;
    }

    private boolean superfluousStrings(List<WordformMeaning> lemmas) {
        String string;
        try {
            string = lemmas.get(0).getMorphology().toString();
            switch (string) {
                case "[МЕЖД]", "[СОЮЗ]", "[ПРЕДЛ]", "[ЧАСТ]":
                    System.out.println("lemma false  " + string);
                    return false;
                default:
//                System.out.println("lemma true  " + string);
                    return true;
            }
        } catch (Exception ex) {
            return false;
        }
    }
}
