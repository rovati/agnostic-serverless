package ch.elca.rovl.dsl.pipeline.deployment;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import ch.elca.rovl.dsl.pipeline.deployment.helper.AwsDeploymentHelper;
import ch.elca.rovl.dsl.pipeline.deployment.helper.AzureDeploymentHelper;
import ch.elca.rovl.dsl.pipeline.deployment.helper.DeploymentHelper;
import ch.elca.rovl.dsl.pipeline.util.Provider;

/**
 * Factory of {@link DeploymentHelper deployment helpers}.
 */
public final class DeploymentHelperFactory {
    final Map<Provider, DeploymentHelper> helpers;

    DeploymentHelperFactory() {
        this.helpers = new HashMap<>();
    }

    /**
     * Returns the deployment helper for the given provider. Creates a new helper if it doesn't
     * exist already.
     * 
     * @param provider
     * @return deployment helper for the given provider
     * @throws IOException
     * @throws URISyntaxException
     */
    DeploymentHelper getHelper(Provider provider) throws IOException, URISyntaxException {
        DeploymentHelper helper = helpers.get(provider);
        if (helper == null) {
            switch (provider) {
                case AZURE:
                    helper = new AzureDeploymentHelper();
                    break;
                case AWS:
                    helper = new AwsDeploymentHelper();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported provider: " + provider);
            }

            helpers.put(provider, helper);
        }

        return helper;
    }

    void close() {
        for (DeploymentHelper dh : helpers.values()) {
            dh.close();
        }
    }
}
