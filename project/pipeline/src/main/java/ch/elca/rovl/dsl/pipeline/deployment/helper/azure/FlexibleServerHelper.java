package ch.elca.rovl.dsl.pipeline.deployment.helper.azure;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.exception.ManagementException;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.postgresqlflexibleserver.PostgreSqlManager;
import com.azure.resourcemanager.postgresqlflexibleserver.models.ActiveDirectoryAuthEnum;
import com.azure.resourcemanager.postgresqlflexibleserver.models.AuthConfig;
import com.azure.resourcemanager.postgresqlflexibleserver.models.Backup;
import com.azure.resourcemanager.postgresqlflexibleserver.models.CheckNameAvailabilityRequest;
import com.azure.resourcemanager.postgresqlflexibleserver.models.CreateMode;
import com.azure.resourcemanager.postgresqlflexibleserver.models.Database;
import com.azure.resourcemanager.postgresqlflexibleserver.models.GeoRedundantBackupEnum;
import com.azure.resourcemanager.postgresqlflexibleserver.models.NameAvailability;
import com.azure.resourcemanager.postgresqlflexibleserver.models.PasswordAuthEnum;
import com.azure.resourcemanager.postgresqlflexibleserver.models.Server;
import com.azure.resourcemanager.postgresqlflexibleserver.models.ServerVersion;
import com.azure.resourcemanager.postgresqlflexibleserver.models.Sku;
import com.azure.resourcemanager.postgresqlflexibleserver.models.SkuTier;
import com.azure.resourcemanager.postgresqlflexibleserver.models.Storage;
import com.azure.resourcemanager.postgresqlflexibleserver.models.Server.DefinitionStages.WithCreate;
import com.azure.resourcemanager.resources.models.Subscription;

import ch.elca.rovl.dsl.pipeline.deployment.DeploymentConstants;
import ch.elca.rovl.dsl.pipeline.deployment.accessinfo.database.DatabaseAccess;
import ch.elca.rovl.dsl.pipeline.templating.resource.DeployableDatabase;
import ch.elca.rovl.dsl.resource.database.DatabaseEngine;
import ch.elca.rovl.dsl.resource.database.WorkloadType;
import ch.elca.rovl.dsl.resource.database.Database.DatabaseConfigType;

public class FlexibleServerHelper {
    static final Logger LOG = LoggerFactory.getLogger("Deployment (Azure)");

    final int dbMinSizeGB = 32;
    final int dbMaxSizeGB = 32000;

    final PostgreSqlManager client;

    public FlexibleServerHelper(AzureResourceManager manager) {
        // retrieve subscription from resource manager
        Subscription s = manager.subscriptions().getById(manager.subscriptionId());
        AzureProfile profile = new AzureProfile(s.innerModel().tenantId(),
            s.subscriptionId(), AzureEnvironment.AZURE);

        // init client
        client = PostgreSqlManager
            .authenticate(new DefaultAzureCredentialBuilder().build(), profile);
    }

    /**
     * Creates a server for the given database, or retrieves it if already existing.
     * @param db
     */
    public Server getOrCreateServer(DeployableDatabase db) {
        if (db.getEngine() != DatabaseEngine.POSTRGESQL)
            throw new UnsupportedOperationException("Database engine not supported: " + db.getDatabase().getEngine());

        // check name availability
        NameAvailability response = client.checkNameAvailabilities()
            .execute(new CheckNameAvailabilityRequest()
                .withName(db.getCloudName())
                .withType("Microsoft.DBforPostgreSQL/flexibleServers"));

        // if server exists, return it
        // NOTE no check whether config matches
        if (!response.nameAvailable()) {
            Server server = client.servers().getByResourceGroup(
                DeploymentConstants.AZURE_DEFAULT_RESOURCE_GROUP,
                db.getCloudName());

            // create firewall rule if it doesn't exist
            try {    
                client.firewallRules().get(DeploymentConstants.AZURE_DEFAULT_RESOURCE_GROUP,
                        server.name(), "openaccess-" + db.getCloudName());
            } catch(ManagementException e) {
                client.firewallRules().define("openaccess-" + db.getCloudName())
                    .withExistingFlexibleServer(DeploymentConstants.AZURE_DEFAULT_RESOURCE_GROUP, server.name())
                    .withStartIpAddress("0.0.0.0")
                    .withEndIpAddress("255.255.255.255")
                    .create();
            }

            LOG.info(String.format("Database '%s' already exists. Skipping deployment.", db.getName()));

            return server;
        } 
        

        Map<DatabaseConfigType,Object> config = db.getConfig();

        // default config values
        WithCreate serverCreate = client
            .servers()
            .define(db.getCloudName())
            .withRegion(DeploymentConstants.AZURE_DEFAULT_REGION)
            .withExistingResourceGroup(DeploymentConstants.AZURE_DEFAULT_RESOURCE_GROUP)
            .withVersion(ServerVersion.ONE_THREE)
            .withAuthConfig(new AuthConfig()
                .withPasswordAuth(PasswordAuthEnum.ENABLED)
                .withActiveDirectoryAuth(ActiveDirectoryAuthEnum.DISABLED))
            .withBackup(new Backup().withBackupRetentionDays(7).withGeoRedundantBackup(GeoRedundantBackupEnum.DISABLED))
            .withAdministratorLogin(db.getDatabase().getUsername())
            .withAdministratorLoginPassword(db.getDatabase().getPassword())
            .withCreateMode(CreateMode.CREATE);
            
        
        // workload config
        Object workloadConfig  = config.get(DatabaseConfigType.WORKLOAD_TYPE);
        if (workloadConfig == null) {
            serverCreate = serverCreate.withSku(
                new Sku().withName("Standard_B1ms").withTier(SkuTier.BURSTABLE));
        } else {
            WorkloadType type = (WorkloadType) workloadConfig;
            switch(type) {
                case DEV:
                    serverCreate = serverCreate.withSku(
                        new Sku().withName("Standard_B1ms").withTier(SkuTier.BURSTABLE));
                    break;
                case PROD_SMALL:
                case PROD_MEDIUM:
                    serverCreate = serverCreate.withSku(
                        new Sku().withName("Standard_D4ds_v5").withTier(SkuTier.GENERAL_PURPOSE));
                    break;
                case PROD_LARGE:
                    serverCreate = serverCreate.withSku(
                        new Sku().withName("Standard_E4ds_v5").withTier(SkuTier.MEMORY_OPTIMIZED));
                    break;
                default:
                    throw new IllegalArgumentException("Database workload type not supported: " + type);
            }
            serverCreate = serverCreate.withSku(
                new Sku().withName("Standard_B1ms").withTier(SkuTier.BURSTABLE));
        }

        // size config
        Object sizeConfig = config.get(DatabaseConfigType.SIZE_GB);
        if (sizeConfig == null) {
            serverCreate = serverCreate.withStorage(new Storage().withStorageSizeGB(dbMinSizeGB));
        } else {
            int sizeGB = (int) sizeConfig;
            if (sizeGB == -1) {
                serverCreate = serverCreate.withStorage(new Storage().withStorageSizeGB(dbMaxSizeGB));
            } else if (sizeGB == 0) {
                serverCreate = serverCreate.withStorage(new Storage().withStorageSizeGB(dbMinSizeGB));
            } else {
                serverCreate = serverCreate.withStorage(new Storage().withStorageSizeGB(sizeGB));
            }
        }

        Server server = serverCreate.create();

        // open access for db
        client.firewallRules().define("openaccess-" + db.getCloudName())
            .withExistingFlexibleServer(DeploymentConstants.AZURE_DEFAULT_RESOURCE_GROUP, server.name())
            .withStartIpAddress("0.0.0.0")
            .withEndIpAddress("255.255.255.255")
            .create();
            
        return server;
    }

    /**
     * Creates a database with the given name in the given server, or retrieves the 
     * database if it already exists.
     * @param server
     * @param dbName
     * @return the database
     */
    public Database getOrCreateDatabase(Server server, String dbName) {
        try {
            return client.databases().get(
                DeploymentConstants.AZURE_DEFAULT_RESOURCE_GROUP, server.name(), dbName);
        } catch (ManagementException e) {
            return client.databases()
                .define(dbName)
                .withExistingFlexibleServer(server.resourceGroupName(), server.name())
                .create();
        }
    }

    // TODO get as argument details of schema
    public void createSchema(Database db) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    // TODO create various roles
    public DatabaseAccess getRoles(Database db) {
        throw new UnsupportedOperationException("Not implemented.");
    }
}
