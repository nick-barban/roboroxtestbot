package com.nb;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.function.aws.MicronautRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FunctionRequestHandler extends MicronautRequestHandler<String, String> {
    private static final Logger LOG = LoggerFactory.getLogger(FunctionRequestHandler.class);

    @Override
    public String execute(@Nullable String input) {
        LOG.info("Executing Lambda function with input: {}", input);
        return "OK";
    }
} 