package ch.elca.rovl.dsl.api.function;

import ch.elca.rovl.dsl.registry.SystemRegistry;

public class FunctionEntryPoint {

    FluentFunctionBuilder ffb;

    public FunctionEntryPoint(SystemRegistry systemRegistry, String name) {
        this.ffb = new FluentFunctionBuilder(name);
        systemRegistry.registerFunction(name, ffb);
    }

    public LocatedFunctionStep pathToProject(String path) {
        ffb.withPathToProject(path);
        return new LocatedFunctionStep(ffb);
    }

}
