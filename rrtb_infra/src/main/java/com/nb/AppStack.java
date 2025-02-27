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
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.IManagedPolicy;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Policy;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.Architecture;
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
import software.constructs.Construct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class AppStack extends Stack {

    private static final String RRTB_INPUT_LAMBDA = "rrtb-input-lambda";
    private static final String RRTB_DAILY_POST_LAMBDA = "rrtb-daily-post-lambda";

    public AppStack(final Construct parent, final String id) {
        this(parent, id, null);
    }

    public AppStack(final Construct parent, final String id, final StackProps props) {
        super(parent, id, props);

        final List<Role> roles = new ArrayList<>();

        final Bucket bucket = Bucket.Builder.create(this, "rrtb-bucket")
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
                .timeout(Duration.seconds(10))
                .memorySize(256)
                .logRetention(RetentionDays.ONE_WEEK)
                .tracing(Tracing.ACTIVE)
                .architecture(Architecture.ARM_64)
                .build();
        CfnOutput.Builder.create(this, "RrtbDailyPostLambda")
                .exportName("RrtbDailyPostLambda")
                .value(rrtbDailyPostLambda.getFunctionArn())
                .build();
        // Create Role
        final Role rrtbDailyPostLambdaRole = Role.Builder.create(this, "RrtbDailyPostLambdaRole")
                .assumedBy(new ServicePrincipal("scheduler.amazonaws.com"))
                .build();
        roles.add(rrtbDailyPostLambdaRole);
        // Create Policy
        final PolicyStatement invokeFunctionStatement = PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(List.of("lambda:InvokeFunction"))
                .resources(List.of(rrtbDailyPostLambda.getFunctionArn()))
                .build();
        final Policy policy = Policy.Builder.create(this, "RrtbScheduleToInvokeLambdasPolicy")
                .roles(roles)
                .policyName("RRtbScheduleToInvokeLambdas")
                .statements(List.of(invokeFunctionStatement))
                .build();
        final CfnScheduleGroup scheduleGroup = CfnScheduleGroup.Builder.create(this, "rrtbScheduleGroup")
                .name("rrtbLambdaScheduleGroup")
                .build();
        // Create rate based schedule every 5 minutes using custom group name
        CfnSchedule cfnSchedule = CfnSchedule.Builder.create(this, "rrtbLambdaSchedule")
                // no flexible time window for this schedule
                .flexibleTimeWindow(CfnSchedule.FlexibleTimeWindowProperty.builder()
                        .mode("OFF").build())
                .groupName(scheduleGroup.getName())
                // TODO by nickbarban: 24/02/25 Should be extracted to env variable
                .scheduleExpression("rate(5 minute)")
                //create target builder and set Lambda ARN and role created above ARN
                .target(CfnSchedule.TargetProperty.builder()
                        .arn(rrtbDailyPostLambda.getFunctionArn())
                        .roleArn(rrtbDailyPostLambdaRole.getRoleArn())
                        .build())
                .build();
    }

    public static String functionPath(String functionName) {
        final String folder;
        if (functionName.equals(RRTB_INPUT_LAMBDA)) {
            folder = "../rrtb_input_tbot/target/";
        } else if (functionName.equals(RRTB_DAILY_POST_LAMBDA)) {
            folder = "../rrtb_daily_post_lambda/target/";
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
        } else {
            throw new IllegalArgumentException("Unknown function name: " + functionName);
        }

        return builder
                .buildTool(BuildTool.MAVEN)
                .build();
    }
}
