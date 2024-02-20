package ch.elca.rovl.dsl.resource;

public abstract class Resource {

    final protected String name;

    protected Resource(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    
}
