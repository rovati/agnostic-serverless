package ch.elca.rovl.dsl.pipeline.deployment.helper;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.postgresqlflexibleserver.models.Server;
import com.azure.resourcemanager.resources.models.ResourceGroups;
import com.azure.resourcemanager.servicebus.models.ServiceBusNamespace;
import ch.elca.rovl.dsl.pipeline.deployment.DeploymentConstants;
import ch.elca.rovl.dsl.pipeline.deployment.DeploymentEngine;
import ch.elca.rovl.dsl.pipeline.deployment.UpdateInfo;
import ch.elca.rovl.dsl.pipeline.deployment.accessinfo.database.DatabaseAccess;
import ch.elca.rovl.dsl.pipeline.deployment.accessinfo.database.DatabaseRole;
import ch.elca.rovl.dsl.pipeline.deployment.accessinfo.function.FunctionAccess;
import ch.elca.rovl.dsl.pipeline.deployment.accessinfo.queue.AwsQueueAccess;
import ch.elca.rovl.dsl.pipeline.deployment.accessinfo.queue.AzureQueueAccess;
import ch.elca.rovl.dsl.pipeline.deployment.helper.azure.FlexibleServerHelper;
import ch.elca.rovl.dsl.pipeline.deployment.helper.azure.FunctionAppHelper;
import ch.elca.rovl.dsl.pipeline.deployment.helper.azure.ServiceBusHelper;
import ch.elca.rovl.dsl.pipeline.deployment.helper.azure.model.ServiceBusQueueKeys;
import ch.elca.rovl.dsl.pipeline.deployment.resource.DeployedDatabase;
import ch.elca.rovl.dsl.pipeline.deployment.resource.DeployedFunction;
import ch.elca.rovl.dsl.pipeline.deployment.resource.DeployedQueue;
import ch.elca.rovl.dsl.pipeline.deployment.resource.DeployedResource;
import ch.elca.rovl.dsl.pipeline.templating.resource.DeployableDatabase;
import ch.elca.rovl.dsl.pipeline.templating.resource.DeployableFunction;
import ch.elca.rovl.dsl.pipeline.templating.resource.DeployableQueue;
import ch.elca.rovl.dsl.pipeline.util.Provider;
import ch.elca.rovl.dsl.pipeline.util.RequiredData;
import ch.elca.rovl.dsl.pipeline.util.RequiredData.Type;
import io.jsonwebtoken.Jwts;

/**
 * Helper class of {@link DeploymentEngine DeploymentEngine} to deploy resources on Azure.
 */
public class AzureDeploymentHelper implements DeploymentHelper {

    static final Logger LOG = LoggerFactory.getLogger("Deployment (Azure)");

    final AzureResourceManager resManager;
    final ServiceBusHelper serviceBusHelper;
    final FunctionAppHelper functionAppHelper;
    final FlexibleServerHelper flexServerHelper;

    /**
     * Constructor. It inizalizes SDK clients and creates the root resource group if it doesn't
     * exist already.
     */
    public AzureDeploymentHelper() {
        AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);
        TokenCredential defaultCredential = new DefaultAzureCredentialBuilder().build();

        // resource manager
        resManager = AzureResourceManager.authenticate(defaultCredential, profile)
                .withDefaultSubscription();
        serviceBusHelper = new ServiceBusHelper(resManager.serviceBusNamespaces());
        functionAppHelper = new FunctionAppHelper(resManager.functionApps());
        flexServerHelper = new FlexibleServerHelper(resManager);

        // resource group creation
        ResourceGroups rgs = resManager.resourceGroups();
        if (!rgs.contain(DeploymentConstants.AZURE_DEFAULT_RESOURCE_GROUP)) {
            resManager.resourceGroups().define(DeploymentConstants.AZURE_DEFAULT_RESOURCE_GROUP)
                    .withRegion(DeploymentConstants.AZURE_DEFAULT_REGION).create();
            LOG.info("Resource group is being created.");
        } else {
            // LOG.info("Resource group already exists.");
        }
    }

    /**
     * Deploys a database on Azure using the SDK. Returns the database pipeline object containing
     * access info for the database.
     * 
     * @param database database to deploy
     * @return deployed database object
     */
    @Override
    public DeployedDatabase deploy(DeployableDatabase database) {
        //LOG.info(String.format("Deploying database '%s' on Azure...", database.getName()));
        Server dbServer = flexServerHelper.getOrCreateServer(database);
        flexServerHelper.getOrCreateDatabase(dbServer, database.getName());
        // NOTE application level, this is left to user to do
        // TODO for the time being give superuser to all functions that interact with db
        // DatabaseAccess roles = flexServerHelper.getRoles(db);
        DatabaseAccess roles = new DatabaseAccess();
        DatabaseRole superuser = new DatabaseRole(database.getUsername(), database.getPassword());
        roles.setSuperUser(superuser);

        DeployedDatabase ddb = new DeployedDatabase(database, dbServer.fullyQualifiedDomainName());
        ddb.setAccessInfo(roles);
        return ddb;
    }

    /**
     * Deploys a queue on Azure using the SDK. Returns the queue pipeline object containing access
     * info for the queue.
     * 
     * @param queue queue to deploy
     * @return deployed queue object
     */
    @Override
    public DeployedQueue deploy(DeployableQueue queue) {
        //LOG.info(String.format("Deploying queue '%s' on Azure...", queue.getName()));

        // namespace creation
        // NOTE satisfy namespace name requirements
        String namespace = queue.getName().replace("_", "-") + "-namespace";
        // LOG.info(String.format("Creating namespace '%s'", namespace));

        ServiceBusNamespace ns;

        if (!serviceBusHelper.namespaceExists(namespace,
                DeploymentConstants.AZURE_DEFAULT_RESOURCE_GROUP)) {
            // LOG.info("Triggering creation of namespace and queue...");
            ns = serviceBusHelper.createNamespaceAndQueue(namespace, queue.getName());
        } else {
            ns = serviceBusHelper.createQueueIfMissing(namespace, queue.getName());
        }

        // LOG.info("Creating access rules...");
        ServiceBusQueueKeys keys = serviceBusHelper.createAccessKeys(ns);

        DeployedQueue deployed = new DeployedQueue(queue);
        deployed.setAccessInfo(
                new AzureQueueAccess(keys.sendConnectionString(), keys.listenConnectionString()));

        // LOG.info(String.format("Queue '%s' successfully deployed.", queue.getName()));
        return deployed;
    }

    /**
     * Deploys a function on Azure using the FunctionApp Maven plugin. Returns the function pipeline
     * object containing access info for the function.
     * 
     * @param function function to deploy
     * @return deployed function object
     */
    @Override
    public DeployedFunction deploy(DeployableFunction function) {
        //LOG.info(String.format("Deploying function '%s' on Azure...", function.getName()));

        // deply
        String generateDir = function.getGeneratedPath();
        functionAppHelper.deployFunction(generateDir, function.getName());

        DeployedFunction deployed = new DeployedFunction(function);

        // create deployed resource object and set access info
        if (!function.isGlue()) {
            FunctionAccess access = new FunctionAccess();

            if (function.getFunction().requiresGlue()) {
                String functionUrl = functionAppHelper.getFunctionUrl(function.getName(),
                        function.getCloudName());
                access.setUrl(functionUrl);
            } else {
                switch (function.getFunction().getTriggerType()) {
                    case HTTP:
                        String functionUrl = functionAppHelper.getFunctionUrl(function.getName(),
                                function.getCloudName());
                        access.setUrl(functionUrl);
                        break;
                    case QUEUE:
                        // do nothing
                        break;
                    // case TIMER:
                    // do nothing
                    // break;
                    default:
                        throw new IllegalStateException("Trigger type not supported.");
                }
            }

            // generate shared secret key to authenticate infra-provider comm
            SecretKey key = Jwts.SIG.HS256.key().build();
            access.setAuthKey(key);
            deployed.addRequiredData(new RequiredData(Type.SHARED_KEY, function.getFunction().getName()));

            deployed.setAccessInfo(access);

        } else {
            // if glue, tag it to receive secret shared key for infra-comm auth
            deployed.addRequiredData(
                    new RequiredData(Type.SHARED_KEY, function.getFunction().getName()));
        }

        deployed.addRequiredData(function.getRequiredData());
        deployed.setFunctionApp(functionAppHelper.getFunctionApp(function.getCloudName()));

        LOG.info(String.format("%s: Deployment completed.", function.getName()));

        return deployed;
    }

    /**
     * Configure a deployed function with the necessary data to access the linked resources.
     */
    @Override
    public void configureFunction(DeployedFunction function, UpdateInfo updateInfo) {
        //LOG.info(String.format("Finalizing configuration of function '%s'...", function.getName()));
        Map<String, String> settings = new HashMap<>();

        // gather configuration
        for (RequiredData.Type type : updateInfo.getTypes()) {
            List<DeployedResource> resources = updateInfo.getResourcesForType(type);
            boolean awsCredentialsSet = false;
            switch (type) {
                case QUEUE_SEND:
                    for (DeployedResource queue : resources) {
                        if (queue.getProvider() == Provider.AWS) {
                            AwsQueueAccess access = (AwsQueueAccess) queue.getAccessInfo();
                            addAwsCredentialsToSettings(settings, access, awsCredentialsSet);
                            awsCredentialsSet = true;
                            settings.put("QUEUE_URL_" + queue.getName(), access.getQueueUrl());
                        } else if (queue.getProvider() == Provider.AZURE) {
                            AzureQueueAccess access = (AzureQueueAccess) queue.getAccessInfo();
                            settings.put("CONN_STRING_" + queue.getName(), access.getSendSAS());
                        } else {
                            throw new IllegalArgumentException(String.format(
                                    "provider '%s' not yet supported", queue.getProvider()));
                        }
                    }
                    break;
                case QUEUE_LISTEN:
                    for (DeployedResource queue : resources) {
                        if (queue.getProvider() == Provider.AWS) {
                            AwsQueueAccess access = (AwsQueueAccess) queue.getAccessInfo();
                            addAwsCredentialsToSettings(settings, access, awsCredentialsSet);
                            awsCredentialsSet = true;
                        } else if (queue.getProvider() == Provider.AZURE) {
                            AzureQueueAccess access = (AzureQueueAccess) queue.getAccessInfo();
                            settings.put("CONN_STRING_" + queue.getName(), access.getReceiveSAS());
                        } else {
                            throw new IllegalArgumentException(String.format(
                                    "provider '%s' not yet supported", queue.getProvider()));
                        }
                    }
                    break;
                case QUEUE_TRIGGER:
                    // noop
                    break;
                case FUNCTION_URL:
                    for (DeployedResource fn : resources) {
                        FunctionAccess access = (FunctionAccess) fn.getAccessInfo();
                        settings.put("FUNCTION_URL_" + fn.getName(), access.getUrl());
                        String encodedKey =
                            Base64.getEncoder().encodeToString(access.getAuthKey().getEncoded());
                        settings.put("JWT_KEY_" + fn.getName(), encodedKey);
                    }
                    break;
                case DATABASE_CONNECTION:
                    // TODO cases of op needed
                    for (DeployedResource db : resources) {
                        DatabaseAccess access = (DatabaseAccess) db.getAccessInfo();
                        settings.put("PSQL_USER_" + db.getName(), access.superuser().username());
                        settings.put("PSQL_PWD_" + db.getName(), access.superuser().password());

                        String url = ((DeployedDatabase) db).getEndpoint();
                        url = "jdbc:postgresql://" + url + ":5432/" + db.getName();
                        settings.put("PSQL_URL_" + db.getName(), url);
                    }
                    break;
                case SHARED_KEY:
                    DeployedFunction dfn = (DeployedFunction) resources.get(0);
                    FunctionAccess access = (FunctionAccess) dfn.getAccessInfo();
                    String encodedKey =
                            Base64.getEncoder().encodeToString(access.getAuthKey().getEncoded());
                    settings.put("JWT_KEY_" + dfn.getName(), encodedKey);
                    break;
                default:
                    throw new IllegalArgumentException(
                            String.format("Type '%s' not supported.", type));
            }
        }

        // update configuration
        function.setFunctionApp(
                functionAppHelper.updateFunctionAppSettings(function.getFunctionApp(), settings));
        //LOG.info(String.format("Update successful", function.getName()));
    }

    /**
     * Adds to a configuration map the environment variables necessary to authenticate to AWS.
     * 
     * @param settings configuraiton amp
     * @param access aws queue access info
     * @param alreadySet whether the credentials have already been added to the configuraiton map
     */
    private void addAwsCredentialsToSettings(Map<String, String> settings, AwsQueueAccess access,
            boolean alreadySet) {
        if (!alreadySet) {
            settings.put("AWS_ACCESS_KEY_ID", access.getCredentials().accessKeyId());
            settings.put("AWS_SECRET_ACCESS_KEY", access.getCredentials().secretAccessKey());
            settings.put("AWS_SESSION_TOKEN", access.getCredentials().sessionToken());
            settings.put("AWS_REGION", DeploymentConstants.AWS_DEFAULT_REGION);
        }
    }

    @Override
    public void close() {}

}
