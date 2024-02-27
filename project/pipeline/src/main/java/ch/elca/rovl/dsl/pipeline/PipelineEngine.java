package ch.elca.rovl.dsl.pipeline;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.elca.rovl.dsl.PlatformResourcesDefinition;
import ch.elca.rovl.dsl.pipeline.debugging.DebuggingEngine;
import ch.elca.rovl.dsl.pipeline.deployment.DeploymentEngine;
import ch.elca.rovl.dsl.pipeline.deployment.accessinfo.function.FunctionAccess;
import ch.elca.rovl.dsl.pipeline.deployment.resource.DeployedFunction;
import ch.elca.rovl.dsl.pipeline.deployment.resource.DeployedResource;
import ch.elca.rovl.dsl.pipeline.infraparsing.InfraParsingEngine;
import ch.elca.rovl.dsl.pipeline.infraparsing.resource.DslDatabase;
import ch.elca.rovl.dsl.pipeline.infraparsing.resource.DslFunction;
import ch.elca.rovl.dsl.pipeline.infraparsing.resource.DslQueue;
import ch.elca.rovl.dsl.pipeline.linking.LinkerEngine;
import ch.elca.rovl.dsl.pipeline.linking.resource.LinkedResource;
import ch.elca.rovl.dsl.pipeline.templating.TemplatingEngine;
import ch.elca.rovl.dsl.pipeline.templating.resource.DeployableResource;
import ch.elca.rovl.dsl.pipeline.util.Constants;
import ch.elca.rovl.dsl.pipeline.util.ResourceType;
import ch.elca.rovl.dsl.pipeline.util.RunTarget;

/**
 * Pipeline of the framework. Its use is to digest platform resources defined by
 * the user plus computing logic in the form of function projects in order to
 * then provision necessary resources on target providers and deploy the code to
 * achieve a working serverless application.
 */
public class PipelineEngine {

    private static final Logger LOG = LoggerFactory.getLogger("Pipeline");

    PlatformResourcesDefinition layout;

    /**
     * Initializes the pipeline engine, given a user-defined infrastructure layout
     * 
     * @param layout infrastructure description
     */
    public PipelineEngine(PlatformResourcesDefinition layout) {
        this.layout = layout;
    }

    /**
     * Runs the pipeline.
     * <p>
     * It first parses the requested cloud resources from the user infrastructure
     * layout. It then
     * links each resource to its chosen target provider, and every function to its
     * project
     * directory. The linking phase terminates with the deduction of network links
     * between the
     * resources. The pipeline then generates the code to be deployed to cloud
     * functions. Finally,
     * it deploys all necessary resources to the target providers.
     */
    public void run(String... args) {
        RunTarget mode;
        Instant startTime = Instant.now();

        if (args.length > 0) {
            if (args.length > 1) {
                LOG.error("Cannot provide more than one parameter!");
                return;
            }

            if (args[0].equals("-debug")) {
                LOG.info("Exec for debug config generation.");
                mode = RunTarget.DEBUG;
            } else if (args[0].equals("-deploy")) {
                LOG.info("Exec for deployment.");
                mode = RunTarget.DEPLOY;
            } else {
                LOG.error(String.format("Parameter '%s' is not recognized.", args[0]));
                return;
            }
        } else {
            LOG.info("No argument was provided, running app validation...");
            mode = RunTarget.VALIDATE;
        }

        LOG.info("Cleaning up output directory...");

        // cleanup generated folder
        runActionAndCatch(() -> {
            try {
                FileUtils.deleteDirectory(new File(Constants.GENERATED_DIR));
            } catch(IOException e) {
                // wait a moment or quarkus dev and retry
                Thread.sleep(1000);
                FileUtils.deleteDirectory(new File(Constants.GENERATED_DIR));
            }
            return null;
        }, Map.of(IOException.class, "Failed to clean up generated folder!"));

        /* INFRA PARSING STEP */

        LOG.info("Reading user defined infrastructure...");
        InfraParsingEngine ipe = new InfraParsingEngine(layout);

        LOG.info("Verifying system layout...");
        ipe.parseResources();

        printDslResources(ipe.getFunctions().values(), ipe.getQueues().values(),
                ipe.getDatabases().values());

        /* LINKING STEP */

        LOG.info("Parsing chosen providers for resources...");
        LinkerEngine le = runActionAndCatch(
                () -> new LinkerEngine(Constants.PROVIDERS_FILENAME, ipe.getFunctions(),
                        ipe.getQueues(), ipe.getDatabases()),
                Map.of(URISyntaxException.class, "Failed to create linker engine."));

        runActionAndCatch(() -> {
            le.matchToProvider(mode == RunTarget.DEBUG);
            return null;
        }, Map.of(IllegalStateException.class, "Could not find provider for some resources!",
                IOException.class, "Failed to load providers file!"));

        LOG.info("Deducing links from computing logic...");
        runActionAndCatch(() -> {
            le.deduceLinks();
            return null;
        }, Map.of(IOException.class, "Failed to deduce links!"));

        LOG.info("Linking resources together...");
        Map<String, LinkedResource> linkedResources = le.linkResources();

        // print resources
        LOG.info(String.format("Resources:", linkedResources.values()));
        for (LinkedResource lr : linkedResources.values()) {
            LOG.info(String.format("\t%s", lr));
        }

        /* DEBUGGING STEP */

        if (mode == RunTarget.DEBUG) {
            DebuggingEngine de = new DebuggingEngine(linkedResources);
            de.choosePortForDatabases();
            LOG.info("Starting Dev Services for databases...");
            de.startDummyDatabases();
            LOG.info("Writing config to function projects...");
            runActionAndCatch(() -> {
                de.writeDebugConfig();
                LOG.info("Written configuration to functions.");
                return null;
            }, Map.of(IOException.class, "Failed to write debugging configuration!"));

            printElapsedTime(startTime, "Generation completed.");

            return;
        }

        /* TEMPLATING STEP */

        LOG.info("Generating templates...");
        TemplatingEngine te = runActionAndCatch(() -> new TemplatingEngine(linkedResources),
                Map.of(IOException.class, "Failed to create tempalting output dir"));

        List<DeployableResource> deployableResources = runActionAndCatch(() -> te.run(),
                Map.of(IOException.class, "Failed to generate templates",
                        InterruptedException.class, "Failed to generate templates"));

        LOG.info("Resources ready for deployment:");
        for (DeployableResource dr : deployableResources) {
            LOG.info(String.format("\t%s", dr));
        }

        if (mode == RunTarget.VALIDATE) {
            LOG.info("Skipping deployment phase.");
            printElapsedTime(startTime, "Verification and projects generation completed.");
            return;
        }

        /* DEPLOYMENT STEP */

        LOG.info("Starting deployment phase...");
        DeploymentEngine de = runActionAndCatch(() -> new DeploymentEngine(deployableResources),
                Map.of(IOException.class, "Failed to parse memory file"));

        runActionAndCatch(() -> {
            de.deployDatabases();
            return null;
        }, Map.of(IOException.class, "Failed to deploy databases!", URISyntaxException.class,
                "Failed to deploy databases!", InterruptedException.class,
                "Failed to deploy databases!"));

        runActionAndCatch(() -> {
            de.deployQueues();
            return null;
        }, Map.of(IOException.class, "Failed to deploy queues!", URISyntaxException.class,
                "Failed to deploy queues!"));

        runActionAndCatch(() -> {
            de.deployFunctions();
            return null;
        }, Map.of(InterruptedException.class, "Failed to deploy functions!", IOException.class,
                "Failed to deploy functions!", URISyntaxException.class,
                "Failed to deploy functions!"));

        LOG.info("Configuring resources...");
        Map<ResourceType, List<DeployedResource>> finalResources = runActionAndCatch(
                () -> de.configureDeployedResources(),
                Map.of(InterruptedException.class, "Failed to configure deployed resources!",
                        IOException.class, "Failed to configure deployed resources!",
                        URISyntaxException.class, "Failed to configure deployed resources!"));

        LOG.info("");
        LOG.info("###");
        printElapsedTime(startTime, "Application successfully deployed.");

        LOG.info("Resources:");
        for (ResourceType type : finalResources.keySet()) {
            List<DeployedResource> resources = finalResources.get(type);
            if (!resources.isEmpty()) {
                LOG.info("\t# " + type);
                for (DeployedResource dr : resources) {
                    LOG.info(String.format("\t\t- %s (cloud resource name: %s)", dr.getName(),
                            dr.getCloudName()));
                    if (type == ResourceType.FUNCTION) {
                        DeployedFunction dfn = (DeployedFunction) dr;
                        if (dfn.getFunction().getFunction().getTrigger() != null) {
                            FunctionAccess fnAccess = ((FunctionAccess) dr.getAccessInfo());
                            if (fnAccess != null && fnAccess.getUrl() != null) {
                                LOG.info(String.format("\t\t\turl: %s", fnAccess.getUrl()));
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Runs the callable, catches the given exceptions logging the corresponding
     * given error.
     * 
     * @param <T>
     * @param c
     * @param exceptions
     * @return
     */
    private static <T extends Object> T runActionAndCatch(Callable<T> c,
            Map<Class<? extends Exception>, String> exceptions) {
        try {
            return c.call();
        } catch (Exception e) {
            for (Class<? extends Exception> mappedE : exceptions.keySet()) {
                if (mappedE.isInstance(e)) {
                    LOG.error(exceptions.get(mappedE));
                    throw new RuntimeException(e);
                }
            }

            LOG.error("An unexpected error occurred!");
            throw new RuntimeException(e);
        }
    }

    private static void printDslResources(Collection<DslFunction> fns, Collection<DslQueue> qs,
            Collection<DslDatabase> dbs) {
        LOG.info("Layout resources:");
        for (DslFunction fn : fns) {
            LOG.info("\t" + fn.toString());
        }
        for (DslQueue q : qs) {
            LOG.info("\t" + q.toString());
        }
        for (DslDatabase db : dbs) {
            LOG.info("\t" + db.toString());
        }
    }

    private static void printElapsedTime(Instant start, String msg) {
        long totalSeconds = Instant.now().getEpochSecond() - start.getEpochSecond();
        long minutes = totalSeconds / 60 % 60;
        long seconds = totalSeconds - minutes * 60;

        if (minutes > 0) {
            LOG.info(msg + " Took: " + minutes + "m" + seconds + "s.");
        } else {
            LOG.info(msg + " Took: " + seconds + "s.");
        }

    }

}
