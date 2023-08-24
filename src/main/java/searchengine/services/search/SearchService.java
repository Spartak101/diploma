package searchengine.services.search;

import searchengine.dto.response.ResponseSearch;
import searchengine.dto.response.ResponseSearchData;
import searchengine.model.Page;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

public interface SearchService {
    ResponseSearch searchResponce(String query, int offset, int limit, String site);
}
