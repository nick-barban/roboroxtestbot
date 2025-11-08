package com.nb.domain;

import io.micronaut.core.annotation.NonNull;

public enum RrtbCommand {
    POST("/post"),
    ABOUT("/about"),
    START("/start");

    private final String command;

    RrtbCommand(String command) {
        this.command = command;
    }

    public @NonNull String command() {
        return this.command;
    }
}
