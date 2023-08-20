package searchengine.services;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.ArrayUtils;
import searchengine.dto.parsing.LemmasOfPage;
import searchengine.dto.response.Ok;
import searchengine.dto.response.ResponseObject;
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
public class ResponseService {
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private IndexObjectRepository indexObjectRepository;

    @Autowired
    public ResponseService(SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexObjectRepository indexObjectRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexObjectRepository = indexObjectRepository;
    }

    public Ok searchResponce(String query, int offset, int limit, String site) {
        Ok ok = new Ok();
        if (queryIsEmpty(query)) {
            ok.setError("Задан пустой поисковый запрос");
            return ok;
        }
        LemmasOfPage lemmasOfPage = new LemmasOfPage();
        ArrayList<String> arrayLemmasQuery = lemmasOfPage.stringsForLemmas(query);
        int site_id = siteRepository.findIdByUrl(site);
        ArrayList<String> sortedFrequencyLemmas = frequencyLemmaQuery(arrayLemmasQuery, site_id);
        ArrayList<ResponseObject> responseObjects = ok.getData();

        return ok;
    }

    public boolean queryIsEmpty(String query) {
        String[] strings = query.split("[^а-яёЁА-Я]");
        if (ArrayUtils.isEmpty(strings)) {
            return true;
        }
        return false;
    }

    private ArrayList<String> frequencyLemmaQuery(ArrayList<String> array, int site_id) {
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

    private void collectionAtZero(ArrayList<String> array, HashMap<String, Integer> countLemma) {
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

    private ArrayList<ResponseObject> responseObjectArrayList(ArrayList<String> sortedFrequencyLemmas, String site){
        ArrayList<ResponseObject> array = new ArrayList<>();
        int lemma_id = lemmaRepository.findAIdByLemma(sortedFrequencyLemmas.get(0));
//        ArrayList<Integer> pagesId = indexObjectRepository.
//        LinkedList<Page> pages = pageRepository.findAllById();
        return null;
    }
}
