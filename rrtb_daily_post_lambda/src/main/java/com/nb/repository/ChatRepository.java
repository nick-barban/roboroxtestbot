package com.nb.repository;

import java.util.Optional;

public interface ChatRepository {
    Optional<Long> getChatIdByName(String chatName);
}
