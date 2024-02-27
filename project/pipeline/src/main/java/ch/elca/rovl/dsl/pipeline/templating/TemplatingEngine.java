package ch.elca.rovl.dsl.pipeline.templating;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elca.rovl.dsl.pipeline.linking.resource.LinkedDatabase;
import ch.elca.rovl.dsl.pipeline.linking.resource.LinkedFunction;
import ch.elca.rovl.dsl.pipeline.linking.resource.LinkedQueue;
import ch.elca.rovl.dsl.pipeline.linking.resource.LinkedResource;
import ch.elca.rovl.dsl.pipeline.templating.resource.DeployableDatabase;
import ch.elca.rovl.dsl.pipeline.templating.resource.DeployableFunction;
import ch.elca.rovl.dsl.pipeline.templating.resource.DeployableQueue;
import ch.elca.rovl.dsl.pipeline.templating.resource.DeployableResource;
import ch.elca.rovl.dsl.pipeline.util.Constants;
import ch.elca.rovl.dsl.pipeline.util.IdGenerator;

/**
 * Engine that takes care for each function to generate code that can be deployed to target
 * providers.
 */
public final class TemplatingEngine {

    static final Logger LOG = LoggerFactory.getLogger("Templates gen");

    final String templatesDir = "src/main/resources/vtemplates";
    final String outputDir = Constants.GENERATED_DIR + "templating/functions/";
    final String packageName = "ch.elca.rovl";

    final TemplatingHelperFactory helperFactory;
    final Map<String, LinkedResource> linkedResouces;
    final ResourceNameMemory resourceMemory;
    final Random rng;

    /**
     * TemplatingEngine constructor.
     * 
     * @param linkedResources list of resources from the previous pipeline step
     * @throws IOException
     */
    public TemplatingEngine(Map<String, LinkedResource> linkedResources) throws IOException {
        this.linkedResouces = linkedResources;
        this.resourceMemory = new ResourceNameMemory();
        this.helperFactory = new TemplatingHelperFactory(outputDir, packageName, resourceMemory);
        this.rng = new Random();

        // create generated dir
        File out = new File(outputDir);
        if (out.exists()) {
            // clean
            FileUtils.cleanDirectory(out);
        } else {
            // create
            FileUtils.forceMkdir(out);
        }
    }

    /**
     * Runs the engine. For each function it generates a project directory that can be built and
     * deployed to the function target provider. It extend the pipeline resource objects with the
     * required information needed for deployment.
     * 
     * @return list of resources for the next pipeline step
     * @throws IOException
     * @throws InterruptedException
     */
    public List<DeployableResource> run() throws IOException, InterruptedException {
        List<LinkedFunction> functions = new ArrayList<>();
        List<LinkedQueue> queues = new ArrayList<>();
        List<LinkedDatabase> databases = new ArrayList<>();

        // separate resources by type
        for (LinkedResource lr : linkedResouces.values()) {
            if (lr instanceof LinkedFunction)
                functions.add((LinkedFunction) lr);
            else if (lr instanceof LinkedQueue)
                queues.add((LinkedQueue) lr);
            else if (lr instanceof LinkedDatabase)
                databases.add((LinkedDatabase) lr);
            else
                throw new IllegalArgumentException("Type of linked resource is not supported.");
        }

        // run templating process on the resources
        List<DeployableResource> deployableResources = new ArrayList<>();
        deployableResources.addAll(processDatabases(databases));
        deployableResources.addAll(processQueues(queues));
        deployableResources.addAll(processFunctions(functions));

        return deployableResources;
    }

    /**
     * Extends the database pipeline objects with the information needed for deployment.
     * 
     * @param databases list of databases
     * @return list of databases extended with the required information for deployment
     */
    private List<DeployableDatabase> processDatabases(List<LinkedDatabase> databases) {
        // convert linked database into deployable database, set cloud name for db
        return databases.stream().map(ldb -> {
            DeployableDatabase ddb = new DeployableDatabase(ldb);
            String cloudName = resourceMemory.get(ldb.getName());
            if (cloudName == null) {
                cloudName = ldb.getName() + "-" + IdGenerator.get().generate();
                try {
                    resourceMemory.addNewMemory(ldb.getName(), cloudName);
                } catch (IOException e) {
                    LOG.info("Failed to add database cloud name to memory");
                    e.printStackTrace();
                }
            }
            ddb.setCloudName(cloudName);
            return ddb;
        }).collect(Collectors.toList());
    }

    /**
     * Extends the queue pipeline objects with the information needed for deployment.
     * 
     * @param queues list of queue
     * @return list of queues extended with the required information for deployment
     */
    private List<DeployableQueue> processQueues(List<LinkedQueue> queues) {
        // convert linked queue into deployable queue
        return queues.stream().map(lq -> {
            DeployableQueue dq = new DeployableQueue(lq);
            String cloudName = resourceMemory.get(lq.getName());
            if (cloudName == null) {
                cloudName = lq.getName() + "-" + IdGenerator.get().generate();
                try {
                    resourceMemory.addNewMemory(lq.getName(), cloudName);
                } catch (IOException e) {
                    LOG.info("Failed to add queue cloud name to memory");
                    e.printStackTrace();
                }
            }
            dq.setCloudName(cloudName);
            return dq;
        }).collect(Collectors.toList());
    }

    /**
     * For each function generates a new project that can be built and deployed to the target
     * provider, and then extends the function pipeline object with the information eneded for
     * deployment.
     * 
     * @param functions list of functions
     * @return list of functions extended with the required information for deployment
     * @throws IOException
     * @throws InterruptedException
     */
    private List<DeployableFunction> processFunctions(List<LinkedFunction> functions)
            throws IOException, InterruptedException {
        List<DeployableFunction> deployableFunctions = new ArrayList<>();

        for (LinkedFunction fn : functions) {
            //LOG.info(String.format("Generating function %s...", fn.toString()));
            // generate project for the target provider
            DeployableFunction dfn = helperFactory.getHelper(fn.getProvider()).generateFunction(fn);
            deployableFunctions.add(dfn);

            // if glue is required, generate the additional function project to be deployed on the
            // same provider as the input queue
            if (fn.requiresGlue()) {
                LOG.info(String.format("Function requires glue..."));
                deployableFunctions.add(helperFactory.getHelper(fn.getInputQueue().getProvider())
                        .generateGlue(dfn));
            }
        }

        return deployableFunctions;
    }

}
