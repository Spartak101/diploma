package searchengine.dto.response;

import lombok.Data;

@Data
public  final class ResponseSearchData {
    private String site;
    private String siteName;
    private String title;
    private String uri;
    private String snippet;
    private float relevance;
}
