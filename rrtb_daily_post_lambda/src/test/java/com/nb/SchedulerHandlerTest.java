package com.nb;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

class SchedulerHandlerTest extends AbstractTest {


    @Test
    void should() throws IOException, InterruptedException {
        org.testcontainers.containers.Container.ExecResult execResult = LOCAL_STACK_CONTAINER.execInContainer(
                "awslocal", "lambda", "create-function",
                "--function-name", "lambda-name",
                "--runtime", "java17",
                "--handler", "com.nb.SchedulerHandler",
                "--role", "arn:aws:iam::000000000000:role/lambda-role",
                "--zip-file", "fileb://lambda.jar",
                "--region", LOCAL_STACK_CONTAINER.getRegion()
        );
        assertResult(execResult);

        TimeUnit.SECONDS.sleep(5);

        execResult = LOCAL_STACK_CONTAINER.execInContainer("awslocal", "lambda", "wait", "function-active", "--function-name", "lambda-name");
        assertResult(execResult);

        execResult = LOCAL_STACK_CONTAINER.execInContainer("awslocal", "lambda", "invoke", "--function-name", "lambda-name", "output0.json");
        assertResult(execResult);

        execResult = LOCAL_STACK_CONTAINER.execInContainer("awslocal",
                "scheduler",
                "create-schedule",
                "--name", "rrtb-schedule",
                "--schedule-expression", "rate(5 minutes)",
                """
                        --target={
                        "RoleArn": "arn:aws:iam::000000000000:role/schedule-role",
                        "Arn":"arn:aws:lambda:us-east-1:000000000000:function:lambda-name",
                        "Input": "test"
                        }
                        """,
                "--flexible-time-window={\"Mode\": \"OFF\"}"
        );
        assertResult(execResult);
    }
}