package searchengine.dto.parsing;

import lombok.Data;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.model.Site;
import searchengine.repository.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

@Data
public class ParseHtml extends RecursiveTask<ArrayList<String>> {
    private String htmlFile;
    private HashSet<String> allLink;
    private MarkStop markStop;
    private Document doc;
    private ArrayList<String> absUrl;
    private String pathParent;
    private Site site;
    private RequestStartTime startTime;
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private IndexObjectRepository indexObjectRepository;
    private StopObjectRepository stopObjectRepository;
    private InitializationOfEntityFields initializationOfEntityFields;
    private Connection.Response response;
    private Elements element;

    public ParseHtml(Site site, String pathParent, String pathHtml, HashSet<String> allLink, MarkStop markStop, RequestStartTime startTime, SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexObjectRepository indexObjectRepository, StopObjectRepository stopObjectRepository) throws IOException {
        this.site = site;
        this.htmlFile = pathHtml;
        this.pathParent = pathParent;
        this.allLink = allLink;
        this.markStop = markStop;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexObjectRepository = indexObjectRepository;
        this.stopObjectRepository = stopObjectRepository;
        this.initializationOfEntityFields = new InitializationOfEntityFields(siteRepository, pageRepository, lemmaRepository, indexObjectRepository, stopObjectRepository);
        this.startTime = startTime;
        startTime.setDateStart(System.currentTimeMillis());
        response = Jsoup.connect(pathHtml).followRedirects(true).execute();
        switch (response.statusCode()) {
            case 200 -> {
                this.doc = Jsoup.connect(htmlFile)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                        .referrer("http://www.google.com")
                        .get();
                System.out.println("1-200 " + htmlFile);
            }
            default -> {
                absUrl = new ArrayList<>();
                System.out.println("1-error " + htmlFile + "\n" + response.statusCode());
            }
        }
        this.element = doc.select("a");
        absUrl = (ArrayList<String>) element.stream().map(element1 -> element1.absUrl("href")).collect(Collectors.toList());
    }


    @Override
    protected ArrayList<String> compute() {
        HashSet<String> name = allLink;
        ArrayList<String> name2 = new ArrayList<>();
//        ArrayList<String> stoppingList = new ArrayList<>();
        ArrayList<ParseHtml> tasks = new ArrayList<>();
        if (name.isEmpty()) {
            initializationOfEntityFields.initialisationSite(site, response.statusCode(), doc);
            initializationOfEntityFields.initialisationPage(site, pathParent, response.statusCode(), doc);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Initialisation " + pathParent);
        }
        if (absUrl == null) {
            return new ArrayList<>();
        }
        try {
            for (int i = 0; i < absUrl.size(); i++) {
                if (absUrl.get(i).matches(pathParent + "([\\/[a-z0-9-]+]+\\/?[a-z0-9-]*\\/*(.html)?\"?)")) {
                    if (!name.contains(absUrl.get(i))) {
                        name.add(absUrl.get(i));
                        long startQueryHtml = System.currentTimeMillis();
                        if ((startQueryHtml - startTime.getDateStart()) < 5000) {
                            Thread.sleep(5000);
                        }
                        System.out.println("2 " + absUrl.get(i));
                        ParseHtml html;
                        try {
                            if (!markStop.isMarkStop()) {
                                html = new ParseHtml(site, pathParent, absUrl.get(i), allLink, markStop, startTime, siteRepository, pageRepository, lemmaRepository, indexObjectRepository, stopObjectRepository);
                                initializationOfEntityFields.initialisationPage(site, absUrl.get(i), response.statusCode(), doc);
                                html.fork();
                                tasks.add(html);
                            } else {
//                                stoppingList.add(absUrl.get(i));
                                initializationOfEntityFields.initialisationStopObject(site, absUrl.get(i));
                                Thread.sleep(100);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            for (ParseHtml url : tasks) {
                name.addAll(url.join());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        for (String s : name) {
            name2.add(s);
        }

        return name2;
    }

    public ParseHtml initializationOfAbsurl(ArrayList<String> pathArray){
        this.absUrl.addAll(pathArray);
        return null;
    }
}