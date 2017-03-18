package com.williamheng.monzocrawler.crawler;


import com.williamheng.monzocrawler.model.Matrix;
import com.williamheng.monzocrawler.model.Resource;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.client.Client;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

import static com.williamheng.monzocrawler.MonzoCrawlerApplication.DEFAULT_NUMBER_OF_CRAWLERS;

@Slf4j
public class MonzoCrawlerOrchestrator {

    private final Client client;
    private final Resource rootResource;
    private final BlockingQueue<Resource> visitQueue;
    private final Set<String> visitedURLs = Collections.synchronizedSet(new HashSet<>());
    private int numberOfCrawlers;
    private final ExecutorService executorService;

    private final Matrix matrix = new Matrix();

    public MonzoCrawlerOrchestrator(Client client, String rootURL, BlockingQueue<Resource> visitQueue)
            throws MalformedURLException {
        this(client, rootURL, DEFAULT_NUMBER_OF_CRAWLERS, visitQueue);
    }

    public MonzoCrawlerOrchestrator(
            Client client,
            String rootURL,
            int numberOfCrawlers,
            BlockingQueue<Resource> visitQueue
    ) throws MalformedURLException {

        this.client = client;
        this.visitQueue = visitQueue;
        this.numberOfCrawlers = numberOfCrawlers;
        this.executorService  = Executors.newFixedThreadPool(numberOfCrawlers);

        if (!rootURL.endsWith("/")) {
            rootURL = String.format("%s/", rootURL);
        }
        URL url = new URL(rootURL);
        this.rootResource = new Resource(url, url.getPath());
    }

    public Future<Matrix> initCrawlOperation() {

        return CompletableFuture.supplyAsync(() -> {

            ArrayList<Future> futures = new ArrayList<>();
            visitQueue.add(rootResource);
            for (int i = 0; i < this.numberOfCrawlers; i++) {
                futures.add(
                        executorService.submit(
                                new MonzoCrawler(client, visitQueue, visitedURLs, matrix, rootResource.getUrl())
                        )
                );
            }

            futures.stream().forEach(f -> {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Executor tasks interrupted", e);
                }
            });

            return MonzoCrawlerOrchestrator.this.matrix;
        });
    }

    public void shutdown() {
        executorService.shutdown();
    }

}
