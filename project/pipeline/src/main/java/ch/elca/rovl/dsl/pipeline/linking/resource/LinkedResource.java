package ch.elca.rovl.dsl.pipeline.linking.resource;

import ch.elca.rovl.dsl.pipeline.infraparsing.resource.DslResource;
import ch.elca.rovl.dsl.pipeline.util.Provider;

/**
 * Abstract object representing a cloud resource with its target provider chosen by the user.
 */
public abstract class LinkedResource {
    final DslResource dslResource;
    final Provider provider;

    public LinkedResource(DslResource dslResource, Provider provider) {
        this.dslResource = dslResource;
        this.provider = provider;
    }

    public String getName() { return dslResource.getName(); }
    public Provider getProvider() { return provider; }
}
