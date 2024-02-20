package ch.elca.rovl.dsl.api.function;

import java.io.File;
import ch.elca.rovl.dsl.api.FluentResourceBuilder;
import ch.elca.rovl.dsl.api.function.models.FunctionRuntime;
import ch.elca.rovl.dsl.api.function.models.functiontrigger.FunctionTrigger;
import ch.elca.rovl.dsl.resource.function.Function;
import ch.elca.rovl.dsl.resource.function.Function.FunctionConfigType;

public class FluentFunctionBuilder implements FluentResourceBuilder {

    // required
    String name;
    String pathToProject;
    FunctionRuntime runtime;
    
    // optional
    FunctionTrigger trigger = null;
    int execTimeoutSeconds = -1;
    

    protected FluentFunctionBuilder(String name) {
        this.name = name;
    }

    protected FluentFunctionBuilder withPathToProject(String path) {
        this.pathToProject = path;
        return this;
    }

    protected FluentFunctionBuilder withFunctionRuntime(FunctionRuntime runtime) {
        this.runtime = runtime;
        return this;
    }

    protected FluentFunctionBuilder withTrigger(FunctionTrigger trigger) {
        if (this.trigger != null)
            throw new IllegalStateException("Cannot define more than one trigger per function!");

        this.trigger = trigger;
        return this;
    }

    protected FluentFunctionBuilder withExecTimeout(int seconds) {
        this.execTimeoutSeconds = seconds;
        return this;
    }

    @Override
    public void validate() {
        if (name == null || name.isEmpty())
            throw new IllegalStateException("Function is missing the mandatory field 'name'");

        if (pathToProject == null || pathToProject.isEmpty())
            throw new IllegalStateException(String.format(
                "Function '%s' is missing the mandatory field 'path to project'", name));

        File proj = new File(pathToProject);
        if (!proj.exists())
            throw new IllegalStateException(String.format(
                "The specified project path for function '%s' does not exist.", name));

        if (runtime == null)
            throw new IllegalStateException(String.format(
                "Function '%s' is missing the mandatory field 'runtime'.", name));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Function build() {
        Function fn = new Function(name, pathToProject, runtime);

        if (execTimeoutSeconds > 0) {
            fn.addConfig(FunctionConfigType.EXEC_TIMEOUT, execTimeoutSeconds);
        }

        if (trigger != null) {
            fn.addConfig(FunctionConfigType.TRIGGER, trigger);
        }

        return fn;
    }
    
}
