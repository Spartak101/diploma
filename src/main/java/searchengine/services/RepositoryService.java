package searchengine.services;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.parsing.MarkStop;
import searchengine.dto.parsing.ParseHtml;
import searchengine.dto.parsing.RequestStartTime;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

@Service
@Getter
public class RepositoryService {
    private SitesList sites;
    private MarkStop markStop;
    private HashSet<String> allLink = new HashSet<>();
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private IndexObjectRepository indexObjectRepository;
    private StopObjectRepository stopObjectRepository;

    @Autowired
    public RepositoryService(SitesList sites, SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexObjectRepository indexObjectRepository, StopObjectRepository stopObjectRepository) {
        this.sites = sites;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexObjectRepository = indexObjectRepository;
        this.stopObjectRepository = stopObjectRepository;
    }

    public List<String> initialisationArrayPath() {
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
        for (String s : urlList) {
            System.out.println("init " + s);
        }
        return urlList;
    }

    public void InitialisationIndexing(String pathHtml, MarkStop markStop) throws IOException {
        Site site = new Site();
        RequestStartTime startTime = new RequestStartTime();
        String path = normalisePathParent(pathHtml);
        site.setUrl(path);
        site.setStatusTime(new Date());
        site.setStatus(Status.INDEXING);
        ParseHtml parseHtml = new ParseHtml(site, path, path, allLink, markStop, startTime, siteRepository, pageRepository, lemmaRepository, indexObjectRepository, stopObjectRepository);
        ArrayList<String> url = new ForkJoinPool().invoke(parseHtml);
        Site siteIndexed = siteRepository.findById(siteRepository.findIdByUrl(path));
        siteIndexed.setStatus(Status.INDEXED);
        siteIndexed.setStatusTime(new Date());
        siteRepository.save(siteIndexed);
    }

    private String normalisePathParent(String pathParent) {
        String string = pathParent.replaceAll("www.", "");
        if (string.charAt(string.length() - 1) != '/') {
            return string + "/";
        }
        return string;
    }
}
