package ch.elca.rovl.dsl.pipeline.templating;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import ch.elca.rovl.dsl.pipeline.templating.helper.AwsTemplatingHelper;
import ch.elca.rovl.dsl.pipeline.templating.helper.AzureTemplatingHelper;
import ch.elca.rovl.dsl.pipeline.templating.helper.TemplatingHelper;
import ch.elca.rovl.dsl.pipeline.util.Provider;

/**
 * Factory of {@link TemplatingHelper templating helpers}.
 */
public final class TemplatingHelperFactory {

    final Map<Provider, TemplatingHelper> helpers;
    final VelocityEngine engine;
    final VelocityContext context;
    final String outputDir;
    final String packageName;
    final ResourceNameMemory memory;

    /**
     * Factory constructor.
     * 
     * @param outputDir root directory where the generated projects are placed
     * @param packageName name of the functions package
     * @param memory data strcture containing names of resources that are already deployed
     */
    public TemplatingHelperFactory(String outputDir, String packageName,
            ResourceNameMemory memory) {
        this.outputDir = outputDir;
        this.packageName = packageName;
        this.memory = memory;
        this.helpers = new HashMap<>();

        // init velocity engine
        engine = new VelocityEngine();
        engine.setProperty(RuntimeConstants.RESOURCE_LOADERS, "classpath");
        engine.setProperty("resource.loader.classpath.class",
                ClasspathResourceLoader.class.getName());
        engine.init();

        context = new VelocityContext();
        context.put("package", packageName);
    }

    /**
     * Returns the helper for the given provider. It creates it if not already existing.
     * 
     * @param provider
     * @return TemplatingHelper for the given provider
     * @throws IOException
     */
    public TemplatingHelper getHelper(Provider provider) throws IOException {
        TemplatingHelper helper = helpers.get(provider);
        if (helper == null) {
            switch (provider) {
                case AZURE:
                    helper = new AzureTemplatingHelper(engine, context, outputDir, packageName,
                            memory);
                    break;
                case AWS:
                    helper = new AwsTemplatingHelper(engine, context, outputDir, packageName,
                            memory);
                    break;
                default:
                    throw new IllegalArgumentException("Provider is not supported");
            }

            helpers.put(provider, helper);
        }

        return helper;
    }
}
