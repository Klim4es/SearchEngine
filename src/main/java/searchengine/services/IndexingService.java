package searchengine.services;

import org.springframework.http.ResponseEntity;

import java.util.concurrent.atomic.AtomicBoolean;

public interface IndexingService {
    ResponseEntity startIndexing();
    ResponseEntity stopIndexing();
}
