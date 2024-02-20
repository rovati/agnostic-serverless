package ch.elca.rovl.dsl.pipeline.deployment.helper.aws;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.elca.rovl.dsl.pipeline.deployment.DeploymentConstants;
import ch.elca.rovl.dsl.pipeline.deployment.helper.aws.model.RestApiInfo;
import ch.elca.rovl.dsl.pipeline.util.Constants;
import ch.elca.rovl.dsl.resource.function.HttpMethod;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.apigateway.ApiGatewayClient;
import software.amazon.awssdk.services.apigateway.model.CreateRestApiRequest;
import software.amazon.awssdk.services.apigateway.model.CreateRestApiResponse;
import software.amazon.awssdk.services.apigateway.model.DeleteStageRequest;
import software.amazon.awssdk.services.apigateway.model.Deployment;
import software.amazon.awssdk.services.apigateway.model.EndpointConfiguration;
import software.amazon.awssdk.services.apigateway.model.EndpointType;
import software.amazon.awssdk.services.apigateway.model.GetDeploymentsRequest;
import software.amazon.awssdk.services.apigateway.model.GetDeploymentsResponse;
import software.amazon.awssdk.services.apigateway.model.GetResourceRequest;
import software.amazon.awssdk.services.apigateway.model.GetStagesRequest;
import software.amazon.awssdk.services.apigateway.model.PutMethodRequest;
import software.amazon.awssdk.services.apigateway.model.RestApi;
import software.amazon.awssdk.services.apigateway.model.TooManyRequestsException;
import software.amazon.awssdk.services.apigateway.model.CreateDeploymentRequest;
import software.amazon.awssdk.services.apigateway.model.CreateDeploymentResponse;

public class ApiHelper {

    static final Logger LOG = LoggerFactory.getLogger("Deployment (AWS)");

    final ApiGatewayClient client;
    List<RestApi> apis;

    public ApiHelper() {
        client = ApiGatewayClient.builder().httpClientBuilder(ApacheHttpClient.builder()).build();

        // create log dir if it dosen't exist
        File logDirectory = new File(Constants.LOGS_DIR);
        if (!logDirectory.exists()) {
            logDirectory.mkdirs();
        }
    }

    /**
     * Creates a rest api for the given function if it doesn't exist already, and enables the given
     * methods on the root resource.
     * 
     * @param functionName
     * @param methods
     * @param lambdaArn
     * @param invokeLambdaRoleArn
     * @return the id of the rest api and the id of the root resource of the rest api
     * @throws InterruptedException
     */
    public RestApiInfo getOrCreateRestApi(String functionName, List<HttpMethod> methods,
            String lambdaArn, String invokeLambdaRoleArn) throws InterruptedException {
        refreshApis(false);

        String restId = null;
        String rootResourceId = null;

        for (RestApi api : apis) {
            if (api.name().equals(DeploymentConstants.AWS_REST_API_PREFIX + functionName)) {
                restId = api.id();
                rootResourceId = api.rootResourceId();
                break;
            }
        }

        // if rest api doesn't exist, create it
        if (restId == null) {
            int tries = 1;
            boolean succeeded = false;
            TooManyRequestsException ex = null;

            while (tries < 10 && !succeeded) {
                try {
                    CreateRestApiResponse resp = client.createRestApi(CreateRestApiRequest.builder()
                            .name(DeploymentConstants.AWS_REST_API_PREFIX + functionName)
                            .description("Rest api for Lambda integration of " + functionName)
                            .endpointConfiguration(EndpointConfiguration.builder()
                                    .types(EndpointType.REGIONAL).build())
                            .build());

                    restId = resp.id();
                    rootResourceId = resp.rootResourceId();
                    refreshApis(true);
                    succeeded = true;
                } catch (TooManyRequestsException e) {
                    LOG.warn("Too Many Requests for createRestApi, attempt " + tries);
                    ex = e;
                    tries++;

                    int mult = new Random().nextInt(5000);
                    Thread.sleep(5000 * (int) Math.pow(2, tries) + mult);
                }
            }

            if (!succeeded) {
                throw ex;
            }
        }

        // enable methods in rest api
        List<String> resourceMethods = createMethodsIfMissing(restId, rootResourceId, methods);

        // create integration to link rest to lambda
        createIntegration(restId, rootResourceId, resourceMethods, lambdaArn, invokeLambdaRoleArn);

        // create deployment
        int rndInt = new Random().nextInt(99000) + 1000;
        String restUrl = createDeployment(String.valueOf(rndInt), restId);

        return new RestApiInfo(restUrl, restId, rootResourceId, resourceMethods);
    }

    public void createIntegration(String restId, String rootResourceId,
            List<String> resourceMethods, String lambdaArn, String invokeLambdaRoleArn) {
        String cmd =
                "aws apigateway put-integration --region eu-central-1 --rest-api-id %s --resource-id %s "
                        + "--http-method %s --type AWS_PROXY --integration-http-method POST "
                        + "--uri arn:aws:apigateway:eu-central-1:lambda:path/2015-03-31/functions/%s/invocations "
                        + "--credentials %s";

        for (String method : resourceMethods) {
            String filledCmd = String.format(cmd, restId, rootResourceId, method, lambdaArn,
                    invokeLambdaRoleArn);

            Process p;
            int cmdResult;
            try {
                ProcessBuilder pb = new ProcessBuilder("cmd", "/c", filledCmd);
                pb.redirectOutput(
                        new File(Constants.LOGS_DIR + "restintegration-" + restId + ".txt"));

                p = pb.start();
                cmdResult = p.waitFor();
                if (cmdResult != 0) {
                    throw new Error("Maven archetype generation for azure-functions failed.");
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(String.format("Failed to run cmd for api '%s'", restId));
            }
        }
    }

    private List<String> createMethodsIfMissing(String restId, String resourceId,
            List<HttpMethod> methods) throws InterruptedException {
        int tries = 1;
        TooManyRequestsException ex = null;
        while (tries < 10) {
            try {

                // get methods defined for root resource in rest api
                Set<String> existingMethods = client.getResource(
                        GetResourceRequest.builder().restApiId(restId).resourceId(resourceId).build())
                        .resourceMethods().keySet();

                // create method if missing
                // TODO remove unnecessary methods
                if (!existingMethods.contains("ANY")) {
                    List<String> resourceMethods = new ArrayList<>();

                    for (HttpMethod method : methods) {
                        if (!existingMethods.contains(method.toString())) {
                            
                                    client.putMethod(PutMethodRequest.builder().restApiId(restId)
                                            .resourceId(resourceId).httpMethod(method.toString())
                                            .authorizationType("NONE").build());
                                
                        }

                        resourceMethods.add(method.toString());
                    }

                    return resourceMethods;
                } else {
                    return List.of("ANY");
                }

        } catch (TooManyRequestsException e) {
                LOG.warn("Too Many Requests for putMethod, attempt " + tries);
                ex = e;
                tries++;

                int mult = new Random().nextInt(5000);
                Thread.sleep(5000 * (int) Math.pow(2, tries) + mult);
            }
        }

        throw ex;

    }

    // TODO max number of stages exception
    private String createDeployment(String stageName, String restId) throws InterruptedException {
        CreateDeploymentResponse response = null;

        int tries = 1;
        boolean succeeded = false;
        TooManyRequestsException ex = null;
        while (tries < 10 && !succeeded) {
            try {
                response = client.createDeployment(CreateDeploymentRequest.builder()
                        .restApiId(restId).stageName(stageName).build());
                succeeded = true;
            } catch (TooManyRequestsException e) {
                LOG.warn("Too Many Requests for createDeployment, attempt " + tries);
                ex = e;
                tries++;

                int mult = new Random().nextInt(5000);
                Thread.sleep(5000 * (int) Math.pow(2, tries) + mult);
            }
        }

        if (!succeeded) {
            throw ex;
        }

        GetDeploymentsResponse deplResp = null;

        tries = 1;
        succeeded = false;
        ex = null;

        while (tries < 10 && !succeeded) {
            try {
                // delete old deployments and stages
                deplResp = client
                        .getDeployments(GetDeploymentsRequest.builder().restApiId(restId).build());
                succeeded = true;
            } catch (TooManyRequestsException e) {
                LOG.warn("Too Many Requests forgetDeployments, attempt " + tries);
                ex = e;
                tries++;

                int mult = new Random().nextInt(5000);
                Thread.sleep(5000 * (int) Math.pow(2, tries) + mult);
            }
        }

        if (!succeeded) {
            throw ex;
        }

        tries = 1;
        succeeded = false;
        ex = null;

        while (tries < 10 && !succeeded) {
            try {

                for (Deployment d : deplResp.items()) {
                    if (d.id() != response.id()) {
                        // get stages of deployment and delete them
                        client.getStages(GetStagesRequest.builder().restApiId(restId)
                                .deploymentId(d.id()).build()).item().stream().forEach(stage -> {
                                    if (!stage.stageName().equals(stageName))
                                        client.deleteStage(
                                                DeleteStageRequest.builder().restApiId(restId)
                                                        .stageName(stage.stageName()).build());
                                });
                    }
                }

                succeeded = true;
            } catch (TooManyRequestsException e) {
                LOG.warn("Too Many Requests for getStages / deleteStage, attempt " + tries);
                ex = e;
                tries++;

                int mult = new Random().nextInt(5000);
                Thread.sleep(5000 * (int) Math.pow(2, tries) + mult);
            }
        }

        if (!succeeded) {
            throw ex;
        }

        return String.format(DeploymentConstants.AWS_REST_URL_FORMAT, restId,
                DeploymentConstants.AWS_DEFAULT_REGION, stageName);
    }

    private void refreshApis(boolean forceRefresh) {
        if (apis == null || forceRefresh) {
            apis = client.getRestApis().items();
        }
    }

    public void close() {
        client.close();
    }
}