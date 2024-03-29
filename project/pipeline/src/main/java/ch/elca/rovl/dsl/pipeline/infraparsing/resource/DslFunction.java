package ch.elca.rovl.dsl.pipeline.infraparsing.resource;

import java.util.HashMap;
import java.util.Map;
import ch.elca.rovl.dsl.api.function.models.FunctionRuntime;
import ch.elca.rovl.dsl.resource.function.Function;
import ch.elca.rovl.dsl.resource.function.Function.FunctionConfigType;


/**
 * Object representing a cloud function. It contains configuration specified through the
 * infrastructure DSL.
 */
public final class DslFunction extends DslResource {

    private final String pathToProject;
    private final FunctionRuntime runtime;
    private final String handler;
    Map<FunctionConfigType, Object> config;

    /**
     * Object constructor. The parameters are mandatory configuration required to deploy the
     * database on any provider.
     * 
     * @param name unique name of the function
     * @param pathToProject path to the (parent of the) directory containing the project with the
     *        code for this function
     * @param runtime runtime and version for this function
     * @param handler fully qualified name of the class containing the Apache Camel route definition
     */
    public DslFunction(String name, String pathToProject, FunctionRuntime runtime, String handler) {
        super(name);

        this.pathToProject = pathToProject;
        this.runtime = runtime;
        this.handler = handler;
        this.config = new HashMap<>();
    }

    public static DslFunction fromApiFunction(Function fn) {
        DslFunction dslFn = new DslFunction(fn.getName(), fn.getProjectPath(), fn.getRuntime(), fn.getHandler());
        dslFn.setConfig(fn.getConfig());
        return dslFn;
    }

    /**
     * Sets function configuration.
     * 
     * @param config map of configuration values
     */
    public void setConfig(Map<FunctionConfigType,Object> config) {
        this.config = config;
    }

    public String getPathToProject() {
        return pathToProject;
    }

    public FunctionRuntime getRuntime() {
        return runtime;
    }

    public String getHandler() {
        return handler;
    }

    public Map<FunctionConfigType, Object> getConfig() {
        return config;
    }

    @Override
    public String toString() {
        return String.format("Function[name=%s]", name);
    }

}
