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
    Map<FunctionConfigType, Object> config;

    /**
     * Object constructor. The parameters are mandatory configuration required to deploy the
     * database on any provider.
     * 
     * @param name unique name of the function
     * @param pathToProject path to the (parent of the) directory containing the project with the
     *        code for this function
     * @param runtime runtime and version for this function
     */
    public DslFunction(String name, String pathToProject, FunctionRuntime runtime) {
        super(name);

        // check validty of the name
        if (!name.matches("[a-zA-Z0-9_]+"))
            throw new IllegalArgumentException(
                    "Name of the resource has to match regex [a-zA-Z0-9_]+");

        this.pathToProject = pathToProject;
        this.runtime = runtime;
        this.config = new HashMap<>();
    }

    public static DslFunction fromApiFunction(Function fn) {
        DslFunction dslFn = new DslFunction(fn.getName(), fn.getProjectPath(), fn.getRuntime());
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

    public Map<FunctionConfigType, Object> getConfig() {
        return config;
    }

    @Override
    public String toString() {
        return String.format("Function[name=%s]", name);
    }

}
