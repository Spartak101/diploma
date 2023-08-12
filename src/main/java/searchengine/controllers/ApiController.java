package searchengine.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.SitesList;
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
    private boolean markStop = false;
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
        if (IsTheClassRunning("searchengine.dto.parsing.ParseHtml")) {
            return ResponseEntity.badRequest().body("{\n" +
                    "\"result\": \"false\",\n" +
                    "\"error\": \"Индексация уже запущена\"\n" +
                    "}");
        }
        repositoryService.InitialisationIndexing(sites, markStop);
        return ResponseEntity.ok().body("{\n" +
                "\"result\": \"true\"\n" +
                "}");
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity stopIndexing() {
        if (IsTheClassRunning("searchengine.dto.parsing.ParseHtml")) {
            markStop = true;
            return ResponseEntity.ok().body("{\n" +
                    "\"result\": \"true\"\n" +
                    "}");
        }
        return ResponseEntity.badRequest().body("{\n" +
                "\"result\": \"false\",\n" +
                "\"error\": \"Индексация не запущена\"\n" +
                "}");
    }

    @PostMapping("/api/indexPage/")
    public Object updatePage(String url) {
        return null;
    }

    private boolean IsTheClassRunning(String className) {
        Stream<String> stream = Arrays.stream(Thread.currentThread().getStackTrace()).map(stackTraceElement -> stackTraceElement.getClassName().toString());
        Object[] arrayClassInThread = stream.toArray();
        for (int i = 0; i < arrayClassInThread.length - 1; i++) {
            if (arrayClassInThread[i].toString().equals(className)) {
                return true;
            }
        }
        return false;
    }
}
