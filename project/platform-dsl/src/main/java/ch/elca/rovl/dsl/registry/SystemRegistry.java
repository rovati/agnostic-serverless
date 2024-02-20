package ch.elca.rovl.dsl.registry;

import java.util.HashMap;
import java.util.Map;
import ch.elca.rovl.dsl.api.database.FluentDatabaseBuilder;
import ch.elca.rovl.dsl.api.function.FluentFunctionBuilder;
import ch.elca.rovl.dsl.api.queue.FluentQueueBuilder;

public final class SystemRegistry {

    private final Map<String, FluentFunctionBuilder> functionBuilders;
    private final Map<String, FluentQueueBuilder> queueBuilders;
    private final Map<String, FluentDatabaseBuilder> databaseBuilders;

    public SystemRegistry() {
        functionBuilders = new HashMap<>();
        queueBuilders = new HashMap<>();
        databaseBuilders = new HashMap<>();
    }

    public boolean registerFunction(String functionName, FluentFunctionBuilder builder) {
        return functionBuilders.put(functionName, builder) != null;
    }

    public boolean registerQueue(String queueName, FluentQueueBuilder builder) {
        return queueBuilders.put(queueName, builder) != null;
    }

    public boolean registerDatabase(String databaseName, FluentDatabaseBuilder builder) {
        return databaseBuilders.put(databaseName, builder) != null;
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

    public Map<String, FluentDatabaseBuilder> getDatabaseBuilders() {
        return databaseBuilders;
    }

}
