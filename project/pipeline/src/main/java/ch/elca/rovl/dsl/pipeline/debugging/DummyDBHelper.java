package ch.elca.rovl.dsl.pipeline.debugging;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import ch.elca.rovl.dsl.pipeline.util.Constants;

/**
 * Helper class used to generate dummy Quarkus projects. The projects can be run
 * in dev mode to trigger Quarkus Dev Services and provision local databases.
 */
public class DummyDBHelper {

    private final String pomVM = "pom.vm";
    private final String pomName = "pom.xml";
    private final String routeVM = "route.vm";
    private final String routeName = "MyRoute.java";
    private final String propsVM = "props.vm";
    private final String propsName = "application.properties";
    private final String dummyDir = Constants.DEBUGGING_DIR + "dummy-dbs/";
    private final String codeDir = "src/main/java/org/acme/";
    private final String resourcesDir = "src/main/resources/";
    private final String templatesDir = "vtemplates/debugging/";

    private final VelocityEngine engine;
    private final VelocityContext context;

    DummyDBHelper() {
        this.engine = new VelocityEngine();
        this.context = new VelocityContext();
        engine.setProperty(RuntimeConstants.RESOURCE_LOADERS, "classpath");
        engine.setProperty("resource.loader.classpath.class",
                ClasspathResourceLoader.class.getName());
        engine.init();

        // create output dir if not exists
        File dummyDbsDir = new File(dummyDir);
        if (!dummyDbsDir.exists()) {
            // create dir
            dummyDbsDir.mkdirs();
        }
    }

    String createDummyProject(String dbName, int port) throws IOException {
        // create project dir
        String projectDir = dummyDir + dbName + "/";
        (new File(projectDir)).mkdir();
        (new File(projectDir + codeDir)).mkdirs();
        (new File(projectDir + resourcesDir)).mkdirs();

        context.put("dbname", dbName);
        context.put("port", port);

        // write files
        FileWriter fw = new FileWriter(projectDir + pomName);
        engine.getTemplate(templatesDir + pomVM).merge(context, fw);
        fw.flush();
        fw.close();
        fw = new FileWriter(projectDir + codeDir + routeName);
        engine.getTemplate(templatesDir + routeVM).merge(context, fw);
        fw.flush();
        fw.close();
        fw = new FileWriter(projectDir + resourcesDir + propsName);
        engine.getTemplate(templatesDir + propsVM)
                .merge(context, fw);
        fw.flush();
        fw.close();

        return projectDir;
    }

}
