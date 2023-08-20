package searchengine.dto.response;

import lombok.Data;

@Data
public  final class ResponseObject {
    private String site;
    private String siteName;
    private String uri;
    private String snippet;
    private float relevance;
}
