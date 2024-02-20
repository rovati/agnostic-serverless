package ch.elca.rovl.dsl.pipeline.infraparsing.resource;

/**
 * Abstract object representing a cloud resource.
 */
public abstract class DslResource {
    protected final String name;

    protected DslResource(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
