package com.nb;
import io.micronaut.function.aws.MicronautRequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
public class FunctionRequestHandler extends MicronautRequestHandler<ScheduledEvent, Void> {
    @Override
    public Void execute(ScheduledEvent input) {
        return null;
    }
}
