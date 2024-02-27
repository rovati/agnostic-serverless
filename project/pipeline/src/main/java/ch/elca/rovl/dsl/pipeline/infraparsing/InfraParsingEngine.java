package ch.elca.rovl.dsl.pipeline.infraparsing;

import java.util.HashMap;
import java.util.Map;
import ch.elca.rovl.dsl.PlatformResourcesDefinition;
import ch.elca.rovl.dsl.api.database.FluentDatabaseBuilder;
import ch.elca.rovl.dsl.api.function.FluentFunctionBuilder;
import ch.elca.rovl.dsl.api.queue.FluentQueueBuilder;
import ch.elca.rovl.dsl.pipeline.infraparsing.resource.DslDatabase;
import ch.elca.rovl.dsl.pipeline.infraparsing.resource.DslFunction;
import ch.elca.rovl.dsl.pipeline.infraparsing.resource.DslQueue;
import ch.elca.rovl.dsl.registry.SystemRegistry;

/**
 * Engine that takes care of parsing the infrastructure layout defined by the user.
 */
public class InfraParsingEngine {

    private final PlatformResourcesDefinition definition;
    private final Map<String, DslFunction> functions;
    private final Map<String, DslQueue> queues;
    private final Map<String, DslDatabase> databases;

    public InfraParsingEngine(PlatformResourcesDefinition definition) {
        this.definition = definition;
        this.functions = new HashMap<>();
        this.queues = new HashMap<>();
        this.databases = new HashMap<>();

        // set registry and register layout resources
        this.definition.define();
    }

    /**
     * Validates the definition of each resource, and then transforms them into resources usable by
     * the subsequent steps of the pipeline.
     */
    public void parseResources() {
        SystemRegistry sysReg = definition.getSystemRegistry();

        // parse and validate function definitions
        Map<String, FluentFunctionBuilder> functionBuilders = sysReg.getFunctionBuilders();
        for (String fnName : functionBuilders.keySet()) {
            if (fnName.contains("-")) {
                throw new IllegalStateException("Resource names cannot contain '-': " + fnName);
            }
            FluentFunctionBuilder ffb = functionBuilders.get(fnName);
            ffb.validate();
            functions.put(fnName, DslFunction.fromApiFunction(ffb.build()));
        }

        // parse and validate queue definitions
        Map<String, FluentQueueBuilder> queueBuilders = sysReg.getQueueBuilders();
        for (String qName : queueBuilders.keySet()) {
            if (qName.contains("-")) {
                throw new IllegalStateException("Resource names cannot contain '-': " + qName);
            }
            FluentQueueBuilder fqb = queueBuilders.get(qName);
            fqb.validate();
            queues.put(qName, DslQueue.fromApiQueue(fqb.build()));
        }

        // parse and validate databases
        Map<String, FluentDatabaseBuilder> dbBuilders = sysReg.getDatabaseBuilders();
        for (String dbName : dbBuilders.keySet()) {
            if (dbName.contains("-")) {
                throw new IllegalStateException("Resource names cannot contain '-': " + dbName);
            }
            FluentDatabaseBuilder fdb = dbBuilders.get(dbName);
            fdb.validate();
            databases.put(dbName, DslDatabase.fromApiDatabase(fdb.build()));
        }

    }

    /**
     * Returns a map {functionName: function} with the user definitions.
     * @return functions map
     */
    public Map<String, DslFunction> getFunctions() {
        return functions;
    }

    /**
     * Returns a map {queueName: queue} with the user definitions.
     * @return queues map
     */
    public Map<String, DslQueue> getQueues() {
        return queues;
    }

    /**
     * Returns a map {databaseName: database} with the user definitions.
     * @return databases map
     */
    public Map<String, DslDatabase> getDatabases() {
        return databases;
    }
    
}
