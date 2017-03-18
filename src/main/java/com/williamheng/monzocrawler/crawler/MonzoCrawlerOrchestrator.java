package com.williamheng.monzocrawler.crawler;


import com.williamheng.monzocrawler.model.Graph;
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

/**
 * How it works:
 *
 * 1. A number of workers (crawlers) is created
 * 2. A queue is maintained for the crawlers to visit
 * 3. Workers act as both producers and consumers (taking and submitting to the queue)
 * 4. The workers will crawl and wait for a URL until there's no more URLs to crawl
 * 5. The workers will terminate itself once it waits for more than the idle time and there's no more URL to crawl
 *
 */
@Slf4j
public class MonzoCrawlerOrchestrator {

    private final Client client;
    private final Resource rootResource;
    private final BlockingQueue<Resource> visitQueue;
    private final Set<String> visitedURLs = Collections.synchronizedSet(new HashSet<>());
    private int numberOfCrawlers;
    private int idleTime;
    private boolean addExternalLinks;
    private final ExecutorService executorService;

    private final Graph graph = new Graph();

    public MonzoCrawlerOrchestrator(
            Client client,
            String rootURL,
            BlockingQueue<Resource> visitQueue,
            int numberOfCrawlers,
            int idleTime,
            boolean addExternalLinks
    ) throws MalformedURLException {

        this.client = client;
        this.visitQueue = visitQueue;
        this.numberOfCrawlers = numberOfCrawlers;
        this.idleTime = idleTime;
        this.addExternalLinks = addExternalLinks;
        this.executorService = Executors.newFixedThreadPool(numberOfCrawlers);

        if (!rootURL.endsWith("/")) {
            rootURL = String.format("%s/", rootURL);
        }
        URL url = new URL(rootURL);
        this.rootResource = new Resource(url, url.getPath());
    }

    public Future<Graph> initCrawlOperation() {

        return CompletableFuture.supplyAsync(() -> {

            ArrayList<Future> futures = new ArrayList<>();
            visitQueue.add(rootResource);
            for (int i = 0; i < this.numberOfCrawlers; i++) {
                futures.add(
                        executorService.submit(
                                MonzoCrawler.builder()
                                        .client(client)
                                        .visitQueue(visitQueue)
                                        .synchronizedVisitedURLs(visitedURLs)
                                        .graph(graph)
                                        .rootURL(rootResource.getUrl())
                                        .idleTime(idleTime)
                                        .addExternalLinks(addExternalLinks)
                                        .build()
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

            return MonzoCrawlerOrchestrator.this.graph;
        });
    }

    public void shutdown() {
        executorService.shutdown();
    }

}
