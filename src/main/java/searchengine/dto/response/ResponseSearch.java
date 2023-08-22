package searchengine.dto.response;

import lombok.Data;

import java.util.ArrayList;




@Data
public class ResponseSearch {
    private boolean result = false;
    private String error;
    private int count;
    private ArrayList<ResponseSearchData> data;
}
