package ch.elca.rovl.dsl.pipeline.util;

/**
 * Enum of possible target platform providers.
 */
public enum Provider {
    AZURE("Azure"),
    AWS("AWS"),
    ALIBABA("Alibaba"),
    ORACLE("Oracle"),
    K8S("Kubernetes"),
    GOOGLE("Google"),
    IBM("IBM"),
    DEBUG("Debug");

    public final String name;

    private Provider(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
