package com.williamheng.monzocrawler.crawler;

import org.jsoup.select.Elements;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class MonzoHTMLScraperTest {
    @Test
    public void scrapesCorrectLinks() throws Exception {
        Elements elements = MonzoHTMLScraper.scrape("<a href=\"1\">1</a>  <a href=\"2\">2</a>  <a>3</a>");

        assertThat(elements, notNullValue());
        assertThat(elements.size(), is(2));
    }

    @Test
    public void returnsNothingIfNoLinks() throws Exception {
        Elements elements = MonzoHTMLScraper.scrape("<p>Some text</p>");

        assertThat(elements, notNullValue());
        assertThat(elements.size(), is(0));
    }

    @Test
    public void returnsNothingIfLinkIsNotValid() throws Exception {
        Elements elements = MonzoHTMLScraper.scrape("<a>What is this</a>");

        assertThat(elements, notNullValue());
        assertThat(elements.size(), is(0));
    }
}