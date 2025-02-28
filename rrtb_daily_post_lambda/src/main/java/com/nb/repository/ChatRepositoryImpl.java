package com.nb.repository;

import jakarta.inject.Singleton;

import java.util.Optional;

@Singleton
public class ChatRepositoryImpl implements ChatRepository {
    @Override
    public Optional<Long> getChatIdByName(String chatName) {
        return Optional.of(-1002314637556L);
    }
}
