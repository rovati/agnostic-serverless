package ch.elca.rovl.dsl.pipeline.templating.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.StringJoiner;

import org.apache.commons.io.FileUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import ch.elca.rovl.dsl.api.function.models.functiontrigger.rest.FunctionHttpTrigger;
import ch.elca.rovl.dsl.pipeline.deployment.DeploymentConstants;
import ch.elca.rovl.dsl.pipeline.linking.resource.LinkedDatabase;
import ch.elca.rovl.dsl.pipeline.linking.resource.LinkedFunction;
import ch.elca.rovl.dsl.pipeline.linking.resource.LinkedQueue;
import ch.elca.rovl.dsl.pipeline.linking.resource.LinkedResource;
import ch.elca.rovl.dsl.pipeline.templating.ResourceNameMemory;
import ch.elca.rovl.dsl.pipeline.templating.TemplatingConstants;
import ch.elca.rovl.dsl.pipeline.templating.TemplatingEngine;
import ch.elca.rovl.dsl.pipeline.templating.resource.DeployableFunction;
import ch.elca.rovl.dsl.pipeline.util.IdGenerator;
import ch.elca.rovl.dsl.pipeline.util.Provider;
import ch.elca.rovl.dsl.pipeline.util.RequiredData.Type;
import ch.elca.rovl.dsl.resource.function.HttpMethod;

/**
 * Helper class of {@link TemplatingEngine TemplatingEngine} to generate function projects that can
 * be built and deployed on Azure.
 */
public final class AzureTemplatingHelper implements TemplatingHelper {

    final VelocityContext context;
    final VelocityEngine engine;
    final Random rng;
    final String outputDir;
    final String packageName;
    final ResourceNameMemory namesMemory;

    String functionAppName;
    String defaultResourceGroup = DeploymentConstants.AZURE_DEFAULT_RESOURCE_GROUP;
    String defaultRegion = DeploymentConstants.AZURE_DEFAULT_REGION_STR;

    List<String> properties;
    final List<String> parentTag;

    final String hostJsonName = "host.json";
    final String localSettingsName = "local.settings.json";
    final String parentPomVM = "vtemplates/azure/azureparentpom.vm";
    final String hostJsonVM = "vtemplates/azure/hostjson.vm";
    final String localSettingsVM = "vtemplates/azure/localsettings.vm";
    final String handlerVMDir = "vtemplates/azure/handler/";
    final String glueVMDir = "vtemplates/azure/glue/";
    final String httpTriggerVM = "httptrigger.vm";
    final String httpTriggerWithDBVM = "httptriggerwithdb.vm";
    final String httpForGlueTriggerVM = "httptriggerforglue.vm";
    final String httpForGlueTriggerWithDBVM = "httpgluewithdb.vm";
    final String servicebusTriggerVM = "sbtrigger.vm";
    final String servicebusTriggerWithDBVM = "sbtriggerwithdb.vm";
    final String glueHandlerVM = "gluehandler.vm";
    final String gluePomVM = "gluepom.vm";


    /**
     * Templating helper for Azure constructor.
     * 
     * @param engine Velocity templating engine
     * @param context Velocity templating context
     * @param outputDir path to the directory where to generate function projects
     * @param packageName package name of the generated projects
     * @param namesMemory data structure containing cloud names of already deployed resources.
     * @throws IOException
     */
    public AzureTemplatingHelper(VelocityEngine engine, VelocityContext context, String outputDir,
            String packageName, ResourceNameMemory namesMemory) throws IOException {
        this.context = context;
        this.engine = engine;
        this.packageName = packageName;
        this.outputDir = outputDir + "azure/";
        this.rng = new Random();
        this.namesMemory = namesMemory;

        // create azure dir in output folder if it does not exist
        File azureDir = new File(this.outputDir);
        if (!azureDir.exists()) {
            // create dir
            azureDir.mkdir();
        }

        // write parent pom
        context.put("package", packageName);
        FileWriter fileWriter = new FileWriter(this.outputDir + TemplatingConstants.POM_NAME);
        Template t = engine.getTemplate(parentPomVM);
        t.merge(context, fileWriter);
        fileWriter.flush();
        fileWriter.close();

        // tag to add to pom
        parentTag =
                Arrays.asList("\t<parent>", String.format("\t\t<groupId>%s</groupId>", packageName),
                        "\t\t<artifactId>generated-azure-functions</artifactId>",
                        "\t\t<version>1.0.0</version>", "\t</parent>");
    }

    /**
     * Generates a project for the given function that is deployable on Azure, then returns the
     * function pipeline object extended with information required for deployment.
     * 
     * @param function
     * @throws IOException
     * @return function pipeline object extended with info for deployment
     */
    @Override
    public DeployableFunction generateFunction(LinkedFunction function) throws IOException {
        String functionAppName = setContext(function);

        String rootPath = outputDir + function.getName() + "/";
        String resourcesPath = rootPath + "src/main/resources/";
        String pomPath = rootPath + TemplatingConstants.POM_NAME;
        String handlerPath = rootPath + "src/main/java/" + packageName.replace('.', '/') + "/"
                + TemplatingConstants.HANDLER_NAME;
        String appPropertiesPath =
                rootPath + "src/main/resources/" + TemplatingConstants.APP_PROPERTIES_NAME;

        copyFunctionProject(function, rootPath);
        removeDebuggingProperties(resourcesPath);
        writeHandler(function, handlerPath);
        extendPom(pomPath, function.hasDatabaseOutput(),
                function.requiresGlue() || function.getInputQueue() != null,
                function.requiresGlue());
        setProperties(function, appPropertiesPath);
        writeAdditionalProviderFile(rootPath);

        DeployableFunction dfn = new DeployableFunction(function, rootPath);
        dfn.setCloudName(functionAppName);

        // if function has queue input and it is not a glue function, register for queue listen
        // perms
        if (function.getInputQueue() != null && !function.requiresGlue()) {
            dfn.require(Type.QUEUE_LISTEN, function.getInputQueue().getName());
        }

        for (LinkedResource lr : function.getOutput()) {
            // if has function output, register for target function url
            if (lr instanceof LinkedFunction)
                dfn.require(Type.FUNCTION_URL, lr.getName());
            // if has queue output, register for queue send perms
            else if (lr instanceof LinkedQueue)
                dfn.require(Type.QUEUE_SEND, lr.getName());
            // if has database output, register for database access perms
            else if (lr instanceof LinkedDatabase)
                dfn.require(Type.DATABASE_CONNECTION, lr.getName());
            else
                throw new IllegalStateException(
                        "Function output is not a supported linked resource.");
        }

        return dfn;
    }

    /**
     * Generates a project of a glue function deployable on Azure for the given function, then
     * returns the function pipeline object extended with information required for deployment.
     * 
     * @param function function that requries glue
     * @throws IOException
     * @return function pipeline object extended with info for deployment
     */
    @Override
    public DeployableFunction generateGlue(DeployableFunction function)
            throws IOException, InterruptedException {
        String functionAppName = setGlueContext(function);
        String fnName = function.getName() + "-glue";

        String rootDir = outputDir + fnName + "/";
        String codeDir = rootDir + "src/main/java/" + packageName.replace(".", "/") + "/";
        String testDir = rootDir + "src/main/test/";

        generateGlueProject(rootDir, codeDir, testDir);
        writeGluePom(rootDir);
        writeGlueHandler(codeDir);
        writeAdditionalProviderFile(rootDir);

        // create function object for next pipeline step
        DeployableFunction dfn = new DeployableFunction(function.getFunction(), rootDir);
        dfn.setCloudName(functionAppName);
        // register for needed configuration data
        dfn.require(Type.FUNCTION_URL, function.getName());
        dfn.require(Type.QUEUE_LISTEN, function.getFunction().getInputQueue().getName());
        dfn.require(Type.QUEUE_TRIGGER, function.getFunction().getInputQueue().getName());
        dfn.setAsGlue(Provider.AZURE);

        return dfn;
    }

    /**
     * Sets values on the templating context for the generation of the project for given function.
     * 
     * @param function
     * @return name of the cloud function corresponding to the given function
     * @throws IOException
     */
    private String setContext(LinkedFunction function) throws IOException {
        String memoryName = namesMemory.get(function.getName());
        String functionAppName;

        if (memoryName != null) {
            functionAppName = memoryName;
        } else {
            functionAppName =
                    function.getName().replace('_', '-') + "-" + IdGenerator.get().generate();
            namesMemory.addNewMemory(function.getName(), functionAppName);
        }

        context.put("functionName", function.getName());
        context.put("functionAppName", functionAppName);
        if (function.getInputQueue() != null)
            context.put("queueName", function.getInputQueue().getName());

        String resourceGroup = function.getResourceGroupName();
        resourceGroup = resourceGroup == null ? defaultResourceGroup : resourceGroup;
        String region = function.getRegion();
        region = region == null ? defaultRegion : region;

        properties = Arrays.asList(
                String.format("\t\t<functionAppName>%s</functionAppName>", functionAppName),
                String.format("\t\t<resourceGroupName>%s</resourceGroupName>", resourceGroup),
                String.format("\t\t<region>%s</region>", region));

        return functionAppName;
    }

    /**
     * Copied the function project to the folder containing the deployable projects.
     * 
     * @param function
     * @param rootPath path to the root directory containing functions deployable on Azure
     * @throws IOException
     */
    private void copyFunctionProject(LinkedFunction function, String rootPath) throws IOException {
        File generatedDir = new File(rootPath);
        if (generatedDir.exists())
            throw new IllegalStateException(String.format(
                    "Error generating function code, dir for function '%s' already exists.",
                    rootPath));

        FileUtils.copyDirectory(
                new File(function.getFunction().getPathToProject() + function.getName()),
                generatedDir);
    }

    private void removeDebuggingProperties(String resourcesPath) throws IOException {
        File propsFile = new File(resourcesPath + "application.properties");
        if (!propsFile.exists())
            return;
        
        FileReader fReader = new FileReader(propsFile);
        BufferedReader reader = new BufferedReader(fReader);
        List<String> modifiedContent = new ArrayList<>();

        Boolean insideGeneratedBlock = false;

        // remove previous data
        for (String line; (line = reader.readLine()) != null;) {
            if (line.contains("# GENERATED - DO NOT EDIT")) {
                insideGeneratedBlock = true;
            }
            if (line.contains("# END GENERATED")) {
                insideGeneratedBlock = false;
                continue;
            }

            if (insideGeneratedBlock && (line.contains("function.name") ||
                    line.contains("function.trigger") ||
                    line.contains("quarkus.datasource") ||
                    line.contains("# GENERATED - DO NOT EDIT") ||
                    line.contains("# END GENERATED"))) {
                continue;
            }
            modifiedContent.add(line);
        }

        fReader.close();
        reader.close();

        FileUtils.writeLines(propsFile, modifiedContent);
    }

    /**
     * Adds the function trigger handler to the generated project.
     * 
     * @param function
     * @param handlerPath path to the location where the handler needs to be written
     * @throws IOException
     */
    private void writeHandler(LinkedFunction function, String handlerPath) throws IOException {
        FileWriter fileWriter = new FileWriter(handlerPath);
        Template t;

        if (function.requiresGlue()) {
            if (function.hasDatabaseOutput()) {
                setDatabaseNamesInContext(function);
                t = engine.getTemplate(handlerVMDir + httpForGlueTriggerWithDBVM);
            } else {
                t = engine.getTemplate(handlerVMDir + httpForGlueTriggerVM);
            }
        } else {
            switch (function.getTriggerType()) {
                case HTTP:
                    // TODO get trigger
                    FunctionHttpTrigger trigger = (FunctionHttpTrigger) function.getTrigger();
                    setTriggerInContext(trigger);

                    if (function.hasDatabaseOutput()) {
                        setDatabaseNamesInContext(function);
                        t = engine.getTemplate(handlerVMDir + httpTriggerWithDBVM);
                    } else {
                        t = engine.getTemplate(handlerVMDir + httpTriggerVM);
                    }
                    break;
                case QUEUE:
                    if (function.hasDatabaseOutput()) {
                        setDatabaseNamesInContext(function);
                        t = engine.getTemplate(handlerVMDir + servicebusTriggerWithDBVM);
                    } else {
                        t = engine.getTemplate(handlerVMDir + servicebusTriggerVM);
                    }
                    break;
                case TIMER:
                default:
                    fileWriter.close();
                    throw new IllegalArgumentException("Unsupported trigger: " + function.getTriggerType());
            }
        }

        t.merge(context, fileWriter);
        fileWriter.flush();
        fileWriter.close();
    }

    /**
     * Modifies the pom by adding necessary tags for the maven build.
     * 
     * @param pomPath path to the generated pom
     * @param addDBDependencies whether to add dependencies to support database interactions
     * @param addExchangeDependencies whether to add dependencies for Apache Camel Exchange
     *        deserialization
     * @param addJwtDependencies whether to add dependencies for Java JWT
     * @throws IOException
     */
    private void extendPom(String pomPath, boolean addDBDependencies,
            boolean addExchangeDependencies, boolean addJwtDependencies) throws IOException {
        File pom = new File(pomPath);
        FileReader fReader = new FileReader(pom);
        BufferedReader reader = new BufferedReader(fReader);
        List<String> modifiedPomContent = new ArrayList<>();

        boolean alreadyContainsJDBC = false;
        boolean alreadyContainsPostgreSQL = false;
        boolean alreadyContainsCommmonsDBCP = false;
        boolean alreadyContainsCommonsPool = false;
        boolean alreadyContainsJackson = false;
        boolean alreadyContainsJjwtApi = false;
        boolean alreadyContainsJjwtImpl = false;
        boolean alreadyContainsJjwtJackson = false;

        for (String line; (line = reader.readLine()) != null;) {
            if (line.contains("camel-quarkus-jdbc"))
                alreadyContainsJDBC = true;
            if (line.contains("quarkus-jdbc-postgresql"))
                alreadyContainsPostgreSQL = true;
            if (line.contains("commons-dbcp"))
                alreadyContainsCommmonsDBCP = true;
            if (line.contains("commons-pool"))
                alreadyContainsCommonsPool = true;
            if (line.contains("jackson-jr-objects"))
                alreadyContainsJackson = true;
            if (line.contains("jjwt-api"))
                alreadyContainsJjwtApi = true;
            if (line.contains("jjwt-impl"))
                alreadyContainsJjwtImpl = true;
            if (line.contains("jjwt-jackson"))
                alreadyContainsJjwtJackson = true;

            // add missing tags
            if (line.trim().startsWith("<modelVersion>")) {
                modifiedPomContent.addAll(parentTag);
            } else if (line.trim().startsWith("</properties>")) {
                modifiedPomContent.addAll(properties);
            } else if (line.trim().startsWith("</dependencies>")) {
                if (addDBDependencies) {
                    if (!alreadyContainsJDBC) {
                        modifiedPomContent.addAll(TemplatingConstants.JDBC_TAG);
                    }
                    if (!alreadyContainsPostgreSQL) {
                        modifiedPomContent.addAll(TemplatingConstants.POSTGRES_TAG);
                    }
                    if (!alreadyContainsCommmonsDBCP) {
                        modifiedPomContent.addAll(TemplatingConstants.COMMONS_DBCP_TAG);
                    }
                    if (!alreadyContainsCommonsPool) {
                        modifiedPomContent.addAll(TemplatingConstants.COMMONS_POOL_TAG);
                    }
                }
                if (addExchangeDependencies && !alreadyContainsJackson) {
                    modifiedPomContent.addAll(TemplatingConstants.JACKSON_TAG);
                }
                if (addJwtDependencies) {
                    if (!alreadyContainsJjwtApi) {
                        modifiedPomContent.addAll(TemplatingConstants.JJWT_API_TAG);
                    }
                    if (!alreadyContainsJjwtImpl) {
                        modifiedPomContent.addAll(TemplatingConstants.JJWT_IMPL_TAG);
                    }
                    if (!alreadyContainsJjwtJackson) {
                        modifiedPomContent.addAll(TemplatingConstants.JJWT_JACKSON_TAG);
                    }
                }
            }

            modifiedPomContent.add(line);
        }

        fReader.close();
        reader.close();

        FileUtils.writeLines(pom, modifiedPomContent);
    }

    /**
     * Writes a properties file containing info about resources this function is linked to, which
     * will be used by Apache Camel components.
     * 
     * @param function
     * @param appPropertiesPath path to where the properties file needs to be written
     * @throws IOException
     */
    private void setProperties(LinkedFunction function, String appPropertiesPath)
            throws IOException {
        File appPropeties = new File(appPropertiesPath);
        if (!appPropeties.exists()) {
            FileUtils.forceMkdirParent(appPropeties);
        }

        for (LinkedResource lr : function.getOutput()) {
            String entry = "";

            if (lr instanceof LinkedFunction)
                entry = TemplatingConstants.FUNCTION_PROPERTY;
            else if (lr instanceof LinkedQueue)
                entry = TemplatingConstants.QUEUE_PROPERTY;
            else if (lr instanceof LinkedDatabase)
                entry = TemplatingConstants.DATABASE_PROPERTY;
            else
                throw new IllegalStateException(
                        "Function output contains an unsupported linked resource.");

            FileUtils.writeStringToFile(appPropeties,
                    String.format(entry, lr.getName(), lr.getProvider()), "utf8", true);
        }

        //add inputs
        LinkedQueue inputQ = function.getInputQueue(); 
        if (inputQ != null) {
            FileUtils.writeStringToFile(appPropeties,
                    String.format(TemplatingConstants.QUEUE_PROPERTY, inputQ.getName(), function.getProvider()), "utf8", true);
        }
        if (function.getTrigger() != null) {
            FileUtils.writeStringToFile(appPropeties,
                    String.format(TemplatingConstants.FUNCTION_PROPERTY, "trigger", function.getProvider()), "utf8", true);
        }
    }

    /**
     * Writes additional project files needed for building and deploying on Azure.
     * 
     * @param rootPath path to the root directory of the generated project
     * @throws IOException
     */
    private void writeAdditionalProviderFile(String rootPath) throws IOException {
        // host.json
        FileWriter fileWriter = new FileWriter(rootPath + hostJsonName);
        Template t = engine.getTemplate(hostJsonVM);
        t.merge(context, fileWriter);
        fileWriter.flush();
        fileWriter.close();

        // local.settings.json
        fileWriter = new FileWriter(rootPath + localSettingsName);
        t = engine.getTemplate(localSettingsVM);
        t.merge(context, fileWriter);
        fileWriter.flush();
        fileWriter.close();
    }

    /**
     * Same as {@link #setContext(LinkedFunction) setContext} but for a glue function.
     */
    private String setGlueContext(DeployableFunction function) throws IOException {
        String memoryName = namesMemory.get(function.getName() + "-glue");
        String functionAppName;
        if (memoryName != null) {
            functionAppName = memoryName;
        } else {
            // TODO replace should not be necessary
            functionAppName = function.getCloudName().replace('_', '-') + "-glue";
            namesMemory.addNewMemory(function.getName() + "-glue", functionAppName);
        }

        context.put("functionName", function.getName() + "-glue");
        context.put("functionAppName", functionAppName);
        if (function.getFunction().getInputQueue() != null)
            context.put("queueName", function.getFunction().getInputQueue().getName());

        context.put("resourceGroup", defaultResourceGroup);
        context.put("region", defaultRegion);
        context.put("targetFunction", function.getName());

        return functionAppName;
    }

    /**
     * Generates the project directories for a glue function to be deployed on Azure.
     * 
     * @param rootDir path to the root directory where the project will be generated
     * @param codeDir path to the code directory
     * @param testDir code to the test directory
     * @throws IOException
     */
    private void generateGlueProject(String rootDir, String codeDir, String testDir)
            throws IOException {
        FileUtils.forceMkdir(new File(rootDir));
        FileUtils.forceMkdir(new File(codeDir));
        FileUtils.forceMkdir(new File(testDir));
    }

    /**
     * Writes the pom for the glue function generated project.
     * 
     * @param rootDir path to the root directory of the generated project
     * @throws IOException
     */
    private void writeGluePom(String rootDir) throws IOException {
        FileWriter fileWriter = new FileWriter(rootDir + TemplatingConstants.POM_NAME);
        Template t = engine.getTemplate(glueVMDir + gluePomVM);

        t.merge(context, fileWriter);
        fileWriter.flush();
        fileWriter.close();
    }

    /**
     * Writes the function trigger handler for the glue function generated project.
     * 
     * @param rootDir path to the root directory of the generated project
     * @throws IOException
     */
    private void writeGlueHandler(String srcDir) throws IOException {
        FileWriter fileWriter = new FileWriter(srcDir + TemplatingConstants.HANDLER_NAME);
        Template t = engine.getTemplate(glueVMDir + glueHandlerVM);

        t.merge(context, fileWriter);
        fileWriter.flush();
        fileWriter.close();
    }

    /**
     * Adds to the templating context values needed for the function trigger.
     * 
     * @param trigger http trigger
     */
    private void setTriggerInContext(FunctionHttpTrigger trigger) {
        StringJoiner sj = new StringJoiner(", ");
        for (HttpMethod http : trigger.getHttpMethods()) {
            switch (http) {
                case POST:
                    sj.add("HttpMethod.POST");
                    break;
                case GET:
                    sj.add("HttpMethod.GET");
                    break;
                case DELETE:
                    sj.add("HttpMethod.DELETE");
                    break;
                case PATCH:
                    sj.add("HttpMethod.PATCH");
                    break;
                default:
                    throw new IllegalArgumentException("Http method not supported: " + http);
            }
        }

        context.put("httpMethods", sj.toString());

        switch (trigger.getAuthType()) {
            case PUBLIC:
                context.put("authLevel", "AuthorizationLevel.ANONYMOUS");
                break;
            default:
                throw new IllegalArgumentException(
                        "Authorization level not supported: " + trigger.getAuthType());
        }
    }

    /**
     * Builds the list of output databases names and sets it in the context for the trigger
     * template.
     * 
     * @param function
     */
    private void setDatabaseNamesInContext(LinkedFunction function) {
        StringJoiner sj = new StringJoiner(", ");
        for (LinkedResource lr : function.getOutput()) {
            if (lr instanceof LinkedDatabase) {
                sj.add("\"" + lr.getName() + "\"");
            }
        }
        String databaseList = "List.of(" + sj.toString() + ")";
        context.put("databaseNames", databaseList);
    }

}
