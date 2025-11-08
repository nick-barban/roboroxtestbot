package com.nb.service;

import io.micronaut.chatbots.telegram.api.Update;
import io.micronaut.core.annotation.NonNull;
import jakarta.validation.constraints.NotNull;

public interface MessageService {
    void sendInputMessage(@NonNull @NotNull Update input);
}
