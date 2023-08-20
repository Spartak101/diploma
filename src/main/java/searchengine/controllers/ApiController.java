package searchengine.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.SitesList;
import searchengine.dto.parsing.MarkStop;
import searchengine.dto.response.Ok;
import searchengine.dto.response.ResponseObjectIndexing;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.Site;
import searchengine.services.RepositoryService;
import searchengine.services.ResponseService;
import searchengine.services.StatisticsService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
@Slf4j
public class ApiController {
    private MarkStop markStop = new MarkStop();
    private final SitesList sites;

    private RepositoryService repositoryService;
    private ResponseService responseService;

    private final StatisticsService statisticsService;

    public ApiController(SitesList sites, StatisticsService statisticsService, RepositoryService repositoryService, ResponseService responseService) {
        this.sites = sites;
        this.statisticsService = statisticsService;
        this.repositoryService = repositoryService;
        this.responseService = responseService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/sites")
    public List<Site> list() {
        Iterable<Site> siteIterable = repositoryService.getSiteRepository().findAll();
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
        List<String> list = repositoryService.initialisationArrayPath();
        for (int i = 0; i < list.size(); i++) {
            String pathHtml = list.get(i);
            new Thread(() -> {
                try {
                    repositoryService.InitialisationIndexing(pathHtml, markStop);
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
        if (repositoryService.pageRefresh(url)) {
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
        if (responseService.queryIsEmpty(query)){
            System.out.println("Bad");
            return ResponseEntity.badRequest().body(new Ok());
        }

        return ResponseEntity.ok().body(responseService.searchResponce(query, offset, limit, site));
    }
}
