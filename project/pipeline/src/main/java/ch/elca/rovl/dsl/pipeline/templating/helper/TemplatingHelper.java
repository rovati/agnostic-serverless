package ch.elca.rovl.dsl.pipeline.templating.helper;

import java.io.IOException;

import ch.elca.rovl.dsl.pipeline.linking.resource.LinkedFunction;
import ch.elca.rovl.dsl.pipeline.templating.resource.DeployableFunction;

/**
 * Interface offering helper methods for the generation of function projects to
 * be deployed on
 * target providers.
 */
public interface TemplatingHelper {

    /**
     * Generates a deployable project for the given function.
     * 
     * @param function
     * @return pipeline object for the next step
     * @throws IOException
     */
    DeployableFunction generateFunction(LinkedFunction function) throws IOException;

    /**
     * Generates a deployable project for a "glue" function: a function that is not
     * defined by the user but that is necessary for the application to work once
     * deployed.
     * 
     * @param function function the "glue" function will interact with
     * @return pipeline object for the next step
     * @throws IOException
     * @throws InterruptedException
     */
    DeployableFunction generateGlue(DeployableFunction function)
            throws IOException, InterruptedException;
}
