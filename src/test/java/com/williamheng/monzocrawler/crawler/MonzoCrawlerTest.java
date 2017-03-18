package com.williamheng.monzocrawler.crawler;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.williamheng.monzocrawler.model.Matrix;
import com.williamheng.monzocrawler.model.Resource;
import com.williamheng.monzocrawler.model.Vertex;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.williamheng.monzocrawler.testutil.TestUtil.stubURIWithContent;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class MonzoCrawlerTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(8080));

    private static String HOST_URL = "http://localhost:8080";

    private MonzoCrawler monzoCrawler;
    private BlockingQueue<Resource> queue;
    private Matrix matrix;

    @Before
    public void setUp() throws Exception {
        queue = new LinkedBlockingQueue<>();
        matrix = new Matrix();
        monzoCrawler = MonzoCrawler.builder()
                .client(JerseyClientBuilder.createClient())
                .visitQueue(queue)
                .synchronizedVisitedURLs(new HashSet<>())
                .matrix(matrix)
                .rootURL(new URL(HOST_URL))
                .addExternalLinks(true)
                .build();
    }

    @Test
    public void visitsRootURL() throws Exception {
        // Given a root URL to crawl
        stubURIWithContent("/", "Something");
        queue.add(buildResourceForRelativePath("/", ""));

        // When crawlerOrchestrator is initiated
        monzoCrawler.run();

        // Then the root URL is visited
        verify(1, getRequestedFor(urlPathEqualTo("/")));
        assertThat(matrix.getResources().size(), is(1));
        assertThat(matrix.getResources().get("/"), notNullValue());
    }

    @Test
    public void doesNotRevisitVisitedVertex() throws Exception {
        // Given that I have two pages linked to each other (thus creating a loop)
        stubURIWithContent("/", "<a href=\"/page\">Page</a>");
        stubURIWithContent("/page", "<a href=\"/\">Index</a>");
        queue.add(buildResourceForRelativePath("/", ""));

        // When I crawl from the root URL
        monzoCrawler.run();

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
        // Given a page with a non-reachable URL
        stubURIWithContent("/", "<a href=\"/page\">Not found</a>");
        stubFor(
                get(
                        urlEqualTo("/page")
                ).willReturn(
                        aResponse().withStatus(404)
                )
        );
        queue.add(buildResourceForRelativePath("/", ""));

        // When crawler crawls
        monzoCrawler.run();

        // Then I expect the crawler to ignore the error and continue on with other operations
        verify(1, getRequestedFor(urlPathEqualTo("/")));
        verify(1, getRequestedFor(urlPathEqualTo("/page")));
        assertThat(queue.size(), is(0));
        assertThat(matrix.getResources().size(), is(1));
    }

    @Test
    public void onlyVisitsSingleDomain() throws Exception {

        // Given a crawler with mock JAX-RS client
        final String baseURL = String.format("%s/", HOST_URL);
        final String baseURLContent = "<a href=\"http://google.com\">Google</a>";
        Client mockClient = Mockito.mock(Client.class);
        WebTarget mockWebTarget = Mockito.mock(WebTarget.class);
        Invocation.Builder mockBuilder = Mockito.mock(Invocation.Builder.class);

        when(mockClient.target(baseURL)).thenReturn(mockWebTarget);
        when(mockWebTarget.request(MediaType.TEXT_HTML)).thenReturn(mockBuilder);
        when(mockBuilder.get(String.class)).thenReturn(baseURLContent);
        monzoCrawler = MonzoCrawler.builder()
                .client(mockClient)
                .visitQueue(queue)
                .synchronizedVisitedURLs(new HashSet<>())
                .matrix(matrix)
                .rootURL(new URL(HOST_URL))
                .build();

        // And that the crawler is to crawl a page with an external link
        queue.add(buildResourceForRelativePath("/", ""));

        // When crawler crawls
        monzoCrawler.run();

        // Then I expect the root URL and Page 1 to be crawled
        Mockito.verify(mockClient, times(1)).target(baseURL);
        Mockito.verify(mockClient, times(0)).target("http://google.com");

        // And that
        assertThat(matrix.getResources().size(), is(1));
        assertThat(matrix.getResources().get("/").getAdjacentSet().size(), is(1));
        assertThat(matrix.getResources().get("/").getAdjacentSet().contains("http://google.com"), is(true));
    }

    @Test
    public void doesNotAddExternalLinks() throws Exception {
        // Given a crawler that does not add external links
        monzoCrawler = MonzoCrawler.builder()
                .client(JerseyClientBuilder.createClient())
                .visitQueue(queue)
                .synchronizedVisitedURLs(new HashSet<>())
                .matrix(matrix)
                .rootURL(new URL(HOST_URL))
                .addExternalLinks(false)
                .build();
        stubURIWithContent("/", "<a href=\"/page1\">Page1</a><a href=\"http://google.com\">Google</a>");
        stubURIWithContent("/page1", "");
        queue.add(buildResourceForRelativePath("/", ""));

        // When crawler crawls
        monzoCrawler.run();

        // Then I expect the external link to not be added
        assertThat(matrix.getResources().size(), is(2));
        assertThat(matrix.getResources().get("/").getAdjacentSet().size(), is(1));
        assertThat(matrix.getResources().get("/").getAdjacentSet().contains("http://google.com"), is(false));
    }

    private static Resource buildResourceForRelativePath(String path, String title) throws MalformedURLException {
        String url = String.format("%s%s", HOST_URL, path);
        return new Resource(new URL(url), title);
    }
}