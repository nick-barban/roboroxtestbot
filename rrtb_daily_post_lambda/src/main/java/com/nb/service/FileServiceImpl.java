package com.nb.service;

import io.micronaut.objectstorage.aws.AwsS3ObjectStorageEntry;
import io.micronaut.objectstorage.aws.AwsS3Operations;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

    @Override
    public Map<String, String> readPosts() {
        LOG.info("Reading files");
        final Set<String> objects = awsS3Operations.listObjects();
        LOG.info("Stored files: {}", objects);
        return objects.stream().collect(Collectors.toMap(s -> s, this::retrieve));
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
