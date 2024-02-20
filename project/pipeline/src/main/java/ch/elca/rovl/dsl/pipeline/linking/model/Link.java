package ch.elca.rovl.dsl.pipeline.linking.model;

import ch.elca.rovl.dsl.pipeline.util.ResourceType;

/**
 * Object representing a link between two cloud resources.<p>
 * A link is formed by a "in" resource and an "out" resource.
 */
public final class Link {
    final String inputName;
    final ResourceType inType;
    final String outputName;
    final ResourceType outType;

    /**
     * Link constructor.
     * 
     * @param input in resource
     * @param inType type of the in resource
     * @param output out resource
     * @param outType type of the out resource
     */
    public Link(String input, ResourceType inType, String output, ResourceType outType) {
        this.inputName = input;
        this.inType = inType;
        this.outputName = output;
        this.outType = outType;
    }

    public String getInput() {
        return inputName;
    }

    public ResourceType getInputType() {
        return inType;
    }

    public String getOutput() {
        return outputName;
    }

    public ResourceType getOutputType() {
        return outType;
    }

    @Override
    public String toString() {
        return String.format("Link[in=%s(%s) - out=%s(%s)]", inputName, inType, outputName,
                outType);
    }
}
