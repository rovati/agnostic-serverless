package ch.elca.rovl.dsl.resource.function;

import java.util.HashMap;
import java.util.Map;
import ch.elca.rovl.dsl.api.function.models.FunctionRuntime;
import ch.elca.rovl.dsl.resource.Resource;

/**
 * Function resource.
 */
public final class Function extends Resource {

    // relative or absolute path to the parent directory of the project
    private final String projectPath;
    private final FunctionRuntime runtime;
    // class extending Camel RouteBuilder and containing the definition of the route
    // of this function
    private final String handler;
    private final Map<FunctionConfigType, Object> config;

    public Function(String name, String projectPath, FunctionRuntime runtime, String handler) {
        super(name);
        this.projectPath = projectPath;
        this.runtime = runtime;
        this.handler = handler;
        this.config = new HashMap<>();
    }

    public void addConfig(FunctionConfigType type, Object value) {
        config.put(type, value);
    }

    public String getProjectPath() {
        return projectPath;
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

    // NOTE expand to support more configuration
    public enum FunctionConfigType {
        TRIGGER, EXEC_TIMEOUT
    }

}
