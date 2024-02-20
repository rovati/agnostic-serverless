package ch.elca.rovl.dsl.pipeline.templating.resource;

import ch.elca.rovl.dsl.pipeline.linking.resource.LinkedResource;
import ch.elca.rovl.dsl.pipeline.util.Provider;

/**
 * Abstract object representing a cloud resource with its target provider chosen by the user and the
 * name of the corresponding resource in the cloud.
 */
public abstract class DeployableResource {

    final LinkedResource linkedResource;
    String cloudName;

    DeployableResource(LinkedResource linkedResource) {
        this.linkedResource = linkedResource;
        this.cloudName = linkedResource.getName();
    }

    public void setCloudName(String cloudName) {
        this.cloudName = cloudName;
    }

    public String getName() {
        return linkedResource.getName();
    }

    public String getCloudName() {
        return cloudName;
    }

    public Provider getProvider() {
        return linkedResource.getProvider();
    }

}
