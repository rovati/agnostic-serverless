package ch.elca.rovl.dsl.pipeline.linking;

import java.io.File;
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
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elca.rovl.dsl.pipeline.infraparsing.resource.DslDatabase;
import ch.elca.rovl.dsl.pipeline.infraparsing.resource.DslFunction;
import ch.elca.rovl.dsl.pipeline.infraparsing.resource.DslQueue;
import ch.elca.rovl.dsl.pipeline.infraparsing.resource.DslResource;
import ch.elca.rovl.dsl.pipeline.linking.helper.DeducerHelper;
import ch.elca.rovl.dsl.pipeline.linking.model.FunctionConnections;
import ch.elca.rovl.dsl.pipeline.linking.model.Link;
import ch.elca.rovl.dsl.pipeline.linking.resource.LinkedDatabase;
import ch.elca.rovl.dsl.pipeline.linking.resource.LinkedFunction;
import ch.elca.rovl.dsl.pipeline.linking.resource.LinkedQueue;
import ch.elca.rovl.dsl.pipeline.linking.resource.LinkedResource;
import ch.elca.rovl.dsl.pipeline.util.Constants;
import ch.elca.rovl.dsl.pipeline.util.Provider;
import ch.elca.rovl.dsl.pipeline.util.ProviderParser;
import ch.elca.rovl.dsl.pipeline.util.ResourceType;

/**
 * Engine that takes care of creating links between the givne resources. It matches each resource to
 * its chosen target provider, and then deduces links between the resources from the computing logic
 * of the functions.
 */
public final class LinkerEngine {

    private static Logger LOG = LoggerFactory.getLogger("Links");

    final String deductionDir = Constants.GENERATED_DIR + "linking/link_deduction/";
    final String routeBuilderName = "FunctionRoute.java";

    final DeducerHelper deducerHelper;
    final ProviderParser providerParser;
    final Map<String, DslFunction> nameToFunction;
    final Map<String, DslQueue> nameToQueue;
    final Map<String, DslDatabase> nameToDatabase;
    final List<Link> links;
    final FunctionConnections connections;
    final Map<String, LinkedResource> linkedResources;
    List<String> execPlugin;

    final VelocityEngine engine;
    final VelocityContext context;

    // NOTE when number of supported resources grows, just pass a single map {name -> dslresource}
    /**
     * LinkerEngine constructor.
     * 
     * @param providersFile path to the file containing the chosen target provider for each resource
     * @param nameToFunction map {functionName -> function} containing all functions parsed at the
     *        previous pipeline step
     * @param nameToQueue map {queueName -> queue} containing all queues parsed at the previous
     *        pipeline step
     * @param nameToDatabase map {databaseName -> database} containing all databases parsed at the
     *        previous pipeline step
     * @throws URISyntaxException if {@code providersFile} is not a valid URI
     */
    public LinkerEngine(String providersFile, Map<String, DslFunction> nameToFunction,
            Map<String, DslQueue> nameToQueue, Map<String, DslDatabase> nameToDatabase)
            throws URISyntaxException {
        this.providerParser = new ProviderParser(providersFile);
        this.nameToFunction = nameToFunction;
        this.nameToQueue = nameToQueue;
        this.nameToDatabase = nameToDatabase;
        this.connections = new FunctionConnections();
        this.links = Collections.synchronizedList(new ArrayList<>());
        this.linkedResources = new HashMap<>();

        engine = new VelocityEngine();
        engine.setProperty(RuntimeConstants.RESOURCE_LOADERS, "classpath");
        engine.setProperty("resource.loader.classpath.class",
                ClasspathResourceLoader.class.getName());
        engine.init();
        context = new VelocityContext();

        this.deducerHelper = new DeducerHelper(deductionDir, routeBuilderName, context, engine);
    }

    /**
     * Deduces the links between resources from the project code of the functions.
     * 
     * @throws IOException
     */
    public void deduceLinks() throws IOException {
        // create temp folder
        File tmp = new File(deductionDir);
        if (tmp.exists()) {
            // clean
            FileUtils.cleanDirectory(tmp);
        } else {
            // create
            FileUtils.forceMkdir(tmp);
        }

        // parallel exec of link deduction
        ExecutorService executor = Executors.newCachedThreadPool();
        Map<Future<?>, LinkedFunction> deductionTasks = new HashMap<>();
        AtomicInteger successfulCompletions = new AtomicInteger();
        int functionsNb = 0;

        for (LinkedResource resource : linkedResources.values()) {
            if (resource instanceof LinkedFunction) {
                LinkedFunction fn = (LinkedFunction) resource;
                functionsNb++;

                deductionTasks.put(executor.submit(() -> {
                    String rootDir;
                    try {
                        rootDir = deducerHelper.copyProject(fn.getFunction());
                    } catch (IOException e) {
                        LOG.error("Failed to copy function project.");
                        e.printStackTrace();
                        return;
                    }
                    try {
                        deducerHelper.addEndpointExtractor(rootDir, fn.getFunction().getHandler());
                    } catch (IOException e) {
                        LOG.error("Failed to add extractor to project.");
                        e.printStackTrace();
                        return;
                    }
                    try {
                        deducerHelper.extendPom(rootDir);
                    } catch (IOException e) {
                        LOG.error("Failed to extend pom.");
                        e.printStackTrace();
                        return;
                    }
                    File endpointsFile = deducerHelper.extractEndpoints(rootDir, fn.getName());
                    try {
                        links.addAll(deducerHelper.createLinks(fn.getName(), endpointsFile));
                    } catch (IOException e) {
                        LOG.error("Failed to create links.");
                        e.printStackTrace();
                        return;
                    }

                    successfulCompletions.incrementAndGet();
                }), fn);
            }
        }

        boolean deductionHasFailed = false;

        // wait until all tasks are done
        for (Future<?> res : deductionTasks.keySet()) {
            try {
                res.get();
            } catch (ExecutionException | InterruptedException e) {
                deductionHasFailed = true;
                LOG.error(String.format("Deduction failed for function %s",
                        deductionTasks.get(res).getName()), e);
            }
        }

        executor.shutdown();

        if (deductionHasFailed || successfulCompletions.get() != functionsNb) {
            throw new IllegalStateException("Link deduction of some functions failed.");
        }

        registerFunctionConnections();

    }

    // TODO change to stack up all resources without provider, and throw once
    /**
     * Matches each resource from the previous pipeline step to the target provider chosen by the
     * user.
     * 
     * @throws IllegalStateException
     * @throws IOException
     */
    public void matchToProvider(boolean debug) throws IllegalStateException, IOException {
        if (debug) {
            for (DslQueue q : nameToQueue.values()) {
                linkedResources.put(q.getName(), new LinkedQueue(q, Provider.DEBUG));
            }
            for (DslFunction f : nameToFunction.values()) {
                linkedResources.put(f.getName(), new LinkedFunction(f, Provider.DEBUG));
            }
    
            for (DslDatabase db : nameToDatabase.values()) {
                linkedResources.put(db.getName(), new LinkedDatabase(db, Provider.DEBUG));
            }
            return;
        }

        providerParser.parse();
        Map<DslResource, Provider> providerLinked = linkToProvider();

        for (DslQueue q : nameToQueue.values()) {
            linkedResources.put(q.getName(), new LinkedQueue(q, providerLinked.get(q)));
        }
        for (DslFunction f : nameToFunction.values()) {
            linkedResources.put(f.getName(), new LinkedFunction(f, providerLinked.get(f)));
        }

        for (DslDatabase db : nameToDatabase.values()) {
            linkedResources.put(db.getName(), new LinkedDatabase(db, providerLinked.get(db)));
        }
    }

    /**
     * For each function registers the input and output resources of its links. Returns a map
     * containing all the resources parsed at the previous pipeline step extended with their chosen
     * target provider and the resources they are linked to.
     * 
     * @return map {resourceName -> resource} containing all linked resources
     */
    public Map<String, LinkedResource> linkResources() {
        linkFunctionInput();
        linkFunctionOutput();
        return linkedResources;
    }

    /**
     * For each deduced link it registers input and output resources of each function.
     */
    private void registerFunctionConnections() {
        for (Link l : links) {
            // if the in resource of the link is a function, register the out resource of the link
            // as an output resource of the fucntion
            if (l.getInputType() == ResourceType.FUNCTION) {
                DslResource outResource;
                switch (l.getOutputType()) {
                    case FUNCTION:
                        outResource = nameToFunction.get(l.getOutput());
                        break;
                    case QUEUE:
                        outResource = nameToQueue.get(l.getOutput());
                        break;
                    case DATABASE:
                        outResource = nameToDatabase.get(l.getOutput());
                        break;
                    default:
                        throw new IllegalStateException("Output type is not supported.");
                }

                connections.registerOutput(l.getInput(), outResource);
            }
            // if the out resource of the link is a function, register the in resource of the link
            // as an input resource of the fucntion
            if (l.getOutputType() == ResourceType.FUNCTION) {
                DslResource inResource;
                switch (l.getInputType()) {
                    case FUNCTION:
                        inResource = nameToFunction.get(l.getInput());
                        break;
                    case QUEUE:
                        inResource = nameToQueue.get(l.getInput());
                        break;
                    default:
                        throw new IllegalStateException("Output type is not supported.");
                }

                connections.registerInput(l.getOutput(), inResource);
            }
        }
    }

    /**
     * For each resource parsed at the previous pipeline step it reads and registers its target
     * provider chosen by the user.
     * 
     * @return a map containing all resources mapped to their chosen provider
     */
    private Map<DslResource, Provider> linkToProvider() {
        Map<DslResource, Provider> providerLinked = new HashMap<>();

        for (String functionName : nameToFunction.keySet()) {
            Provider p = providerParser.getProviderOf(functionName);
            if (p == null) {
                throw new IllegalStateException(String
                        .format("Could not find a provider for function '%s'!", functionName));
            }

            providerLinked.put(nameToFunction.get(functionName), p);
        }

        for (String queueName : nameToQueue.keySet()) {
            Provider p = providerParser.getProviderOf(queueName);
            if (p == null) {
                throw new IllegalStateException(
                        String.format("Could not find a provider for queue '%s'!", queueName));
            }
            providerLinked.put(nameToQueue.get(queueName), p);
        }

        for (String databaseName : nameToDatabase.keySet()) {
            Provider p = providerParser.getProviderOf(databaseName);
            if (p == null) {
                throw new IllegalStateException(String
                        .format("Could not find a provider for database '%s'!", databaseName));
            }
            providerLinked.put(nameToDatabase.get(databaseName), p);
        }

        return providerLinked;
    }

    /**
     * For each function it validates its input and then adds them to the function pipeline object.
     * Inputs of a functions are valid if one of the following cases is met:
     * <ul>
     * <li>there is no input resource</li>
     * <li>there is exactly one queue</li>
     * <li>there is any number of functions</li>
     * </ul>
     */
    private void linkFunctionInput() {
        for (LinkedResource lr : linkedResources.values()) {
            if (lr instanceof LinkedFunction) {
                // get registered inputs of function and separate by resource type
                List<DslResource> inputs = connections.getInputs(lr.getName());
                if (inputs != null && !inputs.isEmpty()) {
                    List<DslFunction> inputFunctions = new ArrayList<>();
                    List<DslQueue> inputQueues = new ArrayList<>();

                    for (DslResource r : inputs) {
                        if (r instanceof DslFunction)
                            inputFunctions.add((DslFunction) r);
                        else if (r instanceof DslQueue)
                            inputQueues.add((DslQueue) r);
                        else
                            throw new IllegalStateException(String.format(
                                    "Function '%s' does not support resource '%s' as input.",
                                    lr.getName(), r.getName()));
                    }

                    validateFunctionInputs((LinkedFunction) lr, inputFunctions, inputQueues);

                    // if input is a queue, add it to the function pipeline object
                    if (inputQueues.size() == 1) {
                        DslQueue inputQ = inputQueues.get(0);
                        ((LinkedFunction) lr)
                                .setInputQueue((LinkedQueue) linkedResources.get(inputQ.getName()));
                    }

                }
            }
        }
    }

    /**
     * Validates the inputs of a function.
     * 
     * @param function function
     * @param fns list of input functions
     * @param qs list of input queues
     * @param apis list of input apis
     */
    private void validateFunctionInputs(LinkedFunction function, List<DslFunction> fns,
            List<DslQueue> qs) {
        // can have only one type among functions, queues and apis
        if (fns.size() > 0 && qs.size() > 0) {
            throw new IllegalArgumentException(String.format(
                    "Illegal layout: function '%s' cannot have a mix of functions and queues as input!"
                            + "input functions: %s - input queues: %s",
                    function.getName(), fns.toString(), qs.toString()));
        }
        // if has queues, can have only exactly one
        if (qs.size() > 1) {
            throw new IllegalArgumentException(String
                    .format("Illegal layout: function '%s' cannot have multiple queues as input!"
                            + "input queues: %s", function.getName(), qs.toString()));
        }

    }

    /**
     * Adds the output resources of a function to its pipeline object.
     */
    private void linkFunctionOutput() {
        for (LinkedResource lr : linkedResources.values()) {
            if (lr instanceof LinkedFunction) {
                List<DslResource> outputResources = connections.getOutputs(lr.getName());
                if (outputResources != null) {
                    for (DslResource r : outputResources) {
                        validateAWSLink(lr, linkedResources.get(r.getName()));
                        ((LinkedFunction) lr).addOutput(linkedResources.get(r.getName()));
                    }
                }
            }
        }
    }

    /**
     * Due to limitations in the AWS sandbox used for development, if a function is linked to a
     * database on AWS also the funciton has to be on AWS. This call validates this condition.
     * 
     * @param fn
     * @param output
     */
    private void validateAWSLink(LinkedResource fn, LinkedResource output) {
        if (output instanceof LinkedDatabase && output.getProvider() == Provider.AWS && fn.getProvider() != Provider.AWS) {
            throw new UnsupportedOperationException(String.format(
                    "Function '%s' has to be deployed on AWS to be able to interact with database '%s' on AWS.",
                    fn.getName(), output.getName()));
        }
    }

}
