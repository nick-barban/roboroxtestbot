package com.nb;

import io.micronaut.aws.cdk.function.MicronautFunction;
import io.micronaut.aws.cdk.function.MicronautFunctionFile;
import io.micronaut.starter.application.ApplicationType;
import io.micronaut.starter.options.BuildTool;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.Schedule;
import software.amazon.awscdk.services.events.targets.LambdaFunction;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.IManagedPolicy;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.lambda.Architecture;
import software.amazon.awscdk.services.lambda.CfnEventSourceMapping;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionUrl;
import software.amazon.awscdk.services.lambda.FunctionUrlAuthType;
import software.amazon.awscdk.services.lambda.FunctionUrlOptions;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.Tracing;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketEncryption;
import software.amazon.awscdk.services.scheduler.CfnSchedule;
import software.amazon.awscdk.services.scheduler.CfnScheduleGroup;
import software.amazon.awscdk.services.sqs.CfnQueue;
import software.constructs.Construct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

public class AppStack extends Stack {

    private static final String RRTB_INPUT_LAMBDA = "rrtb-input-lambda";
    private static final String RRTB_DAILY_POST_LAMBDA = "rrtb-daily-post-lambda";
    public static final String RRTB_OUTPUT_TBOT = "rrtb-output-tbot";

    public AppStack(final Construct parent, final String id) {
        this(parent, id, null);
    }

    public AppStack(final Construct parent, final String id, final StackProps props) {
        super(parent, id, props);

        final Bucket bucket = Bucket.Builder.create(this, "rrtb-posts")
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .encryption(BucketEncryption.S3_MANAGED)
                .enforceSsl(true)
                .versioned(true)
                .removalPolicy(RemovalPolicy.RETAIN)
                .build();
        CfnOutput.Builder.create(this, "RrtbBucket")
                .exportName("RrtbBucketName")
                .value(bucket.getBucketName())
                .build();

        final Function rrtbInputLambda = MicronautFunction.create(ApplicationType.FUNCTION,
                false,
                this,
                        RRTB_INPUT_LAMBDA)
                .runtime(Runtime.JAVA_17)
                .handler("io.micronaut.chatbots.telegram.lambda.Handler")
                .environment(new HashMap<>())
                .code(Code.fromAsset(functionPath(RRTB_INPUT_LAMBDA)))
                .timeout(Duration.seconds(10))
                .memorySize(256)
                .logRetention(RetentionDays.ONE_WEEK)
                .tracing(Tracing.ACTIVE)
                .architecture(Architecture.ARM_64)
                .build();
        final IManagedPolicy sqsCreateQueuePolicy = ManagedPolicy.fromAwsManagedPolicyName("AmazonSQSFullAccess");
        Objects.requireNonNull(rrtbInputLambda.getRole()).addManagedPolicy(sqsCreateQueuePolicy);

        final FunctionUrl rrtbInputUrl = rrtbInputLambda.addFunctionUrl(FunctionUrlOptions.builder()
                .authType(FunctionUrlAuthType.NONE)
                .build());
        CfnOutput.Builder.create(this, "RrtbApiUrl")
                .exportName("RrtbApiUrl")
                .value(rrtbInputUrl.getUrl())
                .build();

        final Function rrtbDailyPostLambda = MicronautFunction.create(ApplicationType.FUNCTION,
                        false,
                        this,
                        RRTB_DAILY_POST_LAMBDA)
                .runtime(Runtime.JAVA_17)
                .handler("com.nb.SchedulerHandler")
                .environment(new HashMap<>())
                .code(Code.fromAsset(functionPath(RRTB_DAILY_POST_LAMBDA)))
                .timeout(Duration.seconds(20))
                .memorySize(256)
                .logRetention(RetentionDays.ONE_WEEK)
                .tracing(Tracing.ACTIVE)
                .architecture(Architecture.ARM_64)
                .build();
        final IManagedPolicy s3ReadOnlyPolicy = ManagedPolicy.fromAwsManagedPolicyName("AmazonS3ReadOnlyAccess");
        Objects.requireNonNull(rrtbDailyPostLambda.getRole()).addManagedPolicy(s3ReadOnlyPolicy);
        Objects.requireNonNull(rrtbDailyPostLambda.getRole()).addManagedPolicy(sqsCreateQueuePolicy);
        CfnOutput.Builder.create(this, "RrtbDailyPostLambda")
                .exportName("RrtbDailyPostLambda")
                .value(rrtbDailyPostLambda.getFunctionArn())
                .build();
        final Rule rule = Rule.Builder.create(this, "rrtb-daily-rule")
                // TODO by nickbarban: 26/02/25 Should be replaced with cron from env
                .schedule(Schedule.rate(Duration.minutes(1)))
                .build();
        rule.addTarget(LambdaFunction.Builder.create(rrtbDailyPostLambda).build());
        // Create output Lambda function
        final Function rrtbOutputLambda = MicronautFunction.create(ApplicationType.FUNCTION,
                        false,
                        this,
                        RRTB_OUTPUT_TBOT)
                .runtime(Runtime.JAVA_17)
                .handler("com.nb.handler.FunctionRequestHandler")
                .environment(new HashMap<>())
                .code(Code.fromAsset(functionPath(RRTB_OUTPUT_TBOT)))
                .timeout(Duration.seconds(10))
                .memorySize(256)
                .logRetention(RetentionDays.ONE_WEEK)
                .tracing(Tracing.ACTIVE)
                .architecture(Architecture.ARM_64)
                .build();
        CfnOutput.Builder.create(this, "RrtbOutputLambda")
                .exportName("RrtbOutputLambda")
                .value(rrtbOutputLambda.getFunctionArn())
                .build();

        // Create SQS Queue
        final CfnQueue inputQueue = CfnQueue.Builder.create(this, "rrtb_input.fifo")
                .queueName("rrtb_input.fifo")
                .fifoQueue(true)
                .build();
        CfnOutput.Builder.create(this, "RrtbInputQueue")
                .exportName("RrtbInputQueue")
                .value(inputQueue.getAttrArn())
                .build();
        // Create SQS Queue
        final CfnQueue inputDlq = CfnQueue.Builder.create(this, "dlq_rrtb_input.fifo")
                .queueName("dlq_rrtb_input.fifo")
                .fifoQueue(true)
                .build();
        CfnOutput.Builder.create(this, "RrtbInputDlq")
                .exportName("RrtbInputDlq")
                .value(inputDlq.getAttrArn())
                .build();

        // Create SQS Queue
        final CfnQueue outputQueue = CfnQueue.Builder.create(this, "rrtb_output.fifo")
                .queueName("rrtb_output.fifo")
                .fifoQueue(true)
                .build();
        CfnOutput.Builder.create(this, "RrtbOutputQueue")
                .exportName("RrtbOutputQueue")
                .value(outputQueue.getAttrArn())
                .build();
        // Create SQS Queue
        final CfnQueue outputDlq = CfnQueue.Builder.create(this, "dlq_rrtb_output.fifo")
                .queueName("dlq_rrtb_output.fifo")
                .fifoQueue(true)
                .build();
        CfnOutput.Builder.create(this, "RrtbOutputDlq")
                .exportName("RrtbOutputDlq")
                .value(outputDlq.getAttrArn())
                .build();

        // Grant Lambda permissions to read from SQS
        final PolicyStatement sqsPolicy = PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(Arrays.asList(
                        "sqs:ReceiveMessage",
                        "sqs:DeleteMessage",
                        "sqs:GetQueueAttributes"))
                .resources(Arrays.asList(outputQueue.getAttrArn()))
                .build();

        rrtbOutputLambda.addToRolePolicy(sqsPolicy);

        // Add SQS trigger to Lambda
        final CfnEventSourceMapping eventSourceMapping = CfnEventSourceMapping.Builder.create(this, "RrtbOutputQueueMapping")
                .functionName(rrtbOutputLambda.getFunctionName())
                .eventSourceArn(outputQueue.getAttrArn())
                .batchSize(1)
                .build();
    }

    public static String functionPath(String functionName) {
        final String folder;
        if (functionName.equals(RRTB_INPUT_LAMBDA)) {
            folder = "../rrtb_input_tbot/target/";
        } else if (functionName.equals(RRTB_DAILY_POST_LAMBDA)) {
            folder = "../rrtb_daily_post_lambda/target/";
        } else if (functionName.equals(RRTB_OUTPUT_TBOT)) {
            folder = "../rrtb_output_tbot/target/";
        } else {
            throw new IllegalArgumentException("Unknown function name: " + functionName);
        }

        return folder + functionFilename(functionName);
    }

    public static String functionFilename(String functionName) {
        MicronautFunctionFile.Builder builder = MicronautFunctionFile.builder()
                .graalVMNative(false)
                // TODO by nickbarban: 17/02/25 Should be fetched from app's pom version
                .version("0.1");
        if (functionName.equals(RRTB_INPUT_LAMBDA)) {
            builder.archiveBaseName("rrtb_input_tbot");
        } else if (functionName.equals(RRTB_DAILY_POST_LAMBDA)) {
            builder.archiveBaseName("rrtb_daily_post_lambda");
        } else if (functionName.equals(RRTB_OUTPUT_TBOT)) {
            builder.archiveBaseName("rrtb_output_tbot");
        } else {
            throw new IllegalArgumentException("Unknown function name: " + functionName);
        }

        return builder
                .buildTool(BuildTool.MAVEN)
                .build();
    }
}