package searchengine.services.indexing;

import lombok.Getter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.parsing.MarkStop;
import searchengine.dto.parsing.ParseHtml;
import searchengine.dto.parsing.RequestStartTime;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.model.StopObject;
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
public class IndexingServiceImpl implements IndexingService {
    private SitesList sites;
    private MarkStop markStop = new MarkStop();
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private IndexObjectRepository indexObjectRepository;
    private StopObjectRepository stopObjectRepository;

    @Autowired
    public IndexingServiceImpl(SitesList sites, SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexObjectRepository indexObjectRepository, StopObjectRepository stopObjectRepository) {
        this.sites = sites;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexObjectRepository = indexObjectRepository;
        this.stopObjectRepository = stopObjectRepository;
    }

    @Override
    public List<String> initialisationArrayPath() {
        HashSet<String> urlMap = new HashSet<>();
        List<String> urlList;
        List<Site> listDB = new ArrayList<>();
        try {
            listDB = siteRepository.findAll();
        } catch (Exception ex) {
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

    @Override
    public void InitialisationIndexing(String pathHtml, MarkStop markStop) throws IOException {
        Site site;
        String path = normalisePathParent(pathHtml);
        site = siteRepository.findByUrl(pathHtml);
        if (site == null) {
            site = new Site();
            site.setUrl(path);
            site.setStatusTime(new Date());
            site.setStatus(Status.INDEXING);
        }
        RequestStartTime startTime = new RequestStartTime();
        HashSet<String> absUrl = new HashSet<>();
        HashSet<String> allLink = new HashSet<>();
        if (site.getId() != 0) {
            absUrl.addAll(stopObjectRepository.findAllBySite_Id(site.getId()));
            ArrayList<StopObject> arrayList = stopObjectRepository.findAllBySite_id(site.getId());
            stopObjectRepository.deleteAll(arrayList);
            allLink = pathToThePageFromBD(path);
        }
        ParseHtml parseHtml = new ParseHtml(absUrl, site, path, path, allLink, markStop, startTime, siteRepository, pageRepository, lemmaRepository, indexObjectRepository, stopObjectRepository);
        new ForkJoinPool().invoke(parseHtml);
        if (!markStop.isMarkStop()) {
            Site siteIndexed = siteRepository.findByUrl(path);
            siteIndexed.setStatus(Status.INDEXED);
            siteIndexed.setStatusTime(new Date());
            siteRepository.save(siteIndexed);
        }
    }

    @Override
    public String normalisePathParent(String pathParent) {
        String string = pathParent.replaceAll("www.", "");
        if (string.charAt(string.length() - 1) != '/') {
            return string + "/";
        }
        return string;
    }

    @Override
    public HashSet<String> pathToThePageFromBD(String pathHtml) {
        HashSet<String> pathSet = new HashSet<>();
        Site site = siteRepository.findByUrl(pathHtml);
        int siteId = site.getId();
        String siteUrl = site.getUrl();
        ArrayList<String> pagePath = pageRepository.findPathBySite_id(siteId);
        for (String s : pagePath) {
            pathSet.add(siteUrl + s);
        }
        return pathSet;
    }
    @Override
    public boolean pageRefresh(String url) throws IOException {
        String pathHtml = url.replaceAll("https?:\\/\\/\\w+\\.\\w+\\/", "");
        String pathParent = url.replaceAll(pathHtml, "");
        Site site = siteRepository.findByUrl(pathParent);
        Connection.Response response;
        if (site == null) {
            return false;
        } else {
            response = Jsoup.connect(pathParent).followRedirects(true).execute();
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                    .referrer("http://www.google.com")
                    .get();
            Page page = pageRepository.findBySite_idAndPath(site.getId(), pathHtml);
            if (page != null) {
                pageRepository.updateContentPage(page.getId(), String.valueOf(doc));
            } else {
                page = new Page();
                page.setSite(site);
                page.setCode(response.statusCode());
                page.setContent(String.valueOf(doc));
                page.setPath(pathHtml);
                pageRepository.save(page);
            }
            return true;
        }
    }
}

