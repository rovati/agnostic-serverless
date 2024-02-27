package ch.elca.rovl.dsl.pipeline.infraparsing.resource;

/**
 * Abstract object representing a cloud resource.
 */
public abstract class DslResource {
    protected final String name;

    protected DslResource(String name) {
        // check validty of the name
        if (!name.matches("[a-zA-Z0-9_]+"))
            throw new IllegalArgumentException(
                    String.format("Name of the resource '%s' has to match regex [a-zA-Z0-9_]+", name));
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
