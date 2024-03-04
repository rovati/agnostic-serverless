package ch.elca.rovl.dsl.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ch.elca.rovl.dsl.api.database.FluentDatabaseBuilder;
import ch.elca.rovl.dsl.api.function.FluentFunctionBuilder;
import ch.elca.rovl.dsl.api.queue.FluentQueueBuilder;

/**
 * System registry containing builders for the defined resources.
 */
public final class SystemRegistry {

    private final List<String> names;
    private final Map<String, FluentFunctionBuilder> functionBuilders;
    private final Map<String, FluentQueueBuilder> queueBuilders;
    private final Map<String, FluentDatabaseBuilder> databaseBuilders;

    public SystemRegistry() {
        names = new ArrayList<>();
        functionBuilders = new HashMap<>();
        queueBuilders = new HashMap<>();
        databaseBuilders = new HashMap<>();
    }

    /**
     * Registers a new function builder. Checks that the given name is not already
     * used by another resource.
     * 
     * @param functionName
     * @param builder
     */
    public void registerFunction(String functionName, FluentFunctionBuilder builder) {
        if (names.contains(functionName)) {
            throw new IllegalArgumentException("Resource name already used: " + functionName);
        } else {
            names.add(functionName);
            functionBuilders.put(functionName, builder);
        }
    }

    /**
     * Registers a new queue builder. Checks that the given name is not already used
     * by another resource.
     * 
     * @param queueName
     * @param builder
     */
    public void registerQueue(String queueName, FluentQueueBuilder builder) {
        if (names.contains(queueName)) {
            throw new IllegalArgumentException("Resource name already used: " + queueName);
        } else {
            names.add(queueName);
            queueBuilders.put(queueName, builder);
        }
    }

    /**
     * Registers a new database builder. Checks that the given name is not already
     * used by another resource.
     * 
     * @param databaseName
     * @param builder
     */
    public void registerDatabase(String databaseName, FluentDatabaseBuilder builder) {
        if (names.contains(databaseName)) {
            throw new IllegalArgumentException("Resource name already used: " + databaseName);
        } else {
            names.add(databaseName);
            databaseBuilders.put(databaseName, builder);
        }
    }

    public FluentFunctionBuilder getFunctionBuilder(String name) {
        return functionBuilders.get(name);
    }

    public FluentQueueBuilder getQueueBuilder(String name) {
        return queueBuilders.get(name);
    }

    public FluentDatabaseBuilder getDatabaseBuilder(String name) {
        return databaseBuilders.get(name);
    }

    public Map<String, FluentFunctionBuilder> getFunctionBuilders() {
        return functionBuilders;
    }

    public Map<String, FluentQueueBuilder> getQueueBuilders() {
        return queueBuilders;
    }

    /**
     * Gets all registered builders.
     * 
     * @return map {resource_name -> builder} of all registered builders
     */
    public Map<String, FluentDatabaseBuilder> getDatabaseBuilders() {
        return databaseBuilders;
    }

}
