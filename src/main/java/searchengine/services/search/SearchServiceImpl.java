package searchengine.services.search;

import com.github.demidko.aot.WordformMeaning;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.ArrayUtils;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.response.SnippetEntity;
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

@Service
@Getter
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final SitesList sites;
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private IndexObjectRepository indexObjectRepository;


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
        array = snippetGenerator(pagesOne, sortedFrequencyLemmas);
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
            searchData.setTitle(Document.createShell(page.getContent()).select("title").text());
            sites.getSites().stream().filter(site -> site.getUrl().equals(siteUrl)).map(Site::getName).forEach(searchData::setSiteName);
            if (searchData.getSiteName() == null) {
                searchData.setSiteName(Document.createShell(page.getContent()).select("title").text());
            }

        }
        return snippets;
    }

    public HashMap<String, Integer> snippetExtractor(Page page, ArrayList<String> lemmasQuery) {
        LemmaServiceImpl lemmaService = new LemmaServiceImpl();
        ArrayList<Element> elements = lemmaService.elementsInDoc(Document.createShell(page.getContent()));
        ArrayList<SnippetEntity> snippetArray = new ArrayList<>();
        elements.forEach(el -> extractedSnippet(lemmasQuery, snippetArray, el));
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
        String[] text = el.toString().split("<([\\s\\S]+?)>");
        String[] tags = el.toString().split(text[1]);
        String[] words = text[1].split(" ");
        for (int i = 0; i < words.length; i++) {
            for (String l : lemmasQuery) {
                if (wordComparator(words[i], l)) {
                    words[i] = "<b>" + words[i] + "</b>";
                    count++;
                }
            }
        }
        if (count > 0) {
            stringBuilder.append(tags[0]);
            for (String s : words) {
                stringBuilder.append(s + " ");
            }
            stringBuilder.append(tags[1]);
        }
        String snippet = stringBuilder.toString();
        SnippetEntity snippetEntity = new SnippetEntity(snippet, count, words.length);
        snippetArray.add(snippetEntity);
    }

    public boolean wordComparator(String world, String lemma) {
        List<WordformMeaning> lemmas = lookupForMeanings(world);
        String lemmaFromTheWord = String.valueOf(lemmas.get(0).getLemma());
        if (lemmaFromTheWord.equals(lemma)) {
            return true;
        }
        return false;
    }
}
