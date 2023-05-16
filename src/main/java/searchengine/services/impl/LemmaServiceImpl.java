package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.services.LemmaHandler;
import searchengine.services.LemmaService;

import java.io.IOException;
import java.net.URL;

@Service
@RequiredArgsConstructor
public class LemmaServiceImpl implements LemmaService {

    @Autowired
    private final LemmaHandler lemmaHandler;

    @Override
    public ResponseEntity<String> findLemmas(SitesList sitesList, URL url) throws IOException {

        try {
            sitesList.getSites().stream().filter(site -> url.getHost().equals(site.getUrl().getHost())).findFirst().orElseThrow();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("result: false " +
                    "error: Данная страница находится за пределами сайтов " +
                    "указанных в конфигурационном файле");
        }
        lemmaHandler.getLemmasFromUrl(url);
        return ResponseEntity.status(HttpStatus.OK).body("result: true");
    }
}
