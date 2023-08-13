package searchengine.dto.parsing;

import lombok.Data;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.*;
import searchengine.repository.IndexObjectRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.RepositoryService;

import java.util.*;

@Data
public class InitializationOfEntityFields {

    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private IndexObjectRepository indexObjectRepository;

    public InitializationOfEntityFields(SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexObjectRepository indexObjectRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexObjectRepository = indexObjectRepository;
    }

    protected Site initialisationSite(String pathParent, int httpStatusCode, Document doc) {
        Site site = new Site();
        switch (httpStatusCode) {
            case 200 -> {
                site.setStatus(Status.INDEXING);
                site.setStatusTime(new Date());
                site.setLastError(HttpStatusCodeError.httpStatusCodeError(httpStatusCode));
                site.setUrl(pathParent);
                site.setName(SiteName.siteName(doc));
            }
            default -> {
                site.setStatus(Status.FAILED);
                site.setStatusTime(new Date());
                site.setLastError(HttpStatusCodeError.httpStatusCodeError(httpStatusCode));
                site.setUrl(pathParent);
                site.setName("");
            }
        }
        siteRepository.save(site);
        return site;
    }

    protected void initialisationPage(Site site, String pathHtml, int httpCode, Document doc) {
        Page page = new Page();
        page.setCode(httpCode);
//        System.out.println("doc   " + String.valueOf(doc));
        page.setContent(String.valueOf(doc));
        String path = pathHtml.replaceAll(site.getUrl(), "");
        page.setPath(path);
        page.setSite(site);
        pageRepository.save(page);
        initialisationLemmas(doc, site, page);
    }

    private void initialisationLemmas(Document doc, Site site, Page page) {
        LemmasOfPage lemmasOfPage = new LemmasOfPage(doc);
        HashMap<String, Integer> lemmas = lemmasOfPage.lemmas();
        Set<String> lemmasArray = lemmas.keySet();
        for (String s : lemmasArray) {
            Lemma lemma = new Lemma();
            lemma.setSite(site);
            lemma.setLemma(s);
            lemma.setFrequency(1);
            lemmaRepository.save(lemma);
            initialisationIndex(lemma, page, lemmas.get(s));
        }
    }

    private void initialisationIndex(Lemma lemma, Page page, int rank) {
        IndexObject indexObject = new IndexObject();
        indexObject.setPage(page);
        indexObject.setLemma(lemma);
        indexObject.setRunk((float) rank);
        indexObjectRepository.save(indexObject);

    }
}
