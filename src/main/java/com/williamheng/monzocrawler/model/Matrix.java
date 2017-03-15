package com.williamheng.monzocrawler.model;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Matrix {

    private final Set<Set> knownResources = ConcurrentHashMap.newKeySet();

}
