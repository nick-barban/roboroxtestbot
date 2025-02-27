package com.nb.repository;

import java.util.Optional;

public class ChatRepositoryImpl implements ChatRepository {
    @Override
    public Optional<Long> getChatIdByName(String chatName) {
        return Optional.of(1002314637556L);
    }
}
