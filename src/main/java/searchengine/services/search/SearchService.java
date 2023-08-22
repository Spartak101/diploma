package searchengine.services.search;

import searchengine.dto.response.ResponseSearch;
import searchengine.dto.response.ResponseSearchData;
import searchengine.model.Page;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

public interface SearchService {
    ResponseSearch searchResponce(String query, int offset, int limit, String site);

    boolean queryIsEmpty(String query);

    ArrayList<String> frequencyLemmaQuery(Set<String> array, int site_id);

    void collectionAtZero(Set<String> array, HashMap<String, Integer> countLemma);

    ArrayList<ResponseSearchData> responseObjectArrayList(ArrayList<String> sortedFrequencyLemmas, String site);

    LinkedList<Page> pagesWithLemma(String lemma);

    LinkedList<Page> lemmaPagesOnTheWebsite(String lemma, String site);
}
