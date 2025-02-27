package com.nb;

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.nb.service.FileService;
import io.micronaut.function.aws.MicronautRequestHandler;
import io.micronaut.serde.annotation.SerdeImport;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@SerdeImport(ScheduledEvent.class)
public class SchedulerHandler extends MicronautRequestHandler<ScheduledEvent, Void> {

    private static final Logger LOG = LoggerFactory.getLogger(SchedulerHandler.class);

    @Inject
    private FileService fileService;

    @Override
    public Void execute(ScheduledEvent input) {
        LOG.info("Scheduled handler invocation");

        final Map<String, String> posts = fileService.readDailyPosts();
        LOG.info("Read {} posts", posts.size());

        return null;
    }
}
