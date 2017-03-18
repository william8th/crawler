package com.williamheng.monzocrawler.model;

import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An intermediate representation to
 */
public class Graph {

    @Getter
    private final Map<String, Vertex> vertices = new ConcurrentHashMap<>();

    public void addVertex(Resource resource, List<String> links) {
        vertices.put(resource.getUrl().getPath(), new Vertex(resource, links));
    }
}
