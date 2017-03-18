package com.williamheng.monzocrawler.model.json;

import lombok.Value;

import java.util.Collection;

@Value
public class JSONGraph {
    private final Collection<JSONNode> nodes;
    private final Collection<JSONLink> links;
}
