package ch.elca.rovl.dsl.pipeline.templating.helper;

import java.io.IOException;

import ch.elca.rovl.dsl.pipeline.linking.resource.LinkedFunction;
import ch.elca.rovl.dsl.pipeline.templating.resource.DeployableFunction;

/**
 * Interface offering helper methods for the generation of function projects to be deployed on
 * target providers.
 */
public interface TemplatingHelper {
    
    DeployableFunction generateFunction(LinkedFunction function) throws IOException;

    DeployableFunction generateGlue(DeployableFunction function)
            throws IOException, InterruptedException;
}
