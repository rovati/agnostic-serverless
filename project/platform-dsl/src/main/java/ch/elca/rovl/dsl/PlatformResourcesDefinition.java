package ch.elca.rovl.dsl;

import ch.elca.rovl.dsl.api.database.DatabaseEntryPoint;
import ch.elca.rovl.dsl.api.function.FunctionEntryPoint;
import ch.elca.rovl.dsl.api.queue.QueueEntryPoint;
import ch.elca.rovl.dsl.registry.SystemRegistry;

/**
 * Class containing the definition of cloud resources needed by the user for the serverless
 * application.
 * <p>
 * This class is intended to be extended by the user who will be implementing the {@link #define()
 * define} method.
 */
public abstract class PlatformResourcesDefinition {

    final SystemRegistry sysRegistry;

    protected PlatformResourcesDefinition() {
        sysRegistry = new SystemRegistry();
    }

    /**
     * Defines the cloud resources requested by the user for the serverless application.
     */
    public abstract void define();

    /**
     * Entry point for the fluent definition of a function resource.
     * 
     * @param name unique name of the function
     * @return the next step of the fluent definition
     */
    protected FunctionEntryPoint function(String name) {
        return new FunctionEntryPoint(sysRegistry, name);
    }

    /**
     * Entry point for the fluent definition of a queue resource.
     * 
     * @param name unique name of the queue
     * @return the next step of the fluent definition
     */
    protected QueueEntryPoint queue(String name) {
        return new QueueEntryPoint(sysRegistry, name);
    }

    /**
     * Entry point for the fluent definition of a database resource.
     * 
     * @param name unique name of the database
     * @return the next step of the fluent definition
     */
    protected DatabaseEntryPoint database(String name) {
        return new DatabaseEntryPoint(sysRegistry, name);
    }


    /**
     * Used by the pipeline to retrieve the defined resources.
     * 
     * @return system registry containing the defined resources
     */
    public SystemRegistry getSystemRegistry() {
        return sysRegistry;
    }
}
