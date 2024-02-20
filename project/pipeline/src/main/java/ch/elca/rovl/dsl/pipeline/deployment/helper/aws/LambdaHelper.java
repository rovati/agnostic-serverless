package ch.elca.rovl.dsl.pipeline.deployment.helper.aws;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elca.rovl.dsl.pipeline.templating.resource.DeployableFunction;
import ch.elca.rovl.dsl.pipeline.util.Constants;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.AddPermissionRequest;
import software.amazon.awssdk.services.lambda.model.Architecture;
import software.amazon.awssdk.services.lambda.model.CreateEventSourceMappingRequest;
import software.amazon.awssdk.services.lambda.model.CreateFunctionRequest;
import software.amazon.awssdk.services.lambda.model.CreateFunctionUrlConfigRequest;
import software.amazon.awssdk.services.lambda.model.Environment;
import software.amazon.awssdk.services.lambda.model.FunctionCode;
import software.amazon.awssdk.services.lambda.model.FunctionUrlAuthType;
import software.amazon.awssdk.services.lambda.model.GetFunctionConfigurationRequest;
import software.amazon.awssdk.services.lambda.model.GetFunctionRequest;
import software.amazon.awssdk.services.lambda.model.GetFunctionUrlConfigRequest;
import software.amazon.awssdk.services.lambda.model.ListEventSourceMappingsRequest;
import software.amazon.awssdk.services.lambda.model.PackageType;
import software.amazon.awssdk.services.lambda.model.ResourceConflictException;
import software.amazon.awssdk.services.lambda.model.ResourceNotFoundException;
import software.amazon.awssdk.services.lambda.model.UpdateFunctionCodeRequest;
import software.amazon.awssdk.services.lambda.model.UpdateFunctionConfigurationRequest;
import software.amazon.awssdk.services.lambda.model.VpcConfig;
import software.amazon.awssdk.services.lambda.model.Runtime;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class LambdaHelper {
    static final Logger LOG = LoggerFactory.getLogger("Deployment (AWS)");

    final LambdaClient lambdaClient;
    final S3Client s3Client;

    final String bucketName;

    public LambdaHelper(String bucketName) {
        lambdaClient = LambdaClient.builder().httpClientBuilder(ApacheHttpClient.builder()).build();
        s3Client = S3Client.builder().httpClientBuilder(ApacheHttpClient.builder()).build();

        this.bucketName = bucketName;

        // create bucket if it doesn't exist
        ListBucketsResponse bucketsList = s3Client.listBuckets();
        if (!bucketsList.buckets().stream().map(b -> b.name()).collect(Collectors.toList())
                .contains(this.bucketName)) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(this.bucketName).build());
        }

        // create log dir if it dosen't exist
        File logDirectory = new File(Constants.LOGS_DIR);
        if (!logDirectory.exists()) {
            logDirectory.mkdirs();
        }
    }

    /**
     * Generates and renames the zip file with the code for Lambda, and returns the
     * file
     * 
     * @param function
     * @return the zip file
     */
    public File generateLambdaZip(DeployableFunction function) {
        // generate zip
        String mavenCmd = " & mvn clean package -DskipTests";
        Process p;
        int cmdResult;
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "cd " + function.getGeneratedPath(), mavenCmd);
            pb.redirectOutput(new File(Constants.LOGS_DIR + "deployment-" + function.getName() + ".txt"));

            p = pb.start();
            cmdResult = p.waitFor();
            if (cmdResult != 0) {
                throw new Error("Zip generation for lambda failed.");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(String.format(
                    "Failed to run deployment command for function '%s'", function.getName()));
        }

        // rename zip to "<function-name>.zip"
        File functionZip = new File(function.getGeneratedPath() + "target/function.zip");
        File renamedFunctionZip = new File(
                function.getGeneratedPath() + "target/" + function.getCloudName() + ".zip");
        if (functionZip.exists()) {
            functionZip.renameTo(renamedFunctionZip);
        } else {
            throw new IllegalStateException("Zip file for AWS Lambda function does not exist.");
        }

        return renamedFunctionZip;
    }

    /**
     * Upload the code zip to s3
     * 
     * @param bucketKey
     * @param zipFile
     */
    public void uploadZip(String bucketKey, File zipFile) {
        s3Client.putObject(PutObjectRequest.builder().bucket(bucketName).key(bucketKey).build(),
                RequestBody.fromFile(zipFile));
    }

    // TODO update with function configuration
    /**
     * Creates the lambda function or updates it if it already exists.
     * 
     * @param function
     * @param bucketKey
     * @param lambdaRoleArn
     * @param handlerMethod
     * @return the arn of the function
     * @throws InterruptedException
     */
    public String createOrUpdateLambda(DeployableFunction function, String bucketKey,
            String lambdaRoleArn, String handlerMethod) throws InterruptedException {
        try {
            // get function if it exists
            String functionArn = lambdaClient
                    .getFunction(GetFunctionRequest.builder()
                            .functionName(function.getCloudName()).build())
                    .configuration().functionArn();

            // update code
            lambdaClient.updateFunctionCode(
                    UpdateFunctionCodeRequest.builder().functionName(function.getCloudName())
                            .s3Bucket(bucketName).s3Key(bucketKey).build());

            // wait while code configuration is being updated
            Thread.sleep(10000);
            // update configuration
            lambdaClient.updateFunctionConfiguration(UpdateFunctionConfigurationRequest.builder()
                    .handler(handlerMethod).functionName(function.getCloudName())
                    .runtime(Runtime.JAVA17).role(lambdaRoleArn).timeout(10).build());

            return functionArn;

        } catch (ResourceNotFoundException e) {
            // create lambda
            return lambdaClient.createFunction(CreateFunctionRequest.builder()
                    .functionName(function.getCloudName()).packageType(PackageType.ZIP)
                    .code(FunctionCode.builder().s3Bucket(bucketName).s3Key(bucketKey).build())
                    .role(lambdaRoleArn).handler(handlerMethod).runtime(Runtime.JAVA17)
                    .architectures(Architecture.X86_64).timeout(15).build()).functionArn();
        }
    }

    /**
     * Configures a function URL for the Lambda funciton if it does not exist
     * already, and returns
     * the Lambda function url.
     * 
     * @param functionName
     * @return function url
     */
    public String getOrCreateFunctionUrl(String functionName) {
        try {
            return lambdaClient.getFunctionUrlConfig(
                    GetFunctionUrlConfigRequest.builder().functionName(functionName).build())
                    .functionUrl();
        } catch (ResourceNotFoundException e) {
            // create url for function
            String functionUrl = lambdaClient.createFunctionUrlConfig(CreateFunctionUrlConfigRequest
                    .builder().functionName(functionName).authType(FunctionUrlAuthType.NONE) // TODO
                                                                                             // change
                    .build()).functionUrl();

            // allow invocation of function
            lambdaClient.addPermission(AddPermissionRequest.builder().functionName(functionName)
                    .functionUrlAuthType(FunctionUrlAuthType.NONE)
                    .action("lambda:InvokeFunctionUrl").principal("*")
                    .statementId("FunctionURLAllowPublicAccess").build());

            return functionUrl;
        }
    }

    /**
     * Returns the name of the execution role assigned to the givne function.
     * 
     * @param functionName
     * @return the execution role name
     */
    public String getRoleNameOfFunction(String functionName) {
        return Arn
                .fromString(lambdaClient.getFunctionConfiguration(GetFunctionConfigurationRequest
                        .builder().functionName(functionName).build()).role())
                .resource().resource();
    }

    /**
     * Updates the configuration of the given function by adding the given
     * environment variables.
     * 
     * @param functionName
     * @param envVars
     * @throws InterruptedException 
     */
    public void addEnvVarsToFunction(String functionName, Map<String, String> envVars) throws InterruptedException {
        int tries = 0;
        while (tries < 3) {
            try {
                lambdaClient.updateFunctionConfiguration(
                        UpdateFunctionConfigurationRequest.builder().functionName(functionName)
                                .environment(Environment.builder().variables(envVars).build()).build());
            } catch (ResourceConflictException e) {
                tries++;
                LOG.warn(String.format("Resource config when adding env vars to lambda '%s'. Retrying in a while...", functionName));
                Thread.sleep(10000);
                continue;
            }
            return;
        }
    }

    /**
     * Adds the event mappings from the given list of source arns to the given
     * function, skipping
     * mappings that already exist.
     * 
     * @param functionName
     * @param eventSourceArns list of arns of the event sources
     */
    public void addEventSourceMappingsToFunction(String functionName,
            List<String> eventSourceArns) {

        List<String> existingMappings = lambdaClient
                .listEventSourceMappings(
                        ListEventSourceMappingsRequest.builder().functionName(functionName).build())
                .eventSourceMappings().stream().map(esm -> esm.eventSourceArn())
                .collect(Collectors.toList());

        for (String arn : new HashSet<>(eventSourceArns)) {
            if (!existingMappings.contains(arn)) {
                lambdaClient.createEventSourceMapping(CreateEventSourceMappingRequest.builder()
                        .eventSourceArn(arn).functionName(functionName).build());
            }
        }

    }

    /**
     * Updates the configuration of the given Lambda by adding the given list of
     * subnet ids.
     * 
     * @param cloudFunctionName
     * @param subnetIds
     */
    public void updateConfigWithSubnetGroups(String cloudFunctionName, List<String> subnetIds,
            List<String> securityGroupIds) {
        lambdaClient
                .updateFunctionConfiguration(UpdateFunctionConfigurationRequest.builder()
                        .functionName(cloudFunctionName).vpcConfig(VpcConfig.builder()
                                .subnetIds(subnetIds).securityGroupIds(securityGroupIds).build())
                        .build());
    }

    public void close() {
        lambdaClient.close();
        s3Client.close();
    }
}
