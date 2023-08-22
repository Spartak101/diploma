package searchengine.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.SitesList;
import searchengine.dto.parsing.MarkStop;
import searchengine.dto.response.ResponseSearch;
import searchengine.dto.response.ResponseObjectIndexing;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.Site;
import searchengine.services.indexing.IndexingServiceImpl;
import searchengine.services.search.SearchServiceImpl;
import searchengine.services.statistics.StatisticsService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
@Slf4j
public class ApiController {
    private MarkStop markStop = new MarkStop();
    private final SitesList sites;

    private IndexingServiceImpl indexingServiceImpl;
    private SearchServiceImpl searchServiceImpl;

    private final StatisticsService statisticsService;

    public ApiController(SitesList sites, StatisticsService statisticsService, IndexingServiceImpl indexingServiceImpl, SearchServiceImpl searchServiceImpl) {
        this.sites = sites;
        this.statisticsService = statisticsService;
        this.indexingServiceImpl = indexingServiceImpl;
        this.searchServiceImpl = searchServiceImpl;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/sites")
    public List<Site> list() {
        Iterable<Site> siteIterable = indexingServiceImpl.getSiteRepository().findAll();
        ArrayList<Site> sites = new ArrayList<>();
        for (Site site : siteIterable) {
            sites.add(site);
        }
        return sites;
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<ResponseObjectIndexing> startIndexing() throws IOException {
        if (!markStop.isMarkStop()) {
            return ResponseEntity.badRequest().body(new ResponseObjectIndexing(false, "Индексация уже запущена"));
        }
        markStop.setMarkStop(false);
        List<String> list = indexingServiceImpl.initialisationArrayPath();
        for (int i = 0; i < list.size(); i++) {
            String pathHtml = list.get(i);
            new Thread(() -> {
                try {
                    indexingServiceImpl.InitialisationIndexing(pathHtml, markStop);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
        return ResponseEntity.ok().body(new ResponseObjectIndexing(true, ""));
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<ResponseObjectIndexing> stopIndexing() {
        if (!markStop.isMarkStop()) {
            markStop.setMarkStop(true);
            return ResponseEntity.ok().body(new ResponseObjectIndexing(true, ""));
        }
        return ResponseEntity.badRequest().body(new ResponseObjectIndexing(false, "Индексация не запущена"));
    }

    @PostMapping("/indexPage")
    public ResponseEntity<ResponseObjectIndexing> updatePage(String url) throws IOException {
        if (indexingServiceImpl.pageRefresh(url)) {
            return ResponseEntity.ok().body(new ResponseObjectIndexing(true, ""));
        } else {
            return ResponseEntity.badRequest().body(new ResponseObjectIndexing(false, "Данная страница находится за пределами индексированных сайтов"));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Object> search(String query, int offset, int limit, String site) {
        System.out.println(query + "\n"
        + offset + "\n"
        + limit + "\n"
        + site);
        if (searchServiceImpl.queryIsEmpty(query)){
            System.out.println("Bad");
            return ResponseEntity.badRequest().body(new ResponseSearch());
        }

        return ResponseEntity.ok().body(searchServiceImpl.searchResponce(query, offset, limit, site));
    }
}
