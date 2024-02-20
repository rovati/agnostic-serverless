package ch.elca.rovl.dsl.api.function;

import ch.elca.rovl.dsl.api.function.models.FunctionRuntime;

public class LocatedFunctionStep {
    FluentFunctionBuilder ffb;

    protected LocatedFunctionStep(FluentFunctionBuilder ffb) {
        this.ffb = ffb;
    }

    public ConfigurableFunctionStep runtime(FunctionRuntime runtime) {
        ffb.withFunctionRuntime(runtime);
        return new ConfigurableFunctionStep(ffb);
    }
}
