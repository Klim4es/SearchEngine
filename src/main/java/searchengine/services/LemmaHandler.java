package searchengine.services;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

public interface LemmaHandler {
    Map<String,Integer> getLemmasFromText(String text) throws IOException;

    List<String> getLemma(String word) throws IOException;

    List<Integer> findLemmaIndexInText(String content, String lemma) throws IOException;

    void getLemmasFromUrl(URL url) throws IOException;
}
