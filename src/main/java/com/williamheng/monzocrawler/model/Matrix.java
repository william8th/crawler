package com.williamheng.monzocrawler.model;

import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Matrix {

    @Getter
    private final Map<String, Vertex> resources = new ConcurrentHashMap<>();

    public void addResource(Resource resource, List<String> links) {
        resources.put(resource.getUrl().getPath(), new Vertex(resource, links));
    }

    public boolean resourceExists(Resource resource) {
        return resources.containsKey(resource.getUrl().getPath());
    }

}
