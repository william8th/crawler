package com.williamheng.monzocrawler.crawler;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.williamheng.monzocrawler.model.Resource;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.williamheng.monzocrawler.testutil.TestUtil.stubURIWithContent;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class MonzoCrawlerTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(8080));

    private static String URLString = "http://localhost:8080";

    private MonzoCrawler crawler;
    private Queue<Resource> queue;
    private Map<String, Resource> resources;

    @Before
    public void setUp() throws Exception {
        queue = new ConcurrentLinkedQueue<>();
        resources = new ConcurrentHashMap<>();
        crawler = new MonzoCrawler(
                JerseyClientBuilder.createClient(),
                URLString,
                queue,
                resources
        );
    }

    @Test
    public void visitsRootURL() throws Exception {
        // Given a root URL
        stubURIWithContent("/", "Something");

        // When crawler is initiated
        Future<String> result = crawler.initCrawlOperation();
        result.get(1, TimeUnit.SECONDS);

        // Then the root URL is visited
        verify(1, getRequestedFor(urlPathEqualTo("/")));
    }

    @Test
    public void doesNotRevisitVisitedVertex() throws Exception {
        // Given that I have two pages linked to each other (thus creating a loop)
        stubURIWithContent("/", "<a href=\"/page\">Page</a>");
        stubURIWithContent("/page", "<a href=\"/\">Index</a>");

        // When I crawl from the root URL
        Future<String> result = crawler.initCrawlOperation();
        result.get(2, TimeUnit.SECONDS);

        // Then I expect to not see the same URL crawled again
        verify(1, getRequestedFor(urlPathEqualTo("/")));
        verify(1, getRequestedFor(urlPathEqualTo("/page")));
        assertThat(resources.size(), is(2));
        assertThat(queue.size(), is(0));
    }

    @Test
    public void ignoresNonReachableURLs() throws Exception {
        stubURIWithContent("/", "<a href=\"/page\">Not found</a>");
        stubFor(
                get(
                        urlEqualTo("/page")
                ).willReturn(
                        aResponse().withStatus(404)
                )
        );

        Future<String> result = crawler.initCrawlOperation();
        result.get(1, TimeUnit.SECONDS);

        verify(1, getRequestedFor(urlPathEqualTo("/")));
        verify(1, getRequestedFor(urlPathEqualTo("/page")));
        assertThat(resources.size(), is(1));
        assertThat(queue.size(), is(0));
    }

}