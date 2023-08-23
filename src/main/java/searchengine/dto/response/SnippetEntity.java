package searchengine.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SnippetEntity {
    private String snippet;
    private int numberOfMatches;
    private int numberOfWords;
}
