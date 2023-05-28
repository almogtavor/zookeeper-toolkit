package io.github.almogtavor.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum PreConfiguredZkHosts {
    LOCALHOST("localhost:2181"),
    CUSTOM("none");

    private String host;
}
