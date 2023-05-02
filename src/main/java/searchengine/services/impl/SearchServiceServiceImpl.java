package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.Morphology;
import org.springframework.stereotype.Service;
import searchengine.dto.search.StatisticsSearch;
import searchengine.model.IndexSearch;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SitePage;
import searchengine.repository.IndexSearchRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.LemmaService;
import searchengine.services.SearchService;
import searchengine.util.CleanHtmlCode;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceServiceImpl implements SearchService {
    private final LemmaService morphology;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final IndexSearchRepository indexSearchRepository;
    private final SiteRepository siteRepository;

    @Override
    public List<StatisticsSearch> allSiteSearch(String searchText, int offset, int limit) throws IOException {
        log.info("Getting results of the search \"" + searchText + "\"");
        List<SitePage> siteList = siteRepository.findAll();
        List<StatisticsSearch> result = new ArrayList<>();
        List<Lemma> foundLemmaList = new ArrayList<>();
        List<String> textLemmaList = getLemmaFromSearchText(searchText);
        for (SitePage site : siteList) {
            foundLemmaList.addAll(getLemmaListFromSite(textLemmaList, site));
        }
        List<StatisticsSearch> searchData = null;
        for (Lemma l : foundLemmaList) {
            if (l.getLemma().equals(searchText)) {
                searchData = new ArrayList<>(getSearchDtoList(foundLemmaList, textLemmaList, offset, limit));
                searchData.sort((o1, o2) -> Float.compare(o2.getRelevance(), o1.getRelevance()));
                if (searchData.size() > limit) {
                    for (int i = offset; i < limit; i++) {
                        result.add(searchData.get(i));
                    }
                    return result;
                }
            } else {
                try {
                    throw new Exception();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        log.info("Search done. Got results.");
        log.info(searchData.toString());
        return searchData;
    }

    @Override
    public List<StatisticsSearch> siteSearch(String searchText, String url, int offset, int limit) throws IOException {
        log.info("Searching for \"" + searchText + "\" in - " + url);
        SitePage site = siteRepository.findByUrl(url);
        List<String> textLemmaList = getLemmaFromSearchText(searchText);
        List<Lemma> foundLemmaList = getLemmaListFromSite(textLemmaList, site);
        foundLemmaList.forEach(x->System.out.println(x.getLemma()));
        return getSearchDtoList(foundLemmaList, textLemmaList, offset, limit);
    }

    private List<String> getLemmaFromSearchText(String searchText) throws IOException {
        String[] words = searchText.toLowerCase(Locale.ROOT).split(" ");
        List<String> lemmaList = new ArrayList<>();
        for (String lemma : words) {
            List<String> list = morphology.getLemmasFromText(lemma).keySet().stream().toList();
            lemmaList.addAll(list);
        }
        return lemmaList;
    }

    private List<Lemma> getLemmaListFromSite(List<String> lemmas, SitePage site) {
        lemmaRepository.flush();
        List<Lemma> lemmaList = lemmaRepository.findLemmaListBySite(lemmas, site);
        List<Lemma> result = new ArrayList<>(lemmaList);
        result.sort(Comparator.comparingInt(Lemma::getFrequency));
        return result;
    }

    private List<StatisticsSearch> getSearchData(Hashtable<Page, Float> pageList, List<String> textLemmaList) throws IOException {
        List<StatisticsSearch> result = new ArrayList<>();

        for (Page page : pageList.keySet()) {
            String uri = page.getPath();
            String content = page.getContent();
            SitePage pageSite = page.getSitePage();
            String site = pageSite.getUrl();
            String siteName = pageSite.getName();
            Float absRelevance = pageList.get(page);

            StringBuilder clearContent = new StringBuilder();
            String title = CleanHtmlCode.clear(content, "title");
            String body = CleanHtmlCode.clear(content, "body");
            clearContent.append(title).append(" ").append(body);
            String snippet = getSnippet(clearContent.toString(), textLemmaList);

            result.add(new StatisticsSearch(site, siteName, uri, title, snippet, absRelevance));
        }
        return result;
    }

    private String getSnippet(String content, List<String> lemmaList) throws IOException {
        List<Integer> lemmaIndex = new ArrayList<>();
        StringBuilder result = new StringBuilder();
        for (String lemma : lemmaList) {
            lemmaIndex.addAll(morphology.findLemmaIndexInText(content, lemma));
        }
        Collections.sort(lemmaIndex);
        List<String> wordsList = getWordsFromContent(content, lemmaIndex);
        for (int i = 0; i < wordsList.size(); i++) {
            result.append(wordsList.get(i)).append("... ");
            if (i > 3) {
                break;
            }
        }
        return result.toString();
    }

    private List<String> getWordsFromContent(String content, List<Integer> lemmaIndex) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < lemmaIndex.size(); i++) {
            int start = lemmaIndex.get(i);
            int end = content.indexOf(" ", start);
            int nextPoint = i + 1;
            while (nextPoint < lemmaIndex.size() && lemmaIndex.get(nextPoint) - end > 0 && lemmaIndex.get(nextPoint) - end < 5) {
                end = content.indexOf(" ", lemmaIndex.get(nextPoint));
                nextPoint += 1;
            }
            i = nextPoint - 1;
            String text = getWordsFromIndex(start, end, content);
            result.add(text);
        }
        result.sort(Comparator.comparingInt(String::length).reversed());
        return result;
    }

    private String getWordsFromIndex(int start, int end, String content) {
        String word = content.substring(start, end);
        int prevPoint;
        int lastPoint;
        if (content.lastIndexOf(" ", start) != -1) {
            prevPoint = content.lastIndexOf(" ", start);
        } else prevPoint = start;
        if (content.indexOf(" ", end + 30) != -1) {
            lastPoint = content.indexOf(" ", end + 30);
        } else lastPoint = content.indexOf(" ", end);
        String text = content.substring(prevPoint, lastPoint);
        try {
            text = text.replaceAll(word, "<b>" + word + "</b>");
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return text;
    }

    private List<StatisticsSearch> getSearchDtoList(List<Lemma> lemmaList, List<String> textLemmaList, int offset, int limit) throws IOException {
        List<StatisticsSearch> result = new ArrayList<>();
        System.out.println(0);
        pageRepository.flush();
        System.out.println(1);
        if (lemmaList.size() >= textLemmaList.size()) {
            List<Page> foundPageList = pageRepository.findByLemmaList(lemmaList);
            indexSearchRepository.flush();
            List<IndexSearch> foundIndexList = indexSearchRepository.findByPagesAndLemmas(lemmaList, foundPageList);
            Hashtable<Page, Float> sortedPageByAbsRelevance = getPageAbsRelevance(foundPageList, foundIndexList);
            List<StatisticsSearch> dataList = getSearchData(sortedPageByAbsRelevance, textLemmaList);
            if (offset > dataList.size()) {
                return new ArrayList<>();
            }

            if (dataList.size() > limit) {
                for (int i = offset; i < limit; i++) {
                    result.add(dataList.get(i));
                }
                return result;
            } else return dataList;
        } else return result;
    }

    private Hashtable<Page, Float> getPageAbsRelevance(List<Page> pageList, List<IndexSearch> indexList) {
        HashMap<Page, Float> pageWithRelevance = new HashMap<>();
        for (Page page : pageList) {
            float relevant = 0;
            for (IndexSearch index : indexList) {
                if (index.getPage() == page) {
                    relevant += index.getLemmaCount();
                }
            }
            pageWithRelevance.put(page, relevant);
        }
        HashMap<Page, Float> pageWithAbsRelevance = new HashMap<>();
        for (Page page : pageWithRelevance.keySet()) {
            float absRelevant = pageWithRelevance.get(page) / Collections.max(pageWithRelevance.values());
            pageWithAbsRelevance.put(page, absRelevant);
        }
        return pageWithAbsRelevance.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey,
                Map.Entry::getValue, (e1, e2) -> e1, Hashtable::new));
    }

}