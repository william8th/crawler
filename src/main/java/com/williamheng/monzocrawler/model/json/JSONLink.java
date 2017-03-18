package com.williamheng.monzocrawler.model.json;

import lombok.Value;

@Value
public class JSONLink {
    private final String source;
    private final String target;
    private final int value;
}
