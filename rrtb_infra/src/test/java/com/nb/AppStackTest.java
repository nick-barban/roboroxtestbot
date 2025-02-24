package com.nb;

import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.assertions.Template;

import java.io.File;
import java.util.Collections;

class AppStackTest {

    @Test
    void testAppStack() {
        String pathname = AppStack.functionPath();
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
