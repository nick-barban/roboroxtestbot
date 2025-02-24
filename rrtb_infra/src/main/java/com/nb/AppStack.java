package com.nb;

import io.micronaut.aws.cdk.function.MicronautFunction;
import io.micronaut.aws.cdk.function.MicronautFunctionFile;
import io.micronaut.starter.application.ApplicationType;
import io.micronaut.starter.options.BuildTool;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.iam.IManagedPolicy;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.lambda.Architecture;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionUrl;
import software.amazon.awscdk.services.lambda.FunctionUrlAuthType;
import software.amazon.awscdk.services.lambda.FunctionUrlOptions;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.Tracing;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AppStack extends Stack {

    public AppStack(final Construct parent, final String id) {
        this(parent, id, null);
    }

    public AppStack(final Construct parent, final String id, final StackProps props) {
        super(parent, id, props);

        Map<String, String> environmentVariables = new HashMap<>();
        Function function = MicronautFunction.create(ApplicationType.FUNCTION,
                false,
                this,
                        // TODO by nickbarban: 17/02/25 Should be renamed
                "micronaut-function")
                .runtime(Runtime.JAVA_17)
                .handler("io.micronaut.chatbots.telegram.lambda.Handler")
                .environment(environmentVariables)
                .code(Code.fromAsset(functionPath()))
                .timeout(Duration.seconds(10))
                .memorySize(2048)
                .logRetention(RetentionDays.ONE_WEEK)
                .tracing(Tracing.ACTIVE)
                .architecture(Architecture.ARM_64)
                .build();
        final IManagedPolicy sqsCreateQueuePolicy = ManagedPolicy.fromAwsManagedPolicyName("AmazonSQSFullAccess");
        Objects.requireNonNull(function.getRole()).addManagedPolicy(sqsCreateQueuePolicy);

        FunctionUrl functionUrl = function.addFunctionUrl(FunctionUrlOptions.builder()
                .authType(FunctionUrlAuthType.NONE)
                .build());
// TODO by nickbarban: 17/02/25 Should be renamed
        CfnOutput.Builder.create(this, "MnTestApiUrl")
                .exportName("MnTestApiUrl")
                .value(functionUrl.getUrl())
                .build();
    }

    public static String functionPath() {
        return "../app/target/" + functionFilename();
    }

    public static String functionFilename() {
        return MicronautFunctionFile.builder()
            .graalVMNative(false)
                // TODO by nickbarban: 17/02/25 Should be fetched from app's pom version
            .version("0.1")
            .archiveBaseName("rrtb")
            .buildTool(BuildTool.MAVEN)
            .build();
    }
}