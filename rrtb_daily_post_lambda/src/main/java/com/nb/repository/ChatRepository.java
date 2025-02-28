package com.nb.repository;

import io.micronaut.chatbots.telegram.api.Chat;

import java.util.Optional;

public interface ChatRepository {
    Optional<Chat> getChatByName(String chatName);
}
