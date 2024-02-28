package ch.elca.rovl.dsl.pipeline.templating.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringJoiner;
import static java.util.Map.entry;

import org.apache.commons.io.FileUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

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

/**
 * Helper class of {@link TemplatingEngine TemplatingEngine} to generate
 * function projects that can
 * be built and deployed on AWS.
 */
public class AwsTemplatingHelper implements TemplatingHelper {

    final String templatesDir = "vtemplates/aws/";
    final String dockerTemplatesDir = templatesDir + "docker/";
    final String triggerTemplatesDir = templatesDir + "handler/";
    final String glueTemplatesDir = templatesDir + "glue/";
    final String dockerignoreVM = "aws-dockerignore.vm";
    final String gitignoreVM = "aws-gitignore.vm";
    final String mvnwVM = "aws-mvnw.vm";
    final String mvnwCmdVM = "aws-mvnw-cmd.vm";
    final String parentPomVM = "aws-parentpom.vm";
    final String dfJvmVM = "aws-df-jvm.vm";
    final String dfLegacyJarVM = "aws-df-legacy-jar.vm";
    final String dfNativeVM = "aws-df-native.vm";
    final String dfNativeMicroVM = "aws-df-native-micro.vm";
    final String restHandlerVM = "rest.vm";
    final String functionUrlHandlerVM = "functionurl.vm";
    final String queueHandlerVM = "queue.vm";
    final String forglueHandlerVM = "forglue.vm";
    final String glueHandlerVM = "gluehandler.vm";
    final String gluePomVM = "gluepom.vm";

    final VelocityEngine engine;
    final VelocityContext context;
    final ResourceNameMemory namesMemory;
    final Random rng;
    final List<String> parentTag;

    final String outputDir;

    /**
     * Templating helper for AWS constructor.
     * 
     * @param engine      Velocity templating engine
     * @param context     Velocity templating context
     * @param outputDir   path to the directory where to generate function projects
     * @param packageName package name of the generated projects
     * @param namesMemory data structure containing cloud names of already deployed
     *                    resources.
     * @throws IOException
     */
    public AwsTemplatingHelper(VelocityEngine engine, VelocityContext context, String outputDir,
            ResourceNameMemory namesMemory) throws IOException {
        this.engine = engine;
        this.context = context;
        this.outputDir = outputDir + "aws/";
        this.namesMemory = namesMemory;
        this.rng = new Random();

        this.context.put("package", TemplatingConstants.PACKAGE_NAME);

        // create generated aws dir
        File awsDir = new File(this.outputDir);
        if (!awsDir.exists()) {
            // create dir
            awsDir.mkdir();
        }

        // write parent pom
        FileWriter fileWriter = new FileWriter(this.outputDir + TemplatingConstants.POM_NAME);
        Template t = engine.getTemplate(templatesDir + parentPomVM);
        t.merge(context, fileWriter);
        fileWriter.flush();
        fileWriter.close();

        // parent tag to add to user defined pom
        parentTag = Arrays.asList("\t<parent>",
                String.format("\t\t<groupId>%s</groupId>", TemplatingConstants.PACKAGE_NAME),
                "\t\t<artifactId>generated-aws-functions</artifactId>",
                "\t\t<version>1.0.0</version>", "\t</parent>");
    }

    /**
     * Generates a project for the given function that is deployable on AWS, then
     * returns the
     * function pipeline object extended with information required for deployment.
     * 
     * @param function
     * @throws IOException
     * @return function pipeline object extended with info for deployment
     */
    @Override
    public DeployableFunction generateFunction(LinkedFunction function) throws IOException {
        String cloudName = setContext(function);

        String rootPath = outputDir + function.getName() + "/";
        String mainPath = rootPath + "src/main/";
        String codePath = mainPath + "java/" + TemplatingConstants.PACKAGE_NAME.replace('.', '/') + "/";
        String resourcesPath = mainPath + "resources/";
        String dockerPath = mainPath + "docker/";

        copyProject(function, rootPath);
        removeDebuggingProperties(resourcesPath);
        writeLambdaFiles(rootPath, dockerPath);
        writeHandler(function, codePath);
        extendPom(rootPath, function.hasDatabaseOutput(),
                function.requiresGlue() || function.getInputQueue() != null,
                function.requiresGlue());
        writeProperties(function, resourcesPath);

        // create deployable function
        DeployableFunction dfn = new DeployableFunction(function, rootPath);
        dfn.setCloudName(cloudName);

        // if function has queue input and it is not a glue function, register for queue
        // listen
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
     * Generates a project of a glue function deployable on AWS for the given
     * function, then returns
     * the function pipeline object extended with information required for
     * deployment.
     * 
     * @param function function that requries glue
     * @throws IOException
     * @return function pipeline object extended with info for deployment
     */
    @Override
    public DeployableFunction generateGlue(DeployableFunction function)
            throws IOException, InterruptedException {
        String fnCloudName = setGlueContext(function);
        String fnName = function.getName() + "-glue";

        String rootPath = outputDir + fnName + "/";
        String codePath = rootPath + "src/main/java/" + TemplatingConstants.PACKAGE_NAME.replace('.', '/') + "/";
        String testPath = rootPath + "src/test/";

        generateGlueArchetype(fnName);
        clenaupGlueArchetype(rootPath, codePath, testPath);
        writeGluePom(rootPath);
        writeGlueHandler(codePath);

        // create function object for next pipeline step
        DeployableFunction dfn = new DeployableFunction(function.getFunction(), rootPath);
        dfn.setCloudName(fnCloudName);
        dfn.setAsGlue(Provider.AWS);
        // register for needed configuration data
        dfn.require(Type.FUNCTION_URL, function.getName());
        dfn.require(Type.QUEUE_LISTEN, function.getFunction().getInputQueue().getName());
        dfn.require(Type.QUEUE_TRIGGER, function.getFunction().getInputQueue().getName());

        return dfn;
    }

    /**
     * Sets values on the templating context for the generation of the project for
     * given function.
     * Returns the function cloud name.
     * 
     * @param function
     * @return function cloud name
     * @throws IOException
     */
    private String setContext(LinkedFunction function) throws IOException {
        String memoryName = namesMemory.get(function.getName());

        if (memoryName == null) {
            memoryName = function.getName().replace('_', '-') + "-" + IdGenerator.get().generate();
            namesMemory.addNewMemory(function.getName(), memoryName);
        }

        context.put("functionName", function.getName());
        if (function.getInputQueue() != null)
            context.put("queueName", function.getInputQueue().getName());

        return memoryName;
    }

    /**
     * Copied the function project to the folder containing the deployable projects.
     * 
     * @param function
     * @param rootPath path to the root directory containing functions deployable on
     *                 Azure
     * @throws IOException
     */
    private void copyProject(LinkedFunction function, String rootPath) throws IOException {
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
     * Writes additional project files needed for building and deploying on AWS.
     * 
     * @param rootPath   path to the root directory of the generated project
     * @param dockerOath path to the docker directory of the generated project
     * @throws IOException
     */
    private void writeLambdaFiles(String rootPath, String dockerPath) throws IOException {
        Map<String, String> files = Map.ofEntries(entry(rootPath + ".gitignore", templatesDir + gitignoreVM),
                entry(rootPath + ".dockerignore", templatesDir + dockerignoreVM),
                entry(rootPath + "mvnw", templatesDir + mvnwVM),
                entry(rootPath + "mvnw.cmd", templatesDir + mvnwCmdVM),
                entry(dockerPath + "Dockerfile.jvm", dockerTemplatesDir + dfJvmVM),
                entry(dockerPath + "Dockerfile.legacy-jar",
                        dockerTemplatesDir + dfLegacyJarVM),
                entry(dockerPath + "Dockerfile.native", dockerTemplatesDir + dfNativeVM),
                entry(dockerPath + "Dockerfile.native-micro",
                        dockerTemplatesDir + dfNativeMicroVM));

        FileWriter fileWriter;
        Template t;

        for (String path : files.keySet()) {
            FileUtils.createParentDirectories(new File(path));
            fileWriter = new FileWriter(path);
            t = engine.getTemplate(files.get(path));
            t.merge(context, fileWriter);
            fileWriter.flush();
            fileWriter.close();
        }
    }

    /**
     * Adds the function trigger handler to the generated project.
     * 
     * @param function
     * @param codePath path to the code directory
     * @throws IOException
     */
    private void writeHandler(LinkedFunction function, String codePath) throws IOException {
        // create dir
        File handlerFile = new File(codePath + TemplatingConstants.HANDLER_NAME);
        FileUtils.forceMkdirParent(handlerFile);

        FileWriter fileWriter = new FileWriter(handlerFile);
        Template t;

        if (function.requiresGlue()) {
            t = engine.getTemplate(triggerTemplatesDir + forglueHandlerVM);
        } else {
            switch (function.getTriggerType()) {
                case HTTP:
                    if (function.getTrigger() == null) {
                        t = engine.getTemplate(triggerTemplatesDir + functionUrlHandlerVM);
                    } else {
                        t = engine.getTemplate(triggerTemplatesDir + restHandlerVM);
                    }
                    break;
                case QUEUE:
                    t = engine.getTemplate(triggerTemplatesDir + queueHandlerVM);
                    break;
                case TIMER:
                default:
                    fileWriter.close();
                    throw new IllegalArgumentException("Unsupported trigger: " + function.getTriggerType());
            }
        }

        setDatabaseSourcesInContext(function);

        t.merge(context, fileWriter);
        fileWriter.flush();
        fileWriter.close();
    }

    /**
     * Modifies the pom by adding necessary tags for the maven build.
     * <p>
     * NOTE this process would be more robust if the command 'quarkus ext add' were
     * to be used instead.
     * 
     * @param pomPath                path to the generated pom
     * @param addDBDependencies      whether to add dependencies to interact with
     *                               databases
     * @param addJacksonDependencies whether to add dependencies for exchange
     *                               deserialization
     * @param addJwtDependencies     whether to add dependencies for Java JWT
     * @throws IOException
     */
    private void extendPom(String rootPath, boolean addDBDependencies,
            boolean addJacksonDependencies, boolean addJwtDependencies) throws IOException {
        File pom = new File(rootPath + TemplatingConstants.POM_NAME);
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
        boolean inDependencyMgmt = false;

        for (String line; (line = reader.readLine()) != null;) {
            if (line.contains("<dependencyManagement>"))
                inDependencyMgmt = true;
            if (line.contains("</dependencyManagement>"))
                inDependencyMgmt = false;
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

            if (inDependencyMgmt) {
                modifiedPomContent.add(line);
                continue;
            }

            // if line is model version, append parent tag
            if (line.trim().startsWith("<modelVersion>")) {
                modifiedPomContent.addAll(parentTag);
            } else if (line.trim().startsWith("</dependencies>") && !inDependencyMgmt) {
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

                if (addJacksonDependencies && !alreadyContainsJackson) {
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
     * Writes a properties file containing info about resources this function is
     * linked to, which
     * will be used by Apache Camel components.
     * 
     * @param function
     * @param appPropertiesPath path to where the properties file needs to be
     *                          written
     * @throws IOException
     */
    private void writeProperties(LinkedFunction function, String resourcesPath) throws IOException {

        File appPropeties = new File(resourcesPath + TemplatingConstants.APP_PROPERTIES_NAME);
        if (!appPropeties.exists()) {
            FileUtils.forceMkdirParent(appPropeties);
        }

        // add outputs
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

        // add inputs
        LinkedQueue inputQ = function.getInputQueue();
        if (inputQ != null) {
            FileUtils.writeStringToFile(appPropeties,
                    String.format(TemplatingConstants.QUEUE_PROPERTY, inputQ.getName(), function.getProvider()), "utf8",
                    true);
        }
        // provider of this function
        FileUtils.writeStringToFile(appPropeties,
                String.format(TemplatingConstants.FUNCTION_PROPERTY, "trigger", function.getProvider()), "utf8", true);

    }

    /**
     * Same as {@link #setContext(LinkedFunction) setContext} but for a glue
     * function.
     * 
     * @throws IOException
     */
    private String setGlueContext(DeployableFunction function) throws IOException {
        String glueFunctionName = function.getName() + "-glue";
        String cloudGlueName;
        String memoryName = namesMemory.get(glueFunctionName);
        if (memoryName != null) {
            cloudGlueName = memoryName;
        } else {
            cloudGlueName = function.getCloudName() + "-glue";
            namesMemory.addNewMemory(function.getName() + "-glue", cloudGlueName);
        }

        context.put("functionName", glueFunctionName);
        if (function.getFunction().getInputQueue() != null)
            context.put("queueName", function.getFunction().getInputQueue().getName());

        context.put("targetFunction", function.getName().replace("-", "_"));

        return cloudGlueName;
    }

    /**
     * Generates the project for the glue function from the maven archetype for Java
     * Lambda
     * functions.
     * 
     * @param functionName name of the glue function
     * @throws IOException
     * @throws InterruptedException
     */
    private void generateGlueArchetype(String functionName)
            throws IOException, InterruptedException {
        // TODO platform dependent!
        String archetypeCmd = "cmd.exe /c cd %s & " + "mvn io.quarkus.platform:quarkus-maven-plugin:3.5.1:create "
                + "-DprojectGroupId=%s -DprojectArtifactId=%s -DprojectVersion=1.0";

        String cmd = String.format(archetypeCmd, outputDir, TemplatingConstants.PACKAGE_NAME, functionName);

        Process p = Runtime.getRuntime().exec(cmd);
        if (p.waitFor() != 0) {
            throw new Error("Maven archetype generation for quarkus aws lambda failed.");
        }
    }

    /**
     * Deletes unnecessary files from the generated Manen Lambda archetype.
     * 
     * @param rootDir path to the root directory of the generated project
     * @param codeDir path to the code directory
     * @param testDir path to the test directory
     * @throws IOException
     */
    private void clenaupGlueArchetype(String rootDir, String codeDir, String testDir)
            throws IOException {
        // delete handler and pom
        FileUtils.delete(new File(codeDir + "GreetingResource.java"));
        FileUtils.delete(new File(rootDir + TemplatingConstants.POM_NAME));

        // clean test folder
        File testDirectory = new File(testDir);
        FileUtils.deleteDirectory(testDirectory);
        testDirectory.mkdir();
    }

    /**
     * Writes the function trigger handler for the glue function generated project.
     * 
     * @param codeDir path to the code directory of the generated project
     * @throws IOException
     */
    private void writeGlueHandler(String codeDir) throws IOException {
        FileWriter fileWriter = new FileWriter(codeDir + TemplatingConstants.HANDLER_NAME);
        Template t = engine.getTemplate(glueTemplatesDir + glueHandlerVM);

        t.merge(context, fileWriter);
        fileWriter.flush();
        fileWriter.close();
    }

    /**
     * Writes the pom for the glue function generated project.
     * 
     * @param rootDir path to the root directory of the generated project
     * @throws IOException
     */
    private void writeGluePom(String rootDir) throws IOException {
        FileWriter fileWriter = new FileWriter(rootDir + TemplatingConstants.POM_NAME);
        Template t = engine.getTemplate(glueTemplatesDir + gluePomVM);

        t.merge(context, fileWriter);
        fileWriter.flush();
        fileWriter.close();
    }

    /**
     * Builds the DataSource producers for the output databases to be put in trigger
     * handler template.
     * 
     * @param function
     */
    private void setDatabaseSourcesInContext(LinkedFunction function) {
        if (!function.hasDatabaseOutput()) {
            context.put("dbImports", "");
            context.put("databaseSources", "");
        } else {
            List<String> databases = new ArrayList<>();
            for (LinkedResource lr : function.getOutput()) {
                if (lr instanceof LinkedDatabase) {
                    databases.add(lr.getName());
                }
            }
            StringJoiner sj = new StringJoiner("\n");
            for (String dbName : databases) {
                sj.add(String.format(TemplatingConstants.AWS_DB_DATASOURCE_BLOCK, dbName, dbName,
                        dbName, dbName, dbName));
            }

            context.put("dbImports", TemplatingConstants.AWS_DB_IMPORTS);
            context.put("databaseSources", sj.toString());
        }
    }

}
