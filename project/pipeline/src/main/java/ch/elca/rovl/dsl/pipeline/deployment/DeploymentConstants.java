package ch.elca.rovl.dsl.pipeline.deployment;

import com.azure.core.management.Region;

/**
 * Constant values used for deployment.
 */
public class DeploymentConstants {
    public static final String AZURE_DEFAULT_RESOURCE_GROUP = "demo-playground";
    public static final Region AZURE_DEFAULT_REGION = Region.EUROPE_WEST;
    public static final String AZURE_DEFAULT_REGION_STR = "westeurope";
    public static final String AZURE_NAMESPACE_SEND_SAS = "SendAccessKey";
    public static final String AZURE_NAMESPACE_RECV_SAS = "ListenAccessKey";
    public static final String AZURE_FUNCTION_URL_FORMAT = "https://%s/api/%s";

    public static final String AWS_SQS_SEND_POLICY_PREFIX = "dslengine-sqs-send-";
    public static final String AWS_SQS_RECV_POLICY_PREFIX = "dslengine-sqs-listen-";
    public static final String AWS_LAMBDA_ROLE_POSTFIX = "-generated-role";
    public static final String AWS_REST_API_PREFIX = "rest-api-";
    public static final String AWS_REST_URL_FORMAT = "https://%s.execute-api.%s.amazonaws.com/%s";
    public static final String AWS_DEFAULT_REGION = "eu-central-1";
    public static final String AWS_LAMBDA_INVOKE_POLICY_PREFIX = "lambda-integration-invoke-";
    public static final String AWS_LAMBDA_INVOKE_ROLE_PREFIX = "LambdaIntegrationInvoke-";
    public static final String AWS_DB_SUBNET_GROUP = "databases-subnet-group";
    public static final String AWS_DB_SUBNET_GROUP_DESC = "Subnet group for databases";
    public static final String AWS_DB_SUBNET_REGION_ONE = AWS_DEFAULT_REGION + "a";
    public static final String AWS_DB_SUBNET_REGION_TWO = AWS_DEFAULT_REGION + "b";
    public static final String AWS_DB_VPC_CIDR = "10.180.0.64/26";
    public static final String AWS_DB_SUBNET_ONE_CIDR = "10.180.0.64/28";
    public static final String AWS_DB_SUBNET_TWO_CIDR = "10.180.0.80/28";
    public static final String AWS_LAMBDA_VPC_POLICY = "LambdaJoinVpcPolicy";
}
