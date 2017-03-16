package com.williamheng.monzocrawler.integration;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.williamheng.monzocrawler.crawler.MonzoCrawler;
import com.williamheng.monzocrawler.model.Matrix;
import com.williamheng.monzocrawler.model.Resource;
import com.williamheng.monzocrawler.model.Vertex;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.williamheng.monzocrawler.testutil.TestUtil.stubURIWithFilename;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class MonzoIntegrationTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(8080));

    private MonzoCrawler crawler;
    private ConcurrentLinkedQueue<Resource> visitQueue;

    @Before
    public void setUp() throws Exception {
        visitQueue = new ConcurrentLinkedQueue<>();
        crawler = new MonzoCrawler(
                JerseyClientBuilder.createClient(),
                "http://localhost:8080",
                visitQueue
        );
    }

    @Test
    public void crawlURL() throws Exception {
        stubURIWithFilename("/", "index.html");
        stubURIWithFilename("/page2", "page2.html");
        stubURIWithFilename("/page3", "page3.html");
        stubURIWithFilename("/page4", "page4.html");

        Future<Matrix> result = crawler.initCrawlOperation();

        assertThat(result, notNullValue());
        Matrix matrix = result.get(1, TimeUnit.SECONDS);

        verify(1, getRequestedFor(urlEqualTo("/")));
        verify(1, getRequestedFor(urlEqualTo("/page2")));
        verify(1, getRequestedFor(urlEqualTo("/page3")));
        verify(1, getRequestedFor(urlEqualTo("/page4")));

        assertThat(matrix.getResources().size(), is(4));

        verticesAdjacentToVertex(matrix, "/", "/page2", "/page3");
        verticesAdjacentToVertex(matrix, "/page2", "/page3", "/page4");
        verticesAdjacentToVertex(matrix, "/page3", "/page4");
        verticesAdjacentToVertex(matrix, "/page4", "/");
    }

    private static void verticesAdjacentToVertex(
            Matrix matrix, String vertexID, String... adjacentVertices) {
        assertThat(matrix.getResources().containsKey(vertexID), is(true));
        Vertex vertex = matrix.getResources().get(vertexID);

        Arrays.stream(adjacentVertices)
                .forEach(link ->
                        assertThat(vertex.getAdjacentSet().contains(link), is(true))
                );
    }

}
