package com.nb;

import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

class SchedulerHandlerTest extends AbstractTest {

    @Test
    @Ignore("the file does not exist as tests are running before projecg is packaged to jar")
    void shouldCreateMountableFileforHostFileOnWindows() {
        final MountableFile given = MountableFile.forHostPath("target/rrtb_daily_post_lambda-0.1.jar");

        final String mountablePath = given.getResolvedPath();

        final File actual = new File(mountablePath);
        Assertions.assertThat(actual).exists();
    }

    @Test
    @Ignore("the file does not exist as tests are running before projecg is packaged to jar")
    void shouldCheckLocalInfrastructure() throws IOException, InterruptedException {
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