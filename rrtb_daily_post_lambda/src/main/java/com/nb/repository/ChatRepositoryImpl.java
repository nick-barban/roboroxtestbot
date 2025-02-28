package com.nb.repository;

import io.micronaut.chatbots.telegram.api.Chat;
import jakarta.inject.Singleton;

import java.util.Optional;

@Singleton
public class ChatRepositoryImpl implements ChatRepository {
    @Override
    public Optional<Chat> getChatByName(String chatName) {
        final Chat chat = new Chat();
        chat.setId(2314637556L);
        chat.setTitle(chatName);
        chat.setType("supergroup");
        return Optional.of(chat);
    }
}
