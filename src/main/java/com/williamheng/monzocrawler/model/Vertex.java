package com.williamheng.monzocrawler.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Value
public class Vertex {

    private final Resource resource;

    // Needs concurrent access
    @Getter
    @Setter(AccessLevel.NONE)
    private final Set<String> adjacentVertices = ConcurrentHashMap.newKeySet();

    public Vertex(Resource resource, List<String> links) {
        this.resource = resource;
        links.stream().forEach(adjacentVertices::add);
    }
}
