package ch.elca.rovl.dsl.api.function;

import ch.elca.rovl.dsl.api.function.models.functiontrigger.FunctionTrigger;

public class ConfigurableFunctionStep {

    FluentFunctionBuilder ffb;

    public ConfigurableFunctionStep(FluentFunctionBuilder ffb) {
        this.ffb = ffb;
    }

    public ConfigurableFunctionStep execTimeout(int seconds) {
        ffb.withExecTimeout(seconds);
        return this;
    }

    public ConfigurableFunctionStep rest(FunctionTrigger trigger) {
        ffb.withTrigger(trigger);
        return this;
    }
    
}
