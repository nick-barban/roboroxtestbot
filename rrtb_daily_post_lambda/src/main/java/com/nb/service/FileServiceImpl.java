package com.nb.service;

import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Singleton
public class FileServiceImpl implements FileService {

    private static final Logger LOG = LoggerFactory.getLogger(FileServiceImpl.class);

    @Override
    public Map<String, String> readDailyPosts() {
        LOG.info("Reading files");
        return Map.of();
    }
}
