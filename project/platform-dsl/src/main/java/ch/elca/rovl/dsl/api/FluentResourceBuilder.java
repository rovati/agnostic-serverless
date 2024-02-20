package ch.elca.rovl.dsl.api;

import ch.elca.rovl.dsl.resource.Resource;

public interface FluentResourceBuilder {
    /**
     * Validates the configuration fields set in the builder.
     */
    public void validate();

    /**
     * Creates an resource object that can be used in the pipeline, from the fields set in the
     * builder.
     * 
     * @param <T> type of the resource
     * @return resource object to be used in the pipeline
     */
    public <T extends Resource> T build();
}
