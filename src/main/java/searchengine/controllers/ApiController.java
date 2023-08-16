package searchengine.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.SitesList;
import searchengine.dto.parsing.MarkStop;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.Site;
import searchengine.services.RepositoryService;
import searchengine.services.StatisticsService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api")
@Slf4j
public class ApiController {
    private MarkStop markStop = new MarkStop();
    private final SitesList sites;

    private RepositoryService repositoryService;

    private final StatisticsService statisticsService;

    public ApiController(SitesList sites, StatisticsService statisticsService, RepositoryService repositoryService) {
        this.sites = sites;
        this.statisticsService = statisticsService;
        this.repositoryService = repositoryService;
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
    public ResponseEntity startIndexing() throws IOException {
        if (!markStop.isMarkStop()) {
            return ResponseEntity.badRequest().body("{\n" +
                    "\"result\": \"false\",\n" +
                    "\"error\": \"Индексация уже запущена\"\n" +
                    "}");
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
        return ResponseEntity.ok().body("{\n" +
                "\"result\": \"true\"\n" +
                "}");
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity stopIndexing() {
        if (!markStop.isMarkStop()) {
            markStop.setMarkStop(true);
            return ResponseEntity.ok().body("{\n" +
                    "\"result\": \"true\"\n" +
                    "}");
        }
        return ResponseEntity.badRequest().body("{\n" +
                "\"result\": \"false\",\n" +
                "\"error\": \"Индексация не запущена\"\n" +
                "}");
    }

    @PostMapping("/indexPage")
    public ResponseEntity updatePage(String url) throws IOException {
        if (repositoryService.pageRefresh(url)) {
            return ResponseEntity.ok().body("{\n" +
                    "\"result\": \"true\"\n" +
                    "}");
        } else {
            return ResponseEntity.badRequest().body("{\n" +
                    "\"result\": \"false\"\n" +
                    "\"error\": \"Данная страница находится за пределами индексированных сайтов\"\n" +
                    "}");
        }
    }


}
