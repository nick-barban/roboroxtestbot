package com.nb;

import io.micronaut.core.annotation.NonNull;

public enum RrtbHashCommand {
    ADD_SCHOOL("#addschool");
    
    private final String command;

    RrtbHashCommand(String command) {
        this.command = command;
    }

    public @NonNull String command() {
        return this.command;
    }

}
