package com.williamheng.monzocrawler.crawler;

import com.williamheng.monzocrawler.model.Graph;
import com.williamheng.monzocrawler.model.Resource;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Builder
public class MonzoCrawler implements Runnable {

    @NonNull
    private final Client client;

    @NonNull
    private final BlockingQueue<Resource> visitQueue;

    @NonNull
    private final Set<String> synchronizedVisitedURLs;

    @NonNull
    private final Graph graph;

    @NonNull
    private final URL rootURL;

    private final int idleTime;
    private boolean addExternalLinks;

    /**
     * Operations:
     * 1. Get content from URL
     * 2. Add same-domain URL to known resources
     */
    @Override
    public void run() {

        while (true) {
            try {
                Resource resource = visitQueue.poll(idleTime, TimeUnit.SECONDS);

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
        Predicate<Resource> isInternalDomain = r -> r.getUrl().getHost().equalsIgnoreCase(rootURL.getHost());
        Predicate<Resource> isExternalDomain = r -> !r.getUrl().getHost().equalsIgnoreCase(rootURL.getHost());

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

            // Internal links are saved by path name e.g. /some/path
            List<String> internalAdjacentLinks = validStructuredLinks.stream()
                    .filter(isInternalDomain)
                    .map(r -> r.getUrl().getPath())
                    .collect(Collectors.toList());

            // External links are saved in the form of absolute URL e.g. http://google.com
            List<String> externalAdjacentLinks = validStructuredLinks.stream()
                    .filter(isExternalDomain)
                    .map(r -> r.getUrl().toString())
                    .collect(Collectors.toList());

            List<String> adjacentLinks = new ArrayList<>();
            adjacentLinks.addAll(internalAdjacentLinks);
            if (addExternalLinks) adjacentLinks.addAll(externalAdjacentLinks);
            graph.addVertex(resource, adjacentLinks);

            validStructuredLinks.stream()
                    .filter(isInternalDomain)
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
        boolean isResourceVisited = synchronizedVisitedURLs.contains(resource.getUrl().toString());
        boolean isResourceInQueue = visitQueue.contains(resource);
        if (!isResourceVisited && !isResourceInQueue) {
            visitQueue.add(resource);
        }
    }

}
