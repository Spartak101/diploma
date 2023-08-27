package searchengine.services.search;

import com.github.demidko.aot.WordformMeaning;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.ArrayUtils;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.response.SnippetEntity;
import searchengine.model.Status;
import searchengine.services.indexing.IndexingServiceImpl;
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

import static com.github.demidko.aot.WordformMeaning.lookupForMeanings;
import static searchengine.services.indexing.IndexingServiceImpl.normalisePathParent;

@Service
@Getter
public class SearchServiceImpl implements SearchService {
    private final SitesList sites;
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private IndexObjectRepository indexObjectRepository;

    @Autowired
    public SearchServiceImpl(SitesList sites, SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexObjectRepository indexObjectRepository) {
        this.sites = sites;
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
        HashSet<String> arrayLemmasQuery = new HashSet<>();
        arrayLemmasQuery.addAll(lemmaServiceImpl.stringsForLemmas(query));
        int site_id = 0;
        if (site != null) {
            site_id = siteRepository.findIdByUrl(site);
        }
        ArrayList<String> sortedFrequencyLemmas = frequencyLemmaQuery(arrayLemmasQuery, site_id);
        ArrayList<ResponseSearchData> responseSearchData = responseObjectArrayList(sortedFrequencyLemmas, site);
        if (!isTheSiteIndexed(site)) {
            responseSearch.setError("Индексация не завершена! Ответ на запрос будет не полон!");
        }
        responseSearch.setError("");
        responseSearch.setResult(true);
        responseSearch.setCount(responseSearchData.size());
        responseSearch.setData(responseSearchData);
        return responseSearch;
    }


    public boolean queryIsEmpty(String query) {
        String[] strings = query.split("[^а-яёЁА-Я]");
        if (ArrayUtils.isEmpty(strings)) {
            return true;
        }
        return false;
    }


    public ArrayList<String> frequencyLemmaQuery(Set<String> array, int site_id) {
        HashMap<String, Integer> countLemma = new HashMap<>();
        float allPages;
        if (site_id == 0) {
            collectionAtZero(array, countLemma);
        } else {
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
        }
        return (ArrayList<String>) countLemma.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }


    public void collectionAtZero(Set<String> array, HashMap<String, Integer> countLemma) {
        float allPages = pageRepository.findCountPageAll();
        for (String s : array) {
            ArrayList<Lemma> lemmas = lemmaRepository.findAllByLemma(s);
            if (lemmas == null) {
                continue;
            }
            int frequencyAll = 0;
            for (Lemma l : lemmas) {
                frequencyAll += l.getFrequency();
            }
            if ((frequencyAll / allPages) > 0.7) {
                System.out.println(allPages / frequencyAll);
                continue;
            }
            countLemma.put(s, frequencyAll);
        }
    }


    public ArrayList<ResponseSearchData> responseObjectArrayList(ArrayList<String> sortedFrequencyLemmas, String site) {
        ArrayList<ResponseSearchData> array;
        LinkedList<Page> pagesOne;
        LinkedList<Page> pagesNext;
        if (site == null) {
            pagesOne = pagesWithLemma(sortedFrequencyLemmas.get(0));
            for (int i = 1; i < sortedFrequencyLemmas.size(); i++) {
                pagesNext = pagesWithLemma(sortedFrequencyLemmas.get(i));
                pagesOne.forEach(pagesNext::removeFirstOccurrence);
            }
        } else {
            pagesOne = lemmaPagesOnTheWebsite(sortedFrequencyLemmas.get(0), site);
            for (int i = 1; i < sortedFrequencyLemmas.size(); i++) {
                pagesNext = lemmaPagesOnTheWebsite(sortedFrequencyLemmas.get(i), site);
                pagesOne.forEach(pagesNext::removeFirstOccurrence);
            }
        }
        array = snippetGenerator(pagesOne, sortedFrequencyLemmas);
        return array;
    }


    public LinkedList<Page> pagesWithLemma(String lemma) {
        ArrayList<Integer> lemma_id = lemmaRepository.findAllIdByLemma(lemma);
        ArrayList<Integer> pagesId = indexObjectRepository.findAllByLemma_idIn(lemma_id);
        LinkedList<Page> pages = new LinkedList();
        pages.addAll(pageRepository.findAllById(pagesId));
        return pages;
    }


    public LinkedList<Page> lemmaPagesOnTheWebsite(String lemma, String site) {
        int site_id = siteRepository.findIdByUrl(site);
        ArrayList<Integer> lemma_id = lemmaRepository.findAllIdByLemmaAndSite_id(lemma, site_id);
        ArrayList<Integer> pagesId = indexObjectRepository.findAllByLemma_idIn(lemma_id);
        LinkedList<Page> pages = new LinkedList();
        pages.addAll(pageRepository.findAllById(pagesId));
        return pages;
    }

    public ArrayList<ResponseSearchData> snippetGenerator(LinkedList<Page> pagesList, ArrayList<String> lemmasQuery) {
        ArrayList<ResponseSearchData> snippets = new ArrayList<>();
        for (Page page : pagesList) {
            ResponseSearchData searchData = new ResponseSearchData();
            String snippet = snippetExtractor(page, lemmasQuery);
            searchData.setSnippet(snippet);
            String siteUrl = siteRepository.findById(page.getSite().getId()).getUrl();
            String siteUrlCropped = siteUrl.substring(0, siteUrl.length() - 1);
            searchData.setSite(siteUrlCropped);
            searchData.setUri("/" + page.getPath());
            Document doc = Jsoup.parse(page.getContent());
            searchData.setTitle(doc.select("title").text());
            for (Site site : sites.getSites()) {
                String url = normalisePathParent(site.getUrl());
                if (url.equals(siteUrl)) {
                    String name = site.getName();
                    searchData.setSiteName(name);
                }
            }
            if (searchData.getSiteName() == null) {
                searchData.setSiteName(Jsoup.parse(page.getContent()).select("title").text());
            }
            searchData.setRelevance(revalenceCalculator(lemmasQuery, page));
            snippets.add(searchData);
        }
        return snippets;
    }

    public String snippetExtractor(Page page, ArrayList<String> lemmasQuery) {
        LemmaServiceImpl lemmaService = new LemmaServiceImpl();
        ArrayList<Element> elements = lemmaService.elementsInDoc(Jsoup.parse(page.getContent()));
        ArrayList<SnippetEntity> snippetArray = new ArrayList<>();
        for (Element el : elements) {
            extractedSnippet(lemmasQuery, snippetArray, el);
        }
        Collections.sort(snippetArray, Comparator.comparing(SnippetEntity::getNumberOfMatches));
        StringBuilder stringBuilder = new StringBuilder();
        int countWordInSnippet = 0;

        for (SnippetEntity snippetEntity : snippetArray) {
            if (countWordInSnippet < 30) {
                stringBuilder.append(snippetEntity.getSnippet());
                countWordInSnippet += snippetEntity.getNumberOfWords();
            }
        }
        return stringBuilder.toString();
    }


    private void extractedSnippet(ArrayList<String> lemmasQuery, ArrayList<SnippetEntity> snippetArray, Element el) {
        StringBuilder stringBuilder = new StringBuilder();
        int count = 0;
        String element = el.toString();
        if (el.text().matches("^((8|\\+7)[\\- ]?)?(\\(?\\d{3}\\)?[\\- ]?)?[\\d\\- ]{7,10}$")) {
            return;
        }
        String[] components = element.split("<([\\s\\S]+?)>");
        String[] tags = element.split("(?<=\\>).+?(?=\\<)");
        int wordLenth = 0;
        for (int i = 1; i < components.length; i++) {
            StringBuilder sb = new StringBuilder();
            String[] words = components[i].split(" ");
            wordLenth += words.length;
            for (int j = 0; j < words.length; i++) {
                for (String l : lemmasQuery) {
                    String w = words[j].replaceAll("[^а-яёЁА-Я]", "");
                    if (w.isEmpty()) {
                        continue;
                    }
                    if (wordComparator(w, l)) {
                        System.out.println(w);
                        words[j] = "<b>" + words[j] + "</b>";
                        count++;
                    }
                }
            }
            if (count > 0) {
                for (String s : words) {
                    sb.append(s.trim() + " ");
                }
            }
            components[i] = sb.toString();
        }
        for (int i = 0; i < tags.length; i++) {
            stringBuilder.append(tags[i]);
            if ((i + 1) < components.length) {
                stringBuilder.append(components[i + 1]);
            }
        }
        String snippet = stringBuilder.toString();
        SnippetEntity snippetEntity = new SnippetEntity(snippet, count, wordLenth);
        snippetArray.add(snippetEntity);
    }

    public boolean wordComparator(String world, String lemma) {
        List<WordformMeaning> lemmas = lookupForMeanings(world);
//   TODO:    КОСТЫЛЬ!!! Надо менять или дополнять библиотеку.
        if (lemmas.isEmpty()) {
            return false;
        }
        String lemmaFromTheWord = String.valueOf(lemmas.get(0).getLemma());
        if (lemmaFromTheWord.equals(lemma)) {
            return true;
        }
        return false;
    }

    public int revalenceCalculator(ArrayList<String> lemmasQuery, Page page) {
        int absoluteRevalence = 0;
        for (String s : lemmasQuery) {
            int site_id = pageRepository.findSite_idById(page.getId());
            int lemma_id = 0;
            try {
                lemma_id = lemmaRepository.findIdByLemmaAndSite_id(s, site_id);
            } catch (Exception e) {
                continue;
            }
            if (lemma_id != 0) {
                absoluteRevalence += indexObjectRepository.findById(lemma_id).get().getRunk();
            }
        }
        return absoluteRevalence;
    }

    public ArrayList<ResponseSearchData> normalizerRelevance(ArrayList<ResponseSearchData> array) {
        ArrayList<ResponseSearchData> arraySort = array;
        float maxRelevance = 0;
        for (ResponseSearchData r : arraySort) {
            if (r.getRelevance() < maxRelevance) {
                maxRelevance = r.getRelevance();
            }
        }
        for (ResponseSearchData r : arraySort) {
            float relativeRelevance = r.getRelevance() / maxRelevance;
            r.setRelevance(relativeRelevance);
        }
        arraySort.sort(Comparator.comparing(ResponseSearchData::getRelevance));
        return arraySort;
    }

    public boolean isTheSiteIndexed(String site) {
        if (site == null) {
            List<searchengine.model.Site> siteList = siteRepository.findAll();
            for (searchengine.model.Site s : siteList) {
                if (!s.getStatus().equals(Status.INDEXED)) {
                    return false;
                }
            }
        }
        searchengine.model.Site siteSolo = siteRepository.findByUrl(site);
        if (!siteSolo.getStatus().equals(Status.INDEXED)) {
            return false;
        }
        return true;
    }
}
