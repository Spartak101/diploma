package searchengine.services.lemma;

import com.github.demidko.aot.WordformMeaning;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LemmaServiceImpl implements LemmaService {
    private Document doc;

    public LemmaServiceImpl(Document doc) {
        this.doc = doc;
    }

    public LemmaServiceImpl() {
    }

    @Override
    public HashMap<String, Integer> lemmas() {
        Elements elementLinks = doc.select("a");
        Elements elementSpan = doc.select("span");
        Elements elementParagraph = doc.select("p");
        HashMap<String, Integer> lemmas = new HashMap<>();
        for (int i = 1; i < 7; i++) {
            Elements headings = doc.select("h" + i);
            lemmas.putAll(countLemmas(stringsForLemmas(headings.text()), lemmas));
        }
        lemmas.putAll(countLemmas(stringsForLemmas(elementLinks.text()), lemmas));
        lemmas.putAll(countLemmas(stringsForLemmas(elementSpan.text()), lemmas));
        lemmas.putAll(countLemmas(stringsForLemmas(elementParagraph.text()), lemmas));
        return lemmas;
    }
    @Override
    public HashMap<String, Integer> countLemmas(ArrayList<String> arrayList, HashMap<String, Integer> lemmas) {
        for (String s : arrayList) {
            if (!lemmas.containsKey(s)) {
                lemmas.put(s, 1);
            } else {
                Integer countLemmas = lemmas.get(s);
                lemmas.put(s, countLemmas + 1);
            }
        }
        return lemmas;
    }
    @Override
    public ArrayList<String> stringsForLemmas(String string) {
        String[] strings = string.split("[^а-яА-Я]+");
        ArrayList<String> arrayList = Arrays.stream(strings)
                .map(WordformMeaning::lookupForMeanings)
                .filter(this::superfluousStrings).map(lemmas -> String.valueOf(lemmas.get(0).getLemma()))
                .collect(Collectors.toCollection(ArrayList::new));
        return arrayList;
    }
    @Override
    public boolean superfluousStrings(List<WordformMeaning> lemmas) {
        String string;
        try {
            string = lemmas.get(0).getMorphology().toString();
            switch (string) {
                case "[МЕЖД]", "[СОЮЗ]", "[ПРЕДЛ]", "[ЧАСТ]":
                    return false;
                default:
                    return true;
            }
        } catch (Exception ex) {
            return false;
        }
    }
}
