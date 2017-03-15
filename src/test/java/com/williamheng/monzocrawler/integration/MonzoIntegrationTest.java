package com.williamheng.monzocrawler.integration;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.williamheng.monzocrawler.crawler.MonzoCrawler;
import com.williamheng.monzocrawler.model.Resource;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.net.URL;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.williamheng.monzocrawler.testutil.TestUtil.stubURIWithFilename;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class MonzoIntegrationTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(8080));

    private MonzoCrawler crawler;
    private ConcurrentLinkedQueue<Resource> visitQueue;
    private ConcurrentHashMap<String, Resource> visitedResources;

    @Before
    public void setUp() throws Exception {
        visitQueue = new ConcurrentLinkedQueue<>();
        visitedResources = new ConcurrentHashMap<>();
        crawler = new MonzoCrawler(
                JerseyClientBuilder.createClient(),
                "http://localhost:8080",
                visitQueue,
                visitedResources
        );
    }

    @Test
    public void crawlURL() throws Exception {
        stubURIWithFilename("/", "index.html");
        stubURIWithFilename("/page2", "page2.html");
        stubURIWithFilename("/page3", "page3.html");
        stubURIWithFilename("/page4", "page4.html");

        Future<String> futureCrawl = crawler.initCrawlOperation();

        assertThat(futureCrawl, notNullValue());
//        String result = futureCrawl.get(1, TimeUnit.SECONDS);
        futureCrawl.get();

        List<ServeEvent> allServeEvents = getAllServeEvents();

        verify(1, getRequestedFor(urlEqualTo("/")));
        verify(1, getRequestedFor(urlEqualTo("/page2")));
        verify(1, getRequestedFor(urlEqualTo("/page3")));
        verify(1, getRequestedFor(urlEqualTo("/page4")));
    }

}
