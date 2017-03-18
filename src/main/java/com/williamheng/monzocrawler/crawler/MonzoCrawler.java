package com.williamheng.monzocrawler.crawler;

import com.williamheng.monzocrawler.model.Matrix;
import com.williamheng.monzocrawler.model.Resource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@AllArgsConstructor
@Slf4j
public class MonzoCrawler implements Runnable {

    private final Client client;
    private final BlockingQueue<Resource> visitQueue;
    private final Set<String> synchronizedVisitedURLs;
    private final Matrix matrix;
    private final URL rootURL;

    /**
     * Operations:
     * 1. Get content from URL
     * 2. Add same-domain URL to known resources
     */
    @Override
    public void run() {

        while (true) {
            try {
                Resource resource = visitQueue.poll(5, TimeUnit.SECONDS);

                // Poll tells us that there's no longer any resource in the queue to crawl
                if (resource == null) break;

                String resourceURL = resource.getUrl().toString();
                if (!synchronizedVisitedURLs.contains(resourceURL)) {
                    synchronizedVisitedURLs.add(resourceURL);
                    this.crawl(resource);
                }
            } catch (InterruptedException e) {
                if (visitQueue.isEmpty()) break;
            }
        }
    }

    private void crawl(Resource resource) {
        URL url = resource.getUrl();
        log.info("Crawling {}", url);
        Predicate<Resource> isSameDomain = r -> r.getUrl().getHost().equalsIgnoreCase(rootURL.getHost());

        try {
            String HTML = client
                    .target(url.toString())
                    .request(MediaType.TEXT_HTML)
                    .get(String.class);

            Elements links = MonzoHTMLScraper.scrape(HTML);
            List<Resource> validStructuredLinks = links.stream()
                    .map(e -> toResource(e))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            List<String> adjacentLinks = validStructuredLinks.stream()
                    .map(r -> r.getUrl().getPath())
                    .collect(Collectors.toList());
            matrix.addResource(resource, adjacentLinks);

            validStructuredLinks.stream()
                    .filter(isSameDomain)
                    .forEach(this::addToQueueIfNotVisited);

        } catch (WebApplicationException e) {
            log.info("Unable to reach URL={}", url);
            log.debug("Unable to reach URL={}", url, e);
        }
    }

    private Optional<Resource> toResource(Element element) {
        String href = element.attr("href");

        try {

            URL url = new URL(rootURL, href);

            return Optional.of(new Resource(url, url.getPath()));

        } catch (MalformedURLException e) {
            log.warn("Unable to form URL with rootURL={}, path={}", rootURL, href, e);
        }

        return Optional.empty();
    }

    private void addToQueueIfNotVisited(Resource resource) {
        boolean isResourceVisited = matrix.resourceExists(resource);
        boolean isResourceInQueue = visitQueue.contains(resource);
        if (!isResourceVisited && !isResourceInQueue) {
            visitQueue.add(resource);
        }
    }

}
