package com.williamheng.monzocrawler.model;

import lombok.AllArgsConstructor;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@AllArgsConstructor
public class Vertex {

    private final Resource resource;

    // Needs concurrent access
    private final Set<String> adjacentSet = ConcurrentHashMap.newKeySet();
}
