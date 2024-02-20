package ch.elca.rovl.dsl.pipeline.linking.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import ch.elca.rovl.dsl.pipeline.infraparsing.resource.DslFunction;
import ch.elca.rovl.dsl.pipeline.linking.LinkerEngine;
import ch.elca.rovl.dsl.pipeline.linking.model.Link;
import ch.elca.rovl.dsl.pipeline.util.Constants;
import ch.elca.rovl.dsl.pipeline.util.PackageExtractor;
import ch.elca.rovl.dsl.pipeline.util.ResourceType;
import ch.elca.rovl.dsl.pipeline.util.ResourceTypeParser;

/**
 * Helper class to {@link LinkerEngine LinkerEngine} that deduces links from the code project of a
 * function.
 */
public class DeducerHelper {

    final String deductionDir;
    final String routeBuilderName;
    List<String> execPlugin;

    final VelocityContext context;
    final VelocityEngine engine;

    /**
     * Constructor.
     * 
     * @param deductionDir directory containing the generated functions to run the deduction on
     * @param routeBuilderName name of the Apache Camel route builder file
     * @param context templating context
     * @param engine templating engine
     */
    public DeducerHelper(String deductionDir, String routeBuilderName, VelocityContext context,
            VelocityEngine engine) {
        this.deductionDir = deductionDir;
        this.routeBuilderName = routeBuilderName;
        this.context = context;
        this.engine = engine;

        // create log dir if it dosen't exist
        File logDirectory = new File(Constants.LOGS_DIR);
        if (!logDirectory.exists()) {
            logDirectory.mkdirs();
        }
    }

    /**
     * Copies a function project to a temporary directory.
     * 
     * @param function
     * @return path to the project root directory in the temporary directory
     * @throws IOException
     */
    public String copyProject(DslFunction function) throws IOException {
        String path = deductionDir + function.getName() + "/";
        File generatedDir = new File(path);
        if (generatedDir.exists())
            throw new IllegalStateException(String.format(
                    "Error generating function code, dir for function '%s' already exists.", path));

        FileUtils.copyDirectory(new File(function.getPathToProject() + function.getName()),
                generatedDir);

        return path;
    }

    // TODO use classgraph to get all file extending RouteBuilder, read all of them and filter for
    // "queue" or "function"
    /**
     * Adds into the function project the Java class to be run in order to extract links info from
     * the Apache Camel route builder.
     * 
     * @param rootDir path to the project root directory in the temporary directory
     * @throws IOException
     */
    public void addEndpointExtractor(String rootDir) throws IOException {
        // look for routebuilder file in project
        File codeDir = new File(rootDir + "src/main/java/");
        Collection<File> matchedFiles = FileUtils.listFiles(codeDir,
                new NameFileFilter(routeBuilderName), TrueFileFilter.INSTANCE);
        if (matchedFiles.size() < 1)
            throw new IllegalStateException("Could not find FunctionRoute.java file in project");
        // NOTE we should not worry about this case
        if (matchedFiles.size() > 1)
            throw new IllegalStateException(
                    "Function project contains multiple FunctionRoute.java files!");

        // get its package
        File functionRouteFile = matchedFiles.iterator().next();
        String packageLine = PackageExtractor.getFilePackage(functionRouteFile);

        // add enpoint extractor
        context.put("package", packageLine);
        File endpointExtractorFile =
                new File(functionRouteFile.getParent() + "/EndpointExtractor.java");
        FileWriter fileWriter = new FileWriter(endpointExtractorFile);
        Template t = engine.getTemplate("vtemplates/linking/endpoint-ext.vm");
        t.merge(context, fileWriter);
        fileWriter.flush();
        fileWriter.close();

        String functionPackage = packageLine.trim().replace("package ", "").replace(";", "");

        // format tag with endpoint extractor location
        execPlugin = Arrays.asList("\t\t\t<plugin>", "\t\t\t\t<groupId>org.codehaus.mojo</groupId>",
                "\t\t\t\t<artifactId>exec-maven-plugin</artifactId>",
                "\t\t\t\t<version>3.1.0</version>", "\t\t\t\t<goals>",
                "\t\t\t\t\t<goal>java</goal>", "\t\t\t\t</goals>", "\t\t\t\t<configuration>",
                String.format("\t\t\t\t\t<mainClass>%s</mainClass>",
                        functionPackage + ".EndpointExtractor"),
                "\t\t\t\t\t<cleanupDaemonThreads>false</cleanupDaemonThreads>",
                "\t\t\t\t</configuration>", "\t\t\t</plugin>");
    }

    // TODO see if can be done with mvn command instead
    /**
     * Modifies the project pom file to add required dependencies and plugins to run the link
     * deducer.
     * 
     * @param rootDir path to the project root directory in the temporary directory
     * @throws IOException
     */
    public void extendPom(String rootDir) throws IOException {
        File pomFile = new File(rootDir + "pom.xml");
        List<String> extendedPom = new ArrayList<>();

        FileReader fReader = new FileReader(pomFile);
        BufferedReader reader = new BufferedReader(fReader);

        boolean alreadyContainsExecPlugin = false;
        boolean alreadyContainsDBComponent = false;

        for (String line; (line = reader.readLine()) != null;) {
            // if end of plugins section is reached and exec plugin is not present, add it
            if (line.trim().startsWith("</plugins>") && !alreadyContainsExecPlugin) {
                // add plugin code
                extendedPom.addAll(execPlugin);
            }

            if (line.contains("<groupId>ch.elca.rovl.databasecomponent</groupId>")) {
                alreadyContainsDBComponent = true;
            }

            if (line.contains("<artifactId>exec-maven-plugin</artifactId>")) {
                alreadyContainsExecPlugin = true;
                break;
            }

            extendedPom.add(line);
        }

        fReader.close();
        reader.close();

        // if at least one of the required was missing, overwrite the pom file
        if (!alreadyContainsExecPlugin || !alreadyContainsDBComponent)
            FileUtils.writeLines(pomFile, extendedPom);
    }

    /**
     * Runs the link deducer to extract Apache Camel producer and consumer endpoints from the route
     * builder.
     * 
     * @param rootDir path to the project root directory in the temporary directory
     * @param functionName name of the function
     * @return {@code File} instance of the file containing the exctracted input and output
     *         resources of this function
     */
    public File extractEndpoints(String rootDir, String functionName) {
        // maven command to build and run the project
        String mavenCmd = " & mvn clean install exec:java";
        Process p;
        int cmdResult;
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "cd " + rootDir, mavenCmd);
            // write output to log file
            pb.redirectOutput(new File(Constants.LOGS_DIR + "deducelinks-" + functionName + ".txt"));

            // run command
            p = pb.start();
            cmdResult = p.waitFor();
            if (cmdResult != 0) {
                throw new Error("Endpoint extraction failed.");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(String.format(
                    "Failed to run endpoint extraction command for function '%s'", functionName));
        }

        // return file containing endpoints
        return new File(rootDir + "endpoints.txt");
    }

    /**
     * Creates links of a function from the information extracted from its code and returns them.
     * 
     * @param functionName name of the function
     * @param endpointsFile file containing the extracted input and output resources
     * @return list of links
     * @throws IOException
     */
    public List<Link> createLinks(String functionName, File endpointsFile) throws IOException {
        List<Link> links = new ArrayList<>();

        FileReader fReader = new FileReader(endpointsFile);
        BufferedReader reader = new BufferedReader(fReader);

        // for each line in the extracted resources file, get the resource name and whether it is an
        // input or output resource
        for (String line; (line = reader.readLine()) != null;) {
            String[] split = line.split("=");
            if (split.length != 2) {
                reader.close();
                fReader.close();
                throw new IllegalStateException("Malformed endpoints file for " + functionName);
            }

            String[] uri = split[1].split(":");

            if (split[0].equals("input") && !uri[0].equals("direct")) {
                // case "extracted -> function" link
                // if input is function, do not create link (already created by another function)
                if (!uri[0].equals("function")) {
                    links.add(new Link(uri[1], ResourceTypeParser.parseType(uri[0]), functionName,
                            ResourceType.FUNCTION));
                }
            } else if (line.startsWith("output=")) {
                // case "function -> extracted" link
                links.add(new Link(functionName, ResourceType.FUNCTION, uri[1],
                        ResourceTypeParser.parseType(uri[0])));
            }
        }

        reader.close();
        fReader.close();

        return links;
    }

}
