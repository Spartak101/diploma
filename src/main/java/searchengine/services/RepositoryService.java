package searchengine.services;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.parsing.ParseHtml;
import searchengine.dto.parsing.RequestStartTime;
import searchengine.model.Site;
import searchengine.repository.IndexObjectRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

@Service
@Getter
public class RepositoryService {
    private SitesList sites;
    private boolean markStop;
    private HashSet<String> allLink = new HashSet<>();
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private IndexObjectRepository indexObjectRepository;

    @Autowired
    public RepositoryService(SitesList sites, SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexObjectRepository indexObjectRepository) {
        this.sites = sites;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexObjectRepository = indexObjectRepository;
    }

    private List<String> initialisationArrayPath() {
        List<String> urlList = new ArrayList<>();
        List<Site> listDB = new ArrayList<>();
        try {
            listDB = siteRepository.findAll();
            listDB.forEach(System.out::println);
        } catch (Exception ex) {
            // ex.printStackTrace();
            System.out.println("База данных пуста!");
        }
        if (!listDB.isEmpty()) {
            for (Site s : listDB) {
                for (searchengine.config.Site t : sites.getSites()) {
                    if (s.getStatus().toString() == "INDEXING" | s.getUrl() != t.getUrl()) {
                        urlList.add(t.getUrl());
                    }
                }
            }
        } else {
            urlList = sites.getSites().stream().map(Site -> Site.getUrl()).collect(Collectors.toList());
        }
//        for (String s : urlList) {
//            System.out.println("init " + s);
//        }
        return urlList;
    }

    public void InitialisationIndexing(SitesList sites, boolean markStop) throws IOException {
        this.sites = sites;
        this.markStop = markStop;
        List<String> urlList = initialisationArrayPath();
        for (int i = 0; i < urlList.size(); i++) {
            RequestStartTime startTime = new RequestStartTime();
            ParseHtml parseHtml = new ParseHtml(normalisePathParent(urlList.get(i)), normalisePathParent(urlList.get(i)), allLink, markStop, startTime, siteRepository, pageRepository, lemmaRepository, indexObjectRepository);
            ArrayList<String> url = new ForkJoinPool().invoke(parseHtml);
        }
    }
    private String normalisePathParent(String pathParent) {
        String string = pathParent.replaceAll("www.", "");
        if (string.charAt(string.length() - 1) != '/') {
            return string + "/";
        }
        return string;
    }
}
