package ch.elca.rovl.dsl.pipeline.deployment;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elca.rovl.dsl.pipeline.deployment.helper.DeploymentHelper;
import ch.elca.rovl.dsl.pipeline.deployment.resource.DeployedDatabase;
import ch.elca.rovl.dsl.pipeline.deployment.resource.DeployedFunction;
import ch.elca.rovl.dsl.pipeline.deployment.resource.DeployedQueue;
import ch.elca.rovl.dsl.pipeline.deployment.resource.DeployedResource;
import ch.elca.rovl.dsl.pipeline.templating.resource.DeployableDatabase;
import ch.elca.rovl.dsl.pipeline.templating.resource.DeployableFunction;
import ch.elca.rovl.dsl.pipeline.templating.resource.DeployableQueue;
import ch.elca.rovl.dsl.pipeline.templating.resource.DeployableResource;
import ch.elca.rovl.dsl.pipeline.util.RequiredData;
import ch.elca.rovl.dsl.pipeline.util.ResourceType;

/**
 * Engine that takes care of deploying resources to their target providers.
 */
public final class DeploymentEngine {

    static final Logger LOG = LoggerFactory.getLogger("Deployment");

    final List<DeployableQueue> queues;
    final List<DeployableFunction> functions;
    final List<DeployableDatabase> databases;
    final Map<String, DeployedResource> deployedMap;
    final List<DeployedFunction> needsData;
    final DeploymentHelperFactory helperFactory;

    /**
     * Constructor.
     * 
     * @param deployableResources list of pipeline resources to be deployed
     * @throws IOException
     */
    public DeploymentEngine(List<DeployableResource> deployableResources) throws IOException {
        this.queues = new ArrayList<>();
        this.functions = new ArrayList<>();
        this.databases = new ArrayList<>();
        this.helperFactory = new DeploymentHelperFactory();
        this.deployedMap = new HashMap<>();
        this.needsData = new ArrayList<>();

        // sort resources
        for (DeployableResource dr : deployableResources) {
            if (dr instanceof DeployableFunction)
                functions.add((DeployableFunction) dr);
            else if (dr instanceof DeployableQueue)
                queues.add((DeployableQueue) dr);
            else if (dr instanceof DeployableDatabase)
                databases.add((DeployableDatabase) dr);
            else
                throw new IllegalArgumentException(
                        "Type of deployable resource is not recognized.");
        }
    }

    // TODO deploy databases, rest APIs and queue in parallel
    /**
     * Provisions servers and deploys databases to their target providers. Retrieves access perms
     * for each database.
     * 
     * @throws IOException
     * @throws URISyntaxException
     * @throws InterruptedException
     */
    public void deployDatabases() throws IOException, URISyntaxException, InterruptedException {
        for (DeployableDatabase db : databases) {
            LOG.info(String.format("Deploying database %s...", db.toString()));
            DeployedDatabase ddb = helperFactory.getHelper(db.getProvider()).deploy(db);
            deployedMap.put(db.getName(), ddb);
        }
        LOG.info("Databases succesfully deployed.");
    }

    /**
     * Deploys queues to their target providers. Retrieves send and listen perms for each queue.
     * 
     * @throws IOException
     * @throws URISyntaxException
     */
    public void deployQueues() throws IOException, URISyntaxException {
        for (DeployableQueue q : queues) {
            LOG.info(String.format("Deploying queue %s...", q.toString()));
            DeployedQueue dq = helperFactory.getHelper(q.getProvider()).deploy(q);
            deployedMap.put(q.getName(), dq);
        }
        LOG.info("Queues succesfully deployed.");
    }

    /**
     * Deploys functions to their target providers. Retrieves functions url of http-triggerable
     * functions and generates JWT tokens for authorization of infra-provider communication.
     * 
     * @throws InterruptedException
     * @throws IOException
     * @throws URISyntaxException
     */
    public void deployFunctions() throws InterruptedException, IOException, URISyntaxException {
        List<DeployedFunction> deployedFunctions = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newCachedThreadPool();

        Map<Future<?>, DeployableFunction> deploymentTasks = new HashMap<>();

        for (DeployableFunction fn : functions) {
            DeploymentHelper dh = helperFactory.getHelper(fn.getProvider());

            deploymentTasks.put(executor.submit(() -> {
                DeployedFunction dfn;
                try {
                    dfn = dh.deploy(fn);
                } catch (InterruptedException e) {
                    LOG.error(String.format("Deployment of function '%s' failed!", fn.getName()));
                    e.printStackTrace();
                    return;
                }
                deployedFunctions.add(dfn);
            }), fn);
        }

        boolean deploymentHasFailed = false;

        // wait until all tasks are done
        for (Future<?> res : deploymentTasks.keySet()) {
            try {
                res.get();
            } catch (ExecutionException e) {
                deploymentHasFailed = true;
                LOG.error(String.format("Deployment failed for function %s",
                        deploymentTasks.get(res).getName()), e);
            }
        }

        executor.shutdown();

        if (deploymentHasFailed) {
            throw new IllegalStateException("Deployment of some functions failed.");
        }

        for (DeployedFunction dfn : deployedFunctions) {
            deployedMap.put(dfn.getName(), dfn);

            if (dfn.requiresData()) {
                needsData.add(dfn);
            }
        }

        LOG.info("Functions succesfully deployed.");
    }

    /**
     * For all resources that needs additional configuration, it gathers the necessary data and
     * update the cloud resource configuration.
     * 
     * @return map of deployed and configured resources
     * @throws InterruptedException
     * @throws IOException
     * @throws URISyntaxException
     */
    public Map<ResourceType, List<DeployedResource>> configureDeployedResources()
            throws InterruptedException, IOException, URISyntaxException {
        for (DeployedFunction fn : needsData) {
            UpdateInfo updateInfo = new UpdateInfo();

            for (RequiredData rd : fn.getRequiredData()) {
                updateInfo.addInfo(rd.getType(), deployedMap.get(rd.getResourceName()));
            }

            if (updateInfo.hasEntries()) {
                helperFactory.getHelper(fn.getProvider()).configureFunction(fn, updateInfo);
            }

        }

        helperFactory.close();

        Map<ResourceType, List<DeployedResource>> finalResources = new HashMap<>();
        for (ResourceType type : ResourceType.values()) {
            finalResources.put(type, new ArrayList<>());
        }

        for (DeployedResource dr : deployedMap.values()) {
            if (dr instanceof DeployedQueue) {
                finalResources.get(ResourceType.QUEUE).add(dr);
            } else if (dr instanceof DeployedDatabase) {
                finalResources.get(ResourceType.DATABASE).add(dr);
            } else if (dr instanceof DeployedFunction) {
                finalResources.get(ResourceType.FUNCTION).add(dr);
            } else {
                throw new IllegalStateException("Unexpected deployed resource type for: " + dr.getName());
            }
        }

        return finalResources;
    }


}
