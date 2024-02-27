package ch.elca.rovl.dsl.pipeline.deployment.helper;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.elca.rovl.dsl.api.function.models.functiontrigger.rest.FunctionHttpTrigger;
import ch.elca.rovl.dsl.pipeline.deployment.DeploymentConstants;
import ch.elca.rovl.dsl.pipeline.deployment.DeploymentEngine;
import ch.elca.rovl.dsl.pipeline.deployment.UpdateInfo;
import ch.elca.rovl.dsl.pipeline.deployment.accessinfo.database.AwsDatabaseAccess;
import ch.elca.rovl.dsl.pipeline.deployment.accessinfo.database.DatabaseAccess;
import ch.elca.rovl.dsl.pipeline.deployment.accessinfo.function.FunctionAccess;
import ch.elca.rovl.dsl.pipeline.deployment.accessinfo.queue.AwsQueueAccess;
import ch.elca.rovl.dsl.pipeline.deployment.accessinfo.queue.AzureQueueAccess;
import ch.elca.rovl.dsl.pipeline.deployment.helper.aws.ApiHelper;
import ch.elca.rovl.dsl.pipeline.deployment.helper.aws.IAMHelper;
import ch.elca.rovl.dsl.pipeline.deployment.helper.aws.LambdaHelper;
import ch.elca.rovl.dsl.pipeline.deployment.helper.aws.RdsHelper;
import ch.elca.rovl.dsl.pipeline.deployment.helper.aws.SQSHelper;
import ch.elca.rovl.dsl.pipeline.deployment.helper.aws.model.RestApiInfo;
import ch.elca.rovl.dsl.pipeline.deployment.resource.DeployedDatabase;
import ch.elca.rovl.dsl.pipeline.deployment.resource.DeployedFunction;
import ch.elca.rovl.dsl.pipeline.deployment.resource.DeployedQueue;
import ch.elca.rovl.dsl.pipeline.deployment.resource.DeployedResource;
import ch.elca.rovl.dsl.pipeline.templating.resource.DeployableDatabase;
import ch.elca.rovl.dsl.pipeline.templating.resource.DeployableFunction;
import ch.elca.rovl.dsl.pipeline.templating.resource.DeployableQueue;
import ch.elca.rovl.dsl.pipeline.util.Provider;
import ch.elca.rovl.dsl.pipeline.util.RequiredData;
import ch.elca.rovl.dsl.pipeline.util.ResourceLoader;
import ch.elca.rovl.dsl.pipeline.util.RequiredData.Type;
import ch.elca.rovl.dsl.resource.function.Function.FunctionConfigType;
import io.jsonwebtoken.Jwts;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;

/**
 * Helper class of {@link DeploymentEngine DeploymentEngine} to deploy resources on AWS.
 */
public class AwsDeploymentHelper implements DeploymentHelper {

    static final Logger LOG = LoggerFactory.getLogger("Deployment (AWS)");

    final SQSHelper sqsHelper;
    final IAMHelper iamHelper;
    final LambdaHelper lambdaHelper;
    final ApiHelper apiHelper;
    final RdsHelper rdsHelper;

    final String sqsWritePolicy;
    final String sqsReadPolicy;
    final String lambdaInvokePolicy;
    final String lambdaAssume;
    final String lambdaRestAssume;
    final String lambdaVpc;

    final String bucketName = "agnostic-serverless-functions";
    final String logPolicyName = "LambdaCloudWatchLogWriter";
    final String lambdaQuarkusHandler =
            "io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest";
    final String logPolicyArn;

    /**
     * Constructor. It inizalizes SDK clients and creates the root resource group if it doesn't
     * exist already.
     */
    public AwsDeploymentHelper() throws IOException, URISyntaxException {
        sqsHelper = new SQSHelper();
        iamHelper = new IAMHelper();
        lambdaHelper = new LambdaHelper(bucketName);
        apiHelper = new ApiHelper();
        rdsHelper = new RdsHelper();

        // load policy documents
        sqsWritePolicy = ResourceLoader.load("sqsSendPolicy.json", LOG);
        sqsReadPolicy = ResourceLoader.load("sqsReadPolicy.json", LOG);
        lambdaInvokePolicy = ResourceLoader.load("lambdaInvoke.json", LOG);
        lambdaAssume = ResourceLoader.load("lambdaAssume.json", LOG);
        lambdaRestAssume = ResourceLoader.load("lambdaRestAssume.json", LOG);
        lambdaVpc = ResourceLoader.load("lambdaVpcPolicy.json", LOG);
        
        if (sqsWritePolicy ==  null || sqsReadPolicy == null || lambdaInvokePolicy == null || lambdaAssume == null || lambdaRestAssume == null || lambdaVpc == null)
            throw new Error("Failed to load AWS policy documents from classpath resources.");
        
        // create cloudwatch log writer role if not already existing
        String logWriterPolicy = iamHelper.getPolicyArn(logPolicyName, true);
        logPolicyArn = logWriterPolicy != null ? logWriterPolicy
                : iamHelper.createPolicy(logPolicyName,
                        "Policy that allows resources to write logs",
                        ResourceLoader.load("lambdaCloudWatchPolicy.json", LOG));
    }

    // TODO create roles in db, create tables in db, support multiple dbs in same server
    /**
     * Deploys a database on AWS using the SDK and AWS CLI. Returns the database pipeline object
     * containing access info for the database.
     * 
     * @param database database to deploy
     * @return deployed database object
     */
    @Override
    public DeployedDatabase deploy(DeployableDatabase database) throws InterruptedException {
        List<String> subnetIds = rdsHelper.createSubnetGroupIfNotExisting();
        List<String> result = rdsHelper.createDatabaseIfNotExisting(database);

        DeployedDatabase ddb = new DeployedDatabase(database, result.get(0));
        AwsDatabaseAccess access = rdsHelper.getAccessRoles(database);
        access.addSubnetIds(subnetIds);
        access.addSecurityGroupIds(result.subList(1, result.size()));
        ddb.setAccessInfo(access);

        return ddb;
    }

    /**
     * Deploys a queue on AWS using the SDK. Returns the queue pipeline object containing access
     * info for the queue.
     * 
     * @param queue queue to deploy
     * @return deployed queue object
     */
    @Override
    public DeployedQueue deploy(DeployableQueue queue) {
        // get queue if exists, or deploy it
        String queueUrl = sqsHelper.getOrCreateQueue(queue.getName());
        String queueArn = sqsHelper.getQueueArn(queueUrl);
        if (queueArn == null)
            throw new IllegalStateException(String
                    .format("Queue '%s' exists but failed to retrieve its arn", queue.getName()));

        // get send and receive policies, or create them if they don't exist
        String sendPolicyArn = iamHelper.getOrCreatePolicyArn(
                DeploymentConstants.AWS_SQS_SEND_POLICY_PREFIX + queue.getName(),
                String.format("Policy that allows write permission to queue '%s", queue.getName()),
                String.format(sqsWritePolicy, queueArn));

        String receivePolicyArn = iamHelper.getOrCreatePolicyArn(
                DeploymentConstants.AWS_SQS_RECV_POLICY_PREFIX + queue.getName(),
                String.format("Policy that allows read permission to queue '%s", queue.getName()),
                String.format(sqsReadPolicy, queueArn));

        // set values and return deployed queue
        AwsQueueAccess accessInfo = new AwsQueueAccess(
                (AwsSessionCredentials) ProfileCredentialsProvider.create().resolveCredentials());
        accessInfo.setQueueArn(queueArn);
        accessInfo.setQueueUrl(queueUrl);
        accessInfo.setSendPolicyArn(sendPolicyArn);
        accessInfo.setReceivePolicyArn(receivePolicyArn);

        DeployedQueue dq = new DeployedQueue(queue);
        dq.setAccessInfo(accessInfo);

        return dq;
    }

    /**
     * Deploys a function on AWS using the SDK. Returns the function pipeline object containing
     * access info for the function.
     * 
     * @param function function to deploy
     * @return deployed function object
     */
    @Override
    public DeployedFunction deploy(DeployableFunction function) throws InterruptedException {
        String bucketKey = function.getCloudName() + ".zip";

        //LOG.info(String.format("%s: Generating zip...", function.getName()));

        // generate zip and upload to s3
        File zipFile = lambdaHelper.generateLambdaZip(function);
        lambdaHelper.uploadZip(bucketKey, zipFile);

        //LOG.info(String.format("%s: Creating execution role for Lambda...", function.getName()));

        String roleName = function.getCloudName() + DeploymentConstants.AWS_LAMBDA_ROLE_POSTFIX;
        String roleArn = iamHelper.getOrCreateRole(roleName, lambdaAssume);

        // NOTE need to wait a bit before attaching policies to new role
        // TODO could avoid wait if role already exists
        Thread.sleep(20000);

        iamHelper.attachPolicyToRole(roleName, logPolicyName, logPolicyArn);
        String lambdaArn = lambdaHelper.createOrUpdateLambda(function, bucketKey, roleArn,
                lambdaQuarkusHandler);

        DeployedFunction dfn = new DeployedFunction(function);

        // configure function triggers
        if (!function.isGlue()) {
            FunctionAccess access = new FunctionAccess();

            if (function.getFunction().requiresGlue()) {
                String functionUrl = lambdaHelper.getOrCreateFunctionUrl(function.getCloudName());
                access.setUrl(functionUrl);
            } else {
                switch (function.getFunction().getTriggerType()) {
                    case HTTP:
                    String functionUrl;
                        if (function.getFunction().getTrigger() != null) {
                            functionUrl = createRestIntegration(function, lambdaArn);
                        } else {
                            functionUrl = lambdaHelper.getOrCreateFunctionUrl(function.getCloudName());
                        }
                        access.setUrl(functionUrl);
                        break;
                    case QUEUE:
                        // noop
                        break;
                    // case TIMER: create event bridge
                    // break;
                    default:
                        close();
                        throw new IllegalStateException("Trigger type not supported.");
                }
            }

            // generate shared secret key to authenticate infra-provider comm
            SecretKey key = Jwts.SIG.HS256.key().build();
            access.setAuthKey(key);
            dfn.addRequiredData(new RequiredData(Type.SHARED_KEY, function.getFunction().getName()));

            dfn.setAccessInfo(access);

        } else {
            // if glue, tag it to receive secret shared key for infra-comm auth
            dfn.addRequiredData(
                    new RequiredData(Type.SHARED_KEY, function.getFunction().getName()));
        }

        dfn.addRequiredData(function.getRequiredData());

        LOG.info(String.format("%s: Deployment completed.", dfn.getName()));

        return dfn;
    }

    /**
     * Configure a deployed function with the necessary data to access the linked resources.
     */
    @Override
    public void configureFunction(DeployedFunction function, UpdateInfo updateInfo)
            throws InterruptedException {
        // LOG.info(String.format("Finalizing configuration of function '%s'...", function.getName()));
        // get function role
        String roleName = lambdaHelper.getRoleNameOfFunction(function.getCloudName());

        Map<String, String> envVars = new HashMap<>();
        List<String> triggerArns = new ArrayList<>();

        // gather data
        for (RequiredData.Type type : updateInfo.getTypes()) {
            List<DeployedResource> resources = updateInfo.getResourcesForType(type);

            switch (type) {
                case QUEUE_SEND:
                    for (DeployedResource queue : resources) {
                        if (queue.getProvider() == Provider.AWS) {
                            AwsQueueAccess access = (AwsQueueAccess) queue.getAccessInfo();
                            iamHelper.attachPolicyToRole(roleName,
                                    DeploymentConstants.AWS_SQS_SEND_POLICY_PREFIX
                                            + queue.getName(),
                                    access.getSendPolicyArn());
                            envVars.put("QUEUE_URL_" + queue.getName(), access.getQueueUrl());
                        } else if (queue.getProvider() == Provider.AZURE) {
                            AzureQueueAccess access = (AzureQueueAccess) queue.getAccessInfo();
                            // TODO is name without "-"?
                            envVars.put("CONN_STRING_" + queue.getName(), access.getSendSAS());
                        } else {
                            close();
                            throw new IllegalArgumentException(String.format(
                                    "provider '%s' not yet supported", queue.getProvider()));
                        }
                    }
                    break;
                case QUEUE_LISTEN:
                    for (DeployedResource queue : resources) {
                        if (queue.getProvider() == Provider.AWS) {
                            AwsQueueAccess access = (AwsQueueAccess) queue.getAccessInfo();
                            iamHelper
                                    .attachPolicyToRole(roleName,
                                            DeploymentConstants.AWS_SQS_RECV_POLICY_PREFIX
                                                    + queue.getName(),
                                            access.getReceivePolicyArn());
                            triggerArns.add(access.getQueueArn());
                        } else if (queue.getProvider() == Provider.AZURE) {
                            AzureQueueAccess access = (AzureQueueAccess) queue.getAccessInfo();
                            envVars.put("CONN_STRING_" + queue.getName(), access.getReceiveSAS());
                        } else {
                            close();
                            throw new IllegalArgumentException(String.format(
                                    "provider '%s' not yet supported", queue.getProvider()));
                        }
                    }
                    break;
                case QUEUE_TRIGGER:
                    for (DeployedResource queue : resources) {
                        if (queue.getProvider() == Provider.AWS) {
                            AwsQueueAccess access = (AwsQueueAccess) queue.getAccessInfo();
                            triggerArns.add(access.getQueueArn());
                        } else {
                            close();
                            throw new IllegalArgumentException(String.format(
                                    "provider '%s' not yet supported", queue.getProvider()));
                        }
                    }
                    break;
                case FUNCTION_URL:
                    for (DeployedResource fn : resources) {
                        FunctionAccess access = (FunctionAccess) fn.getAccessInfo();
                        envVars.put("FUNCTION_URL_" + fn.getName(), access.getUrl());
                        String encodedKey =
                            Base64.getEncoder().encodeToString(access.getAuthKey().getEncoded());
                        envVars.put("JWT_KEY_" + fn.getName(), encodedKey);
                    }
                    break;
                case DATABASE_CONNECTION:
                    // TODO cases of op needed
                    Set<String> subnetGroupIds = new HashSet<>();
                    Set<String> securityGroupIds = new HashSet<>();

                    for (DeployedResource db : resources) {
                        DatabaseAccess access = (DatabaseAccess) db.getAccessInfo();
                        envVars.put("PSQL_USER_" + db.getName(), access.superuser().username());
                        envVars.put("PSQL_PWD_" + db.getName(), access.superuser().password());

                        String url = ((DeployedDatabase) db).getEndpoint();
                        url = "jdbc:postgresql://" + url + ":5432/" + db.getName();
                        envVars.put("PSQL_URL_" + db.getName(), url);

                        if (db.getProvider() == Provider.AWS) {
                            subnetGroupIds.addAll(((AwsDatabaseAccess) access).getSubnetIds());
                            securityGroupIds
                                    .addAll(((AwsDatabaseAccess) access).getSecurityGroupIds());
                        }
                    }

                    if (!subnetGroupIds.isEmpty()) {
                        // update exec role to join vpc
                        String policyArn = iamHelper.getOrCreatePolicyArn(
                                DeploymentConstants.AWS_LAMBDA_VPC_POLICY,
                                "Policy that allows Lambda functions to join VPCs", lambdaVpc);
                        iamHelper.attachPolicyToRole(roleName,
                                DeploymentConstants.AWS_LAMBDA_VPC_POLICY, policyArn);

                        Thread.sleep(15000);
                        // add subnets
                        lambdaHelper.updateConfigWithSubnetGroups(function.getCloudName(),
                                new ArrayList<>(subnetGroupIds), new ArrayList<>(securityGroupIds));
                        Thread.sleep(15000);
                    }
                    break;
                case SHARED_KEY:
                    DeployedFunction dfn = (DeployedFunction) resources.get(0);
                    FunctionAccess access = (FunctionAccess) dfn.getAccessInfo();
                    String encodedKey =
                            Base64.getEncoder().encodeToString(access.getAuthKey().getEncoded());
                    envVars.put("JWT_KEY_" + dfn.getName(), encodedKey);
                    break;
                default:
                    close();
                    throw new IllegalArgumentException(
                            String.format("Type '%s' not supported.", type));
            }
        }


        // add env vars to function
        lambdaHelper.addEnvVarsToFunction(function.getCloudName(), envVars);

        // create event source mappings
        if (!triggerArns.isEmpty()) {
            // NOTE needed to delay creation of event source mapping
            Thread.sleep(10000);
            lambdaHelper.addEventSourceMappingsToFunction(function.getCloudName(), triggerArns);
        }
    }

    /**
     * Creates a REST API integration for the given lambda function.
     * 
     * @param function
     * @param lambdaArn
     * @return
     * @throws InterruptedException
     */
    private String createRestIntegration(DeployableFunction function, String lambdaArn)
            throws InterruptedException {
        FunctionHttpTrigger trigger =
                (FunctionHttpTrigger) function.getConfig().get(FunctionConfigType.TRIGGER);

        // create policy to invoke lambda from integration
        String lambdaInvokePolicyArn =
                iamHelper.getOrCreatePolicyArn(
                        DeploymentConstants.AWS_LAMBDA_INVOKE_POLICY_PREFIX
                                + function.getCloudName(),
                        "Policy to allow invocation of Lambda",
                        String.format(lambdaInvokePolicy, lambdaArn));

        // create role for policy
        String invokeLambdaRoleArn = iamHelper.getOrCreateRole(
                DeploymentConstants.AWS_LAMBDA_INVOKE_ROLE_PREFIX + function.getCloudName(),
                lambdaRestAssume);

        // NOTE maybe we don't have to wait here
        Thread.sleep(10000);
        // attach policy to role
        iamHelper.attachPolicyToRole(
                DeploymentConstants.AWS_LAMBDA_INVOKE_ROLE_PREFIX + function.getCloudName(),
                DeploymentConstants.AWS_LAMBDA_INVOKE_POLICY_PREFIX + function.getCloudName(),
                lambdaInvokePolicyArn);

        // create rest api
        RestApiInfo restInfo = apiHelper.getOrCreateRestApi(function.getCloudName(),
                trigger.getHttpMethods(), lambdaArn, invokeLambdaRoleArn);

        return restInfo.url();
    }

    @Override
    public void close() {
        sqsHelper.close();
        iamHelper.close();
        lambdaHelper.close();
        apiHelper.close();
        rdsHelper.close();
    }

}