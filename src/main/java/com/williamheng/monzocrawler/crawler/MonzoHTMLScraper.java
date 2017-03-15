package com.williamheng.monzocrawler.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class MonzoHTMLScraper {

    public static Elements scrape(String HTML) {
        Document document = Jsoup.parse(HTML);
        Elements elements = document.select("a[href]");

        return elements;
    }
}
