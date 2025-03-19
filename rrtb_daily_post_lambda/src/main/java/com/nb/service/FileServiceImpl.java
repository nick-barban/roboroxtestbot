package com.nb.service;

import io.micronaut.objectstorage.aws.AwsS3ObjectStorageEntry;
import io.micronaut.objectstorage.aws.AwsS3Operations;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class FileServiceImpl implements FileService {

    private static final Logger LOG = LoggerFactory.getLogger(FileServiceImpl.class);
    private final AwsS3Operations awsS3Operations;

    public FileServiceImpl(@Named("posts") AwsS3Operations awsS3Operations) {
        this.awsS3Operations = awsS3Operations;
    }

    private Map<String, String> readPosts(String prefix) {
            LOG.info("Reading files with prefix: {}", prefix);
            final Set<String> objects = awsS3Operations.listObjects();
            LOG.info("Next files were read: {}", objects);
    
            return objects.stream()
                    .filter(key -> key.startsWith(prefix))
                .collect(Collectors.toMap(s -> s, this::retrieve));
    }

    @Override
    public Map<String, String> readTodayPosts() {
        final String today = LocalDate.now().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        final String prefix = "templates/daily/%s/".formatted(today.toLowerCase());
        return readPosts(prefix);
    }

    private String retrieve(String s) {
        return awsS3Operations.retrieve(s)
                .map(this::extract)
                .orElse(null);
    }

    private String extract(AwsS3ObjectStorageEntry entry) {
        try {
            return new String(entry.getInputStream().readAllBytes());
        } catch (IOException e) {
            LOG.warn("Failed to read s3 object storage entry: %s".formatted(entry.getKey()), e);
            return null;
        }
    }
}
