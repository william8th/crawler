package com.williamheng.monzocrawler.crawler;


import com.williamheng.monzocrawler.model.Matrix;
import com.williamheng.monzocrawler.model.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Slf4j
public class MonzoCrawler {

    private final Client client;
    private final Resource rootResource;

    private final Queue<Resource> visitQueue;

    private final Matrix matrix = new Matrix();

    public MonzoCrawler(
            Client client,
            String rootURL,
            Queue<Resource> visitQueue
    ) throws MalformedURLException {
        this.client = client;
        this.visitQueue = visitQueue;

        if (!rootURL.endsWith("/")) {
            rootURL = String.format("%s/", rootURL);
        }
        URL url = new URL(rootURL);
        this.rootResource = new Resource(url, url.getPath());
    }

    public Future<Matrix> initCrawlOperation() {

        return CompletableFuture.supplyAsync(() -> {

            List<Resource> resources = MonzoCrawler.this.crawl(rootResource);

            resources.stream()
                    .forEach(resource -> addToQueueIfNotVisited(resource));

            continueCrawling();

            return MonzoCrawler.this.matrix;
        });
    }

    private void continueCrawling() {

        while (!visitQueue.isEmpty()) {

            Resource resourceToVisit = this.visitQueue.remove();
            List<Resource> resources = this.crawl(resourceToVisit);

            resources.stream()
                    .forEach(resource -> addToQueueIfNotVisited(resource));
        }
    }

    /**
     * Operations:
     * 1. Get content from URL
     * 2. Add URL to known resources
     *
     * @param resource The resource to visit
     * @return Links scraped from page
     */
    private List<Resource> crawl(Resource resource) {

        URL url = resource.getUrl();
        log.info("Crawling {}", url);

        try {
            String HTML = client
                    .target(url.toString())
                    .request(MediaType.TEXT_HTML)
                    .get(String.class);

            Elements links = MonzoHTMLScraper.scrape(HTML);
            List<Resource> validLinks = links.stream()
                    .map(e -> toResource(e))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            List<String> adjacentLinks = validLinks.stream()
                    .map(r -> r.getUrl().getPath())
                    .collect(Collectors.toList());
            matrix.addResource(resource, adjacentLinks);

            return validLinks;

        } catch (WebApplicationException e) {
            log.warn("Unable to reach URL={}", url, e);
        }

        return Collections.emptyList();
    }

    private void addToQueueIfNotVisited(Resource resource) {

        boolean isResourceVisited = matrix.resourceExists(resource);
        boolean isResourceInQueue = visitQueue.contains(resource);
        if (!isResourceVisited && !isResourceInQueue) {
            visitQueue.add(resource);
        }
    }

    private Optional<Resource> toResource(Element element) {

        String href = element.attr("href");
        URL rootURL = rootResource.getUrl();

        try {

            URL url = new URL(rootURL, href);

            return Optional.of(new Resource(url, url.getPath()));

        } catch (MalformedURLException e) {
            log.warn("Unable to form URL with rootURL={}, path={}", rootURL, href, e);
        }

        return Optional.empty();
    }

}
