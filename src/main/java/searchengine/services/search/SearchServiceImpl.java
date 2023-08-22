package searchengine.services.search;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.ArrayUtils;
import searchengine.services.lemma.LemmaServiceImpl;
import searchengine.dto.response.ResponseSearch;
import searchengine.dto.response.ResponseSearchData;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repository.IndexObjectRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Getter
public class SearchServiceImpl implements SearchService {
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private IndexObjectRepository indexObjectRepository;

    @Autowired
    public SearchServiceImpl(SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexObjectRepository indexObjectRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexObjectRepository = indexObjectRepository;
    }

    @Override
    public ResponseSearch searchResponce(String query, int offset, int limit, String site) {
        ResponseSearch responseSearch = new ResponseSearch();
        if (queryIsEmpty(query)) {
            responseSearch.setError("Задан пустой поисковый запрос");
            return responseSearch;
        }
        LemmaServiceImpl lemmaServiceImpl = new LemmaServiceImpl();
        Set<String> arrayLemmasQuery = (Set<String>) lemmaServiceImpl.stringsForLemmas(query);
        int site_id = siteRepository.findIdByUrl(site);
        ArrayList<String> sortedFrequencyLemmas = frequencyLemmaQuery(arrayLemmasQuery, site_id);
//        ArrayList<ResponseSearchData> responseSearchData = responseSearch.getData();

        return responseSearch;
    }
    @Override
    public boolean queryIsEmpty(String query) {
        String[] strings = query.split("[^а-яёЁА-Я]");
        if (ArrayUtils.isEmpty(strings)) {
            return true;
        }
        return false;
    }
    @Override
    public ArrayList<String> frequencyLemmaQuery(Set<String> array, int site_id) {
        HashMap<String, Integer> countLemma = new HashMap<>();
        float allPages;
        if (site_id == 0) {
            collectionAtZero(array, countLemma);
        }
        for (String s : array) {
            Lemma lemma = lemmaRepository.findByLemmaAndSite_id(s, site_id);
            allPages = pageRepository.findCountPageBySite_id(site_id);
            if (lemma == null) {
                continue;
            }
            if (allPages / lemma.getFrequency() > 0.7) {
                continue;
            }
            countLemma.put(s, lemma.getFrequency());
        }
        return (ArrayList<String>) countLemma.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    @Override
    public void collectionAtZero(Set<String> array, HashMap<String, Integer> countLemma) {
        float allPages;
        for (String s : array) {
            ArrayList<Lemma> lemmas = lemmaRepository.findAllByLemma(s);
            if (lemmas == null) {
                continue;
            }
            allPages = pageRepository.findCountPageAll();
            int frequencyAll = 0;
            for (Lemma l : lemmas) {
                frequencyAll += l.getFrequency();
            }
            if (allPages / frequencyAll > 0.7) {
                continue;
            }
            countLemma.put(s, frequencyAll);
        }
    }
    @Override
    public ArrayList<ResponseSearchData> responseObjectArrayList(ArrayList<String> sortedFrequencyLemmas, String site) {
        ArrayList<ResponseSearchData> array = new ArrayList<>();
        LinkedList<Page> pagesOne;
        LinkedList<Page> pagesNext;
        if (site == null) {
            pagesOne = pagesWithLemma(sortedFrequencyLemmas.get(0));
            for (int i = 1; i < sortedFrequencyLemmas.size(); i++) {
                pagesNext = pagesWithLemma(sortedFrequencyLemmas.get(i));
                pagesOne.forEach(pagesNext::removeFirstOccurrence);
            }
        }
        pagesOne = lemmaPagesOnTheWebsite(sortedFrequencyLemmas.get(0), site);
        for (int i = 1; i < sortedFrequencyLemmas.size(); i++) {
            pagesNext = lemmaPagesOnTheWebsite(sortedFrequencyLemmas.get(i), site);
            pagesOne.forEach(pagesNext::removeFirstOccurrence);
        }
        ArrayList<String> snippets = snippetGenerator(pagesOne, sortedFrequencyLemmas, site);
        return null;
    }
    @Override
    public LinkedList<Page> pagesWithLemma(String lemma) {
        ArrayList<Integer> lemma_id = lemmaRepository.findAllIdByLemma(lemma);
        ArrayList<Integer> pagesId = indexObjectRepository.findAllByLemma_idIn(lemma_id);
        LinkedList<Page> pages = (LinkedList<Page>) pageRepository.findAllById(pagesId);
        return pages;
    }
    @Override
    public LinkedList<Page> lemmaPagesOnTheWebsite(String lemma, String site) {
        int site_id = siteRepository.findIdByUrl(site);
        ArrayList<Integer> lemma_id = lemmaRepository.findAllIdByLemmaAndSite_id(lemma, site_id);
        ArrayList<Integer> pagesId = indexObjectRepository.findAllByLemma_idIn(lemma_id);
        LinkedList<Page> pages = (LinkedList<Page>) pageRepository.findAllById(pagesId);
        return pages;
    }

    public ArrayList<String> snippetGenerator(LinkedList<Page> pagesList, ArrayList<String> lemmasQuery, String site){

        return null;
    }
}
