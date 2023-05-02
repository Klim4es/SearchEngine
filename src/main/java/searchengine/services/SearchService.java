package searchengine.services;


import searchengine.dto.search.StatisticsSearch;

import java.io.IOException;
import java.util.List;

public interface SearchService {
    List<StatisticsSearch> allSiteSearch(String text, int offset, int limit) throws IOException;
    List<StatisticsSearch> siteSearch(String searchText, String url, int offset, int limit) throws IOException;
}