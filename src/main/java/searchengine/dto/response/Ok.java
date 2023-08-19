package searchengine.dto.response;

import lombok.Data;
import java.util.HashMap;


@Data
public class Ok {
    private boolean result = false;
    private String error;
    private int count;
    private HashMap<String, String> data;
}
