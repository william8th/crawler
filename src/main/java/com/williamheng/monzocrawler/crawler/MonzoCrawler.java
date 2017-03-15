package com.williamheng.monzocrawler.crawler;


import com.williamheng.monzocrawler.model.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@Slf4j
public class MonzoCrawler {

    private final Client client;
    private final Resource rootResource;

    private final Queue<Resource> visitQueue;
    private final Map<String, Resource> visitedResources;

    public MonzoCrawler(
            Client client,
            String rootURL,
            Queue<Resource> visitQueue,
            Map<String, Resource> visitedResources
    ) throws MalformedURLException {
        this.client = client;
        this.visitQueue = visitQueue;
        this.visitedResources = visitedResources;

        if (!rootURL.endsWith("/")) {
            rootURL = String.format("%s/", rootURL);
        }
        URL url = new URL(rootURL);
        this.rootResource = new Resource(url, url.getPath());
    }

    public Future<String> initCrawlOperation() {

        return CompletableFuture.supplyAsync(() -> {

            Elements elements = MonzoCrawler.this.crawl(rootResource);

            elements.stream()
                    .map(e -> toResource(e))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .forEach(resource -> {
                        addToQueueIfNotVisited(resource);
                    });

            continueCrawling();

            return "";
        });
    }

    private void continueCrawling() {

        while (!visitQueue.isEmpty()) {

            Resource resourceToVisit = this.visitQueue.remove();
            Elements elementsToVisit = this.crawl(resourceToVisit);

            elementsToVisit.stream()
                    .map(e -> toResource(e))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
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
    private Elements crawl(Resource resource) {

        URL url = resource.getUrl();
        log.info("Crawling {}", url);

        try {
            String HTML = client
                    .target(url.toString())
                    .request(MediaType.TEXT_HTML)
                    .get(String.class);

            resource.setVisited(true);
            visitedResources.put(resource.getUrl().getPath(), resource);

            return MonzoHTMLScraper.scrape(HTML);

        } catch (WebApplicationException e) {
            log.warn("Unable to reach URL={}", url, e);
        }

        return new Elements();
    }

    private void addToQueueIfNotVisited(Resource resource) {

        String path = resource.getUrl().getPath();

        boolean isResourceVisited = visitedResources.containsKey(path) && visitedResources.get(path).isVisited();
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
