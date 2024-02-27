package ch.elca.rovl.dsl.resource;

/**
 * Abstract class representing a platform resource.
 */
public abstract class Resource {

    // unique name of the resource
    final protected String name;

    protected Resource(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    
}
