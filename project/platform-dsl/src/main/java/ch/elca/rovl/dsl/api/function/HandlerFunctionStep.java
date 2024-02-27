package ch.elca.rovl.dsl.api.function;

public class HandlerFunctionStep {
    final FluentFunctionBuilder ffb;

    protected HandlerFunctionStep(FluentFunctionBuilder ffb) {
        this.ffb = ffb;
    }

    public ConfigurableFunctionStep handler(String handler) {
        ffb.withHandler(handler);
        return new ConfigurableFunctionStep(ffb);
    }
    
}
