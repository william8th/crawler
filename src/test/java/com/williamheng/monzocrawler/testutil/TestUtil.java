package com.williamheng.monzocrawler.testutil;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;

public class TestUtil {

    public static void stubURIWithFilename(String URI, String filename) {
        stubFor(
                get(
                        urlEqualTo(URI)
                ).willReturn(
                        aResponse().withBodyFile(filename)
                )
        );
    }

    public static void stubURIWithContent(String URI, String content) {
        stubFor(
                get(
                        urlEqualTo(URI)
                ).willReturn(
                        aResponse().withBody(content)
                )
        );
    }
}
