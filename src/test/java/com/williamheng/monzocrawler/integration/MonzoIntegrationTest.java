package com.williamheng.monzocrawler.integration;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.williamheng.monzocrawler.crawler.MonzoCrawlerOrchestrator;
import com.williamheng.monzocrawler.model.Graph;
import com.williamheng.monzocrawler.model.Resource;
import com.williamheng.monzocrawler.model.Vertex;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.williamheng.monzocrawler.testutil.TestUtil.stubURIWithFilename;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class MonzoIntegrationTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(8080));

    private MonzoCrawlerOrchestrator crawler;
    private BlockingQueue<Resource> visitQueue;

    @Before
    public void setUp() throws Exception {
        visitQueue = new LinkedBlockingQueue<>();
        crawler = new MonzoCrawlerOrchestrator(
                JerseyClientBuilder.createClient(),
                "http://localhost:8080",
                visitQueue,
                3,
                1,
                false
        );
    }

    @Test
    public void crawlURL() throws Exception {
        stubURIWithFilename("/", "index.html");
        stubURIWithFilename("/page2", "page2.html");
        stubURIWithFilename("/page3", "page3.html");
        stubURIWithFilename("/page4", "page4.html");

        Future<Graph> result = crawler.initCrawlOperation();

        assertThat(result, notNullValue());
        Graph graph = result.get();

        verify(1, getRequestedFor(urlEqualTo("/")));
        verify(1, getRequestedFor(urlEqualTo("/page2")));
        verify(1, getRequestedFor(urlEqualTo("/page3")));
        verify(1, getRequestedFor(urlEqualTo("/page4")));

        assertThat(graph.getVertices().size(), is(4));

        verticesAdjacentToVertex(graph, "/", "/page2", "/page3");
        verticesAdjacentToVertex(graph, "/page2", "/page3", "/page4");
        verticesAdjacentToVertex(graph, "/page3", "/page4");
        verticesAdjacentToVertex(graph, "/page4", "/");
    }

    private static void verticesAdjacentToVertex(
            Graph graph, String vertexID, String... adjacentVertices) {
        assertThat(graph.getVertices().containsKey(vertexID), is(true));
        Vertex vertex = graph.getVertices().get(vertexID);

        Arrays.stream(adjacentVertices)
                .forEach(link ->
                        assertThat(vertex.getAdjacentVertices().contains(link), is(true))
                );
    }

}
