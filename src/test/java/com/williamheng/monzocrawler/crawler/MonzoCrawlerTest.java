package com.williamheng.monzocrawler.crawler;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.williamheng.monzocrawler.model.Matrix;
import com.williamheng.monzocrawler.model.Resource;
import com.williamheng.monzocrawler.model.Vertex;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Queue;
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

    @Before
    public void setUp() throws Exception {
        queue = new ConcurrentLinkedQueue<>();
        crawler = new MonzoCrawler(
                JerseyClientBuilder.createClient(),
                URLString,
                queue
        );
    }

    @Test
    public void visitsRootURL() throws Exception {
        // Given a root URL
        stubURIWithContent("/", "Something");

        // When crawler is initiated
        Future<Matrix> result = crawler.initCrawlOperation();
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
        Future<Matrix> result = crawler.initCrawlOperation();
        Matrix matrix = result.get(1, TimeUnit.SECONDS);

        // Then I expect to not see the same URL crawled again
        verify(1, getRequestedFor(urlPathEqualTo("/")));
        verify(1, getRequestedFor(urlPathEqualTo("/page")));
        assertThat(queue.size(), is(0));
        assertThat(matrix.getResources().size(), is(2));

        Vertex rootVertex = matrix.getResources().get("/");
        assertThat(rootVertex.getAdjacentSet().size(), is(1));
        assertThat(rootVertex.getAdjacentSet().contains("/page"), is(true));

        Vertex pageVertex = matrix.getResources().get("/page");
        assertThat(pageVertex.getAdjacentSet().size(), is(1));
        assertThat(pageVertex.getAdjacentSet().contains("/"), is(true));
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

        Future<Matrix> result = crawler.initCrawlOperation();
        Matrix matrix = result.get(1, TimeUnit.SECONDS);

        verify(1, getRequestedFor(urlPathEqualTo("/")));
        verify(1, getRequestedFor(urlPathEqualTo("/page")));
        assertThat(queue.size(), is(0));
        assertThat(matrix.getResources().size(), is(1));
    }

    @Test
    public void onlyVisitsSingleDomain() throws Exception {

        stubURIWithContent("/", "<a href=\"http://localhost:8080/page1\">Page1</a><a href=\"http://google.com\">Google</a>");
        stubURIWithContent("/page1", "Nothing");

        Future<Matrix> result = crawler.initCrawlOperation();
        Matrix matrix = result.get(1, TimeUnit.SECONDS);

        verify(1, getRequestedFor(urlPathEqualTo("/")));

        assertThat(matrix.getResources().size(), is(2));
        assertThat(matrix.getResources().get("/").getAdjacentSet().size(), is(2));
        assertThat(matrix.getResources().get("/page1").getAdjacentSet().size(), is(0));
    }

}