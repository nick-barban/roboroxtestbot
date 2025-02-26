package com.nb;

import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.assertions.Template;

import java.io.File;
import java.util.Collections;

class AppStackTest {

    @Test
    void testAppStack() {
        final String rrtbInputLambdaName = "rrtb-input-lambda";
        String pathname = AppStack.functionPath(rrtbInputLambdaName);
        File file = new File(pathname);
        boolean exists = file.exists();
        if (exists) {
            App parent = new App();
            AppStack stack = new AppStack(parent, "TestMicronautAppStack");
            Template template = Template.fromStack(stack);
            template.hasResourceProperties("AWS::Lambda::Function", Collections.singletonMap("Handler", "io.micronaut.chatbots.telegram.lambda.Handler"));
        }
    }
}
