package com.nb;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.DefaultStackSynthesizer;

public class Main {
    public static void main(final String[] args) {
        App app = new App();

        // Create a custom synthesizer that skips bootstrap validation
        DefaultStackSynthesizer synthesizer = DefaultStackSynthesizer.Builder.create()
            .generateBootstrapVersionRule(false)
            .build();

        // Create the main application stack
        new AppStack(app, "RrtbAppStack", StackProps.builder()
                .env(Environment.builder()
                        .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                        .region(System.getenv("CDK_DEFAULT_REGION"))
                        .build())
                .synthesizer(synthesizer)
                .build());

        app.synth();
    }
}