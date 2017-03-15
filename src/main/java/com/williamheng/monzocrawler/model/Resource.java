package com.williamheng.monzocrawler.model;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.net.URL;

@RequiredArgsConstructor
@Getter
public class Resource {

    @NonNull
    private final URL url;

    @NonNull
    private final String title;

    @Setter
    private boolean visited = false;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Resource resource = (Resource) o;

        return url.equals(resource.url);
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }
}
