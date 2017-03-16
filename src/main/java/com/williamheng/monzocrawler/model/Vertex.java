package com.williamheng.monzocrawler.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Vertex {

    private final Resource resource;

    // Needs concurrent access
    @Getter
    private final Set<String> adjacentSet = ConcurrentHashMap.newKeySet();

    public Vertex(Resource resource, List<String> links) {
        this.resource = resource;
        links.stream().forEach(adjacentSet::add);
    }
}
