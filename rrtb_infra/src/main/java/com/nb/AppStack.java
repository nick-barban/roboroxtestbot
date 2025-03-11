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
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.GlobalSecondaryIndexProps;
import software.amazon.awscdk.services.dynamodb.ProjectionType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.events.CronOptions;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.Schedule;
import software.amazon.awscdk.services.events.targets.LambdaFunction;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.IManagedPolicy;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
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
import software.amazon.awscdk.services.sqs.CfnQueue;
import software.constructs.Construct;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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

        // Create GitHub Actions IAM policy
        final PolicyStatement githubActionsPolicy = PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(Arrays.asList(
                        // ECR permissions
                        "ecr:SetRepositoryPolicy",
                        "ecr:GetRepositoryPolicy",
                        "ecr:InitiateLayerUpload",
                        "ecr:UploadLayerPart",
                        "ecr:CompleteLayerUpload",
                        "ecr:BatchCheckLayerAvailability",
                        "ecr:PutImage",
                        "ecr:CreateRepository",
                        "ecr:DescribeRepositories",
                        "ecr:DeleteRepository",
                        "ecr:GetAuthorizationToken",
                        // SSM permissions
                        "ssm:PutParameter",
                        "ssm:GetParameter",
                        "ssm:DeleteParameter",
                        // S3 permissions
                        "s3:PutObject",
                        "s3:GetObject",
                        "s3:ListBucket",
                        "s3:DeleteObject",
                        "s3:GetBucketLocation",
                        "s3:PutBucketPolicy",
                        "s3:CreateBucket",
                        "s3:PutBucketVersioning",
                        "s3:GetBucketPolicy",
                        "s3:PutBucketPublicAccessBlock",
                        "s3:GetBucketPublicAccessBlock",
                        // CloudFormation permissions
                        "cloudformation:CreateStack",
                        "cloudformation:DeleteStack",
                        "cloudformation:DescribeStacks",
                        "cloudformation:UpdateStack",
                        "cloudformation:CreateChangeSet",
                        "cloudformation:DeleteChangeSet",
                        "cloudformation:DescribeChangeSet",
                        "cloudformation:ExecuteChangeSet",
                        "cloudformation:ListStacks",
                        "cloudformation:GetTemplateSummary",
                        "cloudformation:DescribeStackEvents"))
                .resources(Arrays.asList(
                        // ECR resources
                        "arn:aws:ecr:" + this.getRegion() + ":" + this.getAccount() + ":repository/*",
                        // SSM resources
                        "arn:aws:ssm:" + this.getRegion() + ":" + this.getAccount() + ":parameter/cdk-bootstrap/*",
                        // S3 resources
                        "arn:aws:s3:::cdk-hnb659fds-assets-" + this.getAccount() + "-" + this.getRegion(),
                        "arn:aws:s3:::cdk-hnb659fds-assets-" + this.getAccount() + "-" + this.getRegion() + "/*",
                        "arn:aws:s3:::cdk-rrtb-assets-" + this.getAccount() + "-" + this.getRegion(),
                        "arn:aws:s3:::cdk-rrtb-assets-" + this.getAccount() + "-" + this.getRegion() + "/*",
                        // CloudFormation resources
                        "arn:aws:cloudformation:" + this.getRegion() + ":" + this.getAccount() + ":stack/*"))
                .build();

        // Add a separate policy statement for IAM permissions
        final PolicyStatement iamPolicy = PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(Arrays.asList(
                        "iam:CreateRole",
                        "iam:DeleteRole",
                        "iam:GetRole",
                        "iam:PutRolePolicy",
                        "iam:DeleteRolePolicy",
                        "iam:GetRolePolicy",
                        "iam:AttachRolePolicy",
                        "iam:DetachRolePolicy",
                        "iam:PassRole",
                        "iam:UpdateAssumeRolePolicy",
                        "iam:ListRolePolicies",
                        "iam:ListAttachedRolePolicies",
                        "sts:AssumeRole"))
                .resources(Arrays.asList(
                        "arn:aws:iam::" + this.getAccount() + ":role/*",
                        "arn:aws:iam::" + this.getAccount() + ":role/cdk-*"))
                .build();

        // Create CDK roles with trust relationships
        final Role deployRole = Role.Builder.create(this, "CdkDeployRole")
                .roleName("cdk-rrtb-deploy-role-" + this.getRegion())
                .assumedBy(new ServicePrincipal("cloudformation.amazonaws.com"))
                .build();

        final Role publishingRole = Role.Builder.create(this, "CdkPublishingRole")
                .roleName("cdk-rrtb-file-publishing-role-" + this.getRegion())
                .assumedBy(new ServicePrincipal("cloudformation.amazonaws.com"))
                .build();

        final Role lookupRole = Role.Builder.create(this, "CdkLookupRole")
                .roleName("cdk-rrtb-lookup-role-" + this.getRegion())
                .assumedBy(new ServicePrincipal("cloudformation.amazonaws.com"))
                .build();

        final ManagedPolicy githubActionsManagedPolicy = ManagedPolicy.Builder.create(this, "GitHubActionsPolicy")
                .managedPolicyName("GitHubActionsPolicy")
                .statements(Arrays.asList(githubActionsPolicy, iamPolicy))
                .description("Policy for GitHub Actions to manage CDK resources")
                .build();

        CfnOutput.Builder.create(this, "GitHubActionsPolicyArn")
                .exportName("GitHubActionsPolicyArn")
                .value(githubActionsManagedPolicy.getManagedPolicyArn())
                .build();

        // Create School table
        final Table schoolTable = Table.Builder.create(this, "SchoolTable")
                .tableName("School")
                .partitionKey(Attribute.builder()
                        .name("id")
                        .type(AttributeType.STRING)
                        .build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .removalPolicy(RemovalPolicy.DESTROY)
                .pointInTimeRecovery(true)
                .build();
        CfnOutput.Builder.create(this, "SchoolTableName")
                .exportName("SchoolTableName")
                .value(schoolTable.getTableName())
                .build();

        // Create Group table
        final Table groupTable = Table.Builder.create(this, "GroupTable")
                .tableName("Group")
                .partitionKey(Attribute.builder()
                        .name("id")
                        .type(AttributeType.STRING)
                        .build())
                .sortKey(Attribute.builder()
                        .name("schoolId")
                        .type(AttributeType.STRING)
                        .build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .removalPolicy(RemovalPolicy.DESTROY)
                .pointInTimeRecovery(true)
                .build();
        // Create GSIs for Group table
        groupTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("TeacherIndex")
                .partitionKey(Attribute.builder()
                        .name("teacherId")
                        .type(AttributeType.STRING)
                        .build())
                .projectionType(ProjectionType.ALL)
                .build());
        CfnOutput.Builder.create(this, "GroupTableName")
                .exportName("GroupTableName")
                .value(groupTable.getTableName())
                .build();

        // Create User table
        final Table userTable = Table.Builder.create(this, "UserTable")
                .tableName("User")
                .partitionKey(Attribute.builder()
                        .name("id")
                        .type(AttributeType.STRING)
                        .build())
                .sortKey(Attribute.builder()
                        .name("type")
                        .type(AttributeType.STRING)
                        .build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .removalPolicy(RemovalPolicy.DESTROY)
                .pointInTimeRecovery(true)
                .build();
        // Create GSIs for User table
        userTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("ParentIndex")
                .partitionKey(Attribute.builder()
                        .name("parentId")
                        .type(AttributeType.STRING)
                        .build())
                .projectionType(ProjectionType.ALL)
                .build());
        CfnOutput.Builder.create(this, "UserTableName")
                .exportName("UserTableName")
                .value(userTable.getTableName())
                .build();

        // New table for tracking user states during post creation
        final Table userStateTable = Table.Builder.create(this, "UserStateTable")
                .tableName("UserState")
                .partitionKey(Attribute.builder()
                        .name("userId")
                        .type(AttributeType.STRING)
                        .build())
                .sortKey(Attribute.builder()
                        .name("commandType")
                        .type(AttributeType.STRING)
                        .build())
                .removalPolicy(RemovalPolicy.DESTROY)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .pointInTimeRecovery(true)
                .timeToLiveAttribute("ttl")
                .build();
        CfnOutput.Builder.create(this, "UserStateTableName")
                .exportName("UserStateTableName")
                .value(userStateTable.getTableName())
                .build();

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
        
        // Add DynamoDB permissions
        final PolicyStatement dynamoDbPolicy = PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(Arrays.asList(
                        "dynamodb:GetItem",
                        "dynamodb:PutItem",
                        "dynamodb:UpdateItem",
                        "dynamodb:DeleteItem",
                        "dynamodb:Query",
                        "dynamodb:Scan"))
                .resources(Arrays.asList(
                        userStateTable.getTableArn(),
                        userStateTable.getTableArn() + "/index/*",
                        userTable.getTableArn(),
                        userTable.getTableArn() + "/index/*",
                        groupTable.getTableArn(),
                        groupTable.getTableArn() + "/index/*",
                        schoolTable.getTableArn(),
                        schoolTable.getTableArn() + "/index/*"))
                .build();
        rrtbInputLambda.addToRolePolicy(dynamoDbPolicy);

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
                .schedule(Schedule.cron(CronOptions.builder()
                        .minute("0")
                        .hour("7")
                        .day("*")
                        .month("*")
                        .year("*")
                        .build()))
                .build();
        rule.addTarget(LambdaFunction.Builder.create(rrtbDailyPostLambda).build());
        // Create output Lambda function
        final Function rrtbOutputLambda = MicronautFunction.create(ApplicationType.FUNCTION,
                        false,
                        this,
                        RRTB_OUTPUT_TBOT)
                .runtime(Runtime.JAVA_17)
                .handler("com.nb.FunctionRequestHandler")
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
                .resources(List.of(outputQueue.getAttrArn()))
                .build();

        rrtbOutputLambda.addToRolePolicy(sqsPolicy);

        // Add SQS trigger to Lambda
        final CfnEventSourceMapping eventSourceMapping = CfnEventSourceMapping.Builder.create(this, "RrtbOutputQueueMapping")
                .functionName(rrtbOutputLambda.getFunctionName())
                .eventSourceArn(outputQueue.getAttrArn())
                .batchSize(1)
                .build();
        CfnOutput.Builder.create(this, "RrtbOutputQueueEventMapping")
                .exportName("RrtbOutputQueueEventMapping")
                .value(eventSourceMapping.getAttrEventSourceMappingArn())
                .build();
    }

    public static String functionPath(String functionName) {
        final String folder = switch (functionName) {
            case RRTB_INPUT_LAMBDA -> "../rrtb_input_tbot/target/";
            case RRTB_DAILY_POST_LAMBDA -> "../rrtb_daily_post_lambda/target/";
            case RRTB_OUTPUT_TBOT -> "../rrtb_output_tbot/target/";
            default -> throw new IllegalArgumentException("Unknown function name: " + functionName);
        };

        return folder + functionFilename(functionName);
    }

    public static String functionFilename(String functionName) {
        MicronautFunctionFile.Builder builder = MicronautFunctionFile.builder()
                .graalVMNative(false)
                // TODO by nickbarban: 17/02/25 Should be fetched from app's pom version
                .version("0.1");
        switch (functionName) {
            case RRTB_INPUT_LAMBDA -> builder.archiveBaseName("rrtb_input_tbot");
            case RRTB_DAILY_POST_LAMBDA -> builder.archiveBaseName("rrtb_daily_post_lambda");
            case RRTB_OUTPUT_TBOT -> builder.archiveBaseName("rrtb_output_tbot");
            default -> throw new IllegalArgumentException("Unknown function name: " + functionName);
        }

        return builder
                .buildTool(BuildTool.MAVEN)
                .build();
    }
}