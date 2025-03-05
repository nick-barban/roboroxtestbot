## Micronaut 4.7.4 Documentation

- [User Guide](https://docs.micronaut.io/4.7.4/guide/index.html)
- [API Reference](https://docs.micronaut.io/4.7.4/api/index.html)
- [Configuration Reference](https://docs.micronaut.io/4.7.4/guide/configurationreference.html)
- [Micronaut Guides](https://guides.micronaut.io/index.html)
---

# Telegram ChatBot

Follow the instructions in the [Micronaut ChatBot Documentation](https://micronaut-projects.github.io/micronaut-chatbots/latest/guide/) to create a Telegram ChatBot.

Once you have a username and HTTP auth key for your new bot, edit the application config in this project to set the bot username and make up a WEBHOOK_TOKEN so you can ensure it's Telegram that's calling your bot.

## Lambda handler class

The Cdk project defined in `rrtb_infra` is already configured to use `io.micronaut.chatbots.telegram.lambda.Handler` as the handler for your Lambda function.

You can then set up the Telegram webhook by running the following command:

```bash
curl -X POST 'https://api.telegram.org/bot${HTTP_AUTH_KEY}/setWebhook?url=${YOUR_HTTP_TRIGGER_URL}&secret_token=${YOUR_SECRET_TOKEN}'
```

Where HTTP_AUTH_KEY is the key given to you by the BotFather, YOUR_HTTP_TRIGGER_URL is the URL of your HTTP function and YOUR_SECRET_TOKEN is the value you chose for the WEBHOOK_TOKEN in the configuration.


## Requisites

- [AWS Account](https://aws.amazon.com/free/)
- [CDK CLI](https://docs.aws.amazon.com/cdk/v2/guide/cli.html)
- [AWS CLI](https://aws.amazon.com/cli/)

## How to deploy

### Generate the deployable artifact

```
./mvnw package
```

### Deploy

The `infra/cdk.json` file tells the CDK Toolkit how to execute your app.

`cd infra`
`cdk synth` - emits the synthesized CloudFormation template
`cdk deploy` - deploy this stack to your default AWS account/region
`cd ..`

Other useful commands:

`cdk diff` - compare deployed stack with current state
`cdk docs`- open CDK documentation

### Cleanup

```
cd infra
cdk destroy
cd ..
```


- [Micronaut Maven Plugin documentation](https://micronaut-projects.github.io/micronaut-maven-plugin/latest/)
## Feature aws-cdk documentation

- [https://docs.aws.amazon.com/cdk/v2/guide/home.html](https://docs.aws.amazon.com/cdk/v2/guide/home.html)


## Feature aws-lambda-function-url documentation

- [Micronaut AWS Lambda Function URLs documentation](https://micronaut-projects.github.io/micronaut-aws/latest/guide/index.html#amazonApiGateway)

- [https://docs.aws.amazon.com/lambda/latest/dg/lambda-urls.html](https://docs.aws.amazon.com/lambda/latest/dg/lambda-urls.html)


## Feature validation documentation

- [Micronaut Validation documentation](https://micronaut-projects.github.io/micronaut-validation/latest/guide/)


## Feature serialization-jackson documentation

- [Micronaut Serialization Jackson Core documentation](https://micronaut-projects.github.io/micronaut-serialization/latest/guide/)


## Feature maven-enforcer-plugin documentation

- [https://maven.apache.org/enforcer/maven-enforcer-plugin/](https://maven.apache.org/enforcer/maven-enforcer-plugin/)


## Feature aws-lambda-events-serde documentation

- [Micronaut AWS Lambda Events Serde documentation](https://micronaut-projects.github.io/micronaut-aws/snapshot/guide/#eventsLambdaSerde)

- [https://github.com/aws/aws-lambda-java-libs/tree/main/aws-lambda-java-events](https://github.com/aws/aws-lambda-java-libs/tree/main/aws-lambda-java-events)


## Feature aws-lambda documentation

- [Micronaut AWS Lambda Function documentation](https://micronaut-projects.github.io/micronaut-aws/latest/guide/index.html#lambda)


## Feature chatbots-telegram-lambda documentation

- [Micronaut Telegram ChatBot as AWS Lambda function documentation](https://micronaut-projects.github.io/micronaut-chatbots/latest/guide/)

--- 

#Prepare AWS

## Prepare AWS github actions

- creare user
    ```bash
    aws iam create-user --user-name github-actions
    ```

- create access keys for this user
    ```bash
    aws iam create-access-key --user-name github-actions
    ```

- attach the AWSCloudFormationFullAccess policy
    ```bash
  aws iam attach-user-policy --user-name github-actions --policy-arn arn:aws:iam::aws:policy/AWSCloudFormationFullAccess
  ```

- attach the AWSCloudFormationReadOnlyAccess policy for additional CloudFormation permissions
    ```bash
  aws iam attach-user-policy --user-name github-actions --policy-arn arn:aws:iam::aws:policy/AWSCloudFormationReadOnlyAccess
    ```

- attach the IAMFullAccess policy since CDK needs to create IAM roles and policies
    ```bash
  aws iam attach-user-policy --user-name github-actions --policy-arn arn:aws:iam::aws:policy/IAMFullAccess
    ``` 

- verify the attached policies
    ```bash
  aws iam list-attached-user-policies --user-name github-actions
    ```
