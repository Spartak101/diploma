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
        HashSet<String> urlMap = new HashSet<>();
        List<String> urlList = new ArrayList<>();
        List<Site> listDB = new ArrayList<>();
        try {
            listDB = siteRepository.findAll();
            listDB.forEach(site -> System.out.println(site.getUrl()));
        } catch (Exception ex) {
            // ex.printStackTrace();
            System.out.println("База данных пуста!");
        }
        if (!listDB.isEmpty()) {
            for (Site s : listDB) {
                for (searchengine.config.Site t : sites.getSites()) {
                    if (s.getStatus().toString() == "INDEXING" | s.getUrl() != normalisePathParent(t.getUrl())) {
                        urlMap.add(normalisePathParent(t.getUrl()));
                    }
                }
            }
            urlList = urlMap.stream().toList();
        } else {
            urlList = sites.getSites().stream().map(Site -> Site.getUrl()).collect(Collectors.toList());
        }
        urlList.forEach(System.out::println);
        return urlList;
    }

    public void InitialisationIndexing(String pathHtml, MarkStop markStop) throws IOException {
        Site site = new Site();
        String path = normalisePathParent(pathHtml);
        try {
            site = siteRepository.findByUrl(pathHtml);
        } catch (Exception e) {
            site.setUrl(path);
            site.setStatusTime(new Date());
            site.setStatus(Status.INDEXING);
        }
        RequestStartTime startTime = new RequestStartTime();
        ArrayList<String> absUrl = stopObjectRepository.findAllBySite_Id(site.getId());
        HashSet<String> allLink = (HashSet<String>) site.getPages().stream().map(page -> path + page.getPath()).collect(Collectors.toSet());
        ParseHtml parseHtml = new ParseHtml(site, path, path, allLink, markStop, startTime, siteRepository, pageRepository, lemmaRepository, indexObjectRepository, stopObjectRepository);
        parseHtml.initializationOfAbsurl(absUrl);
        ArrayList<String> url = new ForkJoinPool().invoke(parseHtml);
        if (!markStop.isMarkStop()) {
            Site siteIndexed = siteRepository.findByUrl(path);
            siteIndexed.setStatus(Status.INDEXED);
            siteIndexed.setStatusTime(new Date());
            siteRepository.save(siteIndexed);
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
