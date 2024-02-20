package ch.elca.rovl.dsl.api.function.models.functiontrigger;

import ch.elca.rovl.dsl.resource.function.TriggerType;

public abstract class FunctionTrigger {
    TriggerType type;

    public FunctionTrigger(TriggerType type) {
        this.type = type;
    }

    public TriggerType type() { return type; }
}
