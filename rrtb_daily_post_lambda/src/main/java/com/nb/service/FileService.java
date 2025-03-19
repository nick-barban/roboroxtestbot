package com.nb.service;

import java.util.Map;

public interface FileService {
    Map<String, String> readPosts();

    Map<String, String> readTodayPosts();
}
