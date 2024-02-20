package ch.elca.rovl.dsl.pipeline.deployment.resource;

import ch.elca.rovl.dsl.pipeline.deployment.accessinfo.AccessInfo;
import ch.elca.rovl.dsl.pipeline.templating.resource.DeployableResource;
import ch.elca.rovl.dsl.pipeline.util.Provider;

/**
 * Abstract object representing a deployed cloud resource with its target provider chosen by the user,
 * infomration on how to access the resource and permissions to do so.
 */
public abstract class DeployedResource {
    final DeployableResource deployableResource;
    AccessInfo accessInfo;

    DeployedResource(DeployableResource deployableResource) {
        this.deployableResource = deployableResource;
    }

    public void setAccessInfo(AccessInfo info) {
        this.accessInfo = info;
    }

    public String getName() {
        return deployableResource.getName();
    }

    public Provider getProvider() {
        return deployableResource.getProvider();
    }

    public AccessInfo getAccessInfo() {
        return accessInfo;
    }

    public String getCloudName() {
        return deployableResource.getCloudName();
    }
}
