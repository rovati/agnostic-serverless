package ch.elca.rovl.dsl.pipeline.linking.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.elca.rovl.dsl.pipeline.infraparsing.resource.DslResource;

/**
 * Data structure to keep track of input and output resources of the various functions.
 */
public final class FunctionConnections {
    final Map<String, List<DslResource>> inputs;
    final Map<String, List<DslResource>> outputs;

    /**
     * FunctionConnections constructor.
     */
    public FunctionConnections() {
        inputs = new HashMap<>();
        outputs = new HashMap<>();
    }

    /**
     * Registers the given resource among the input resources of the given function.
     * 
     * @param fnName name of the function
     * @param input input resource
     */
    public void registerInput(String fnName, DslResource input) {
        List<DslResource> fnInputs = inputs.get(fnName);
        if (fnInputs == null) {
            List<DslResource> newInputs = new ArrayList<>();
            newInputs.add(input);
            inputs.put(fnName, newInputs);
        } else {
            // TODO check whether resource already in list
            fnInputs.add(input);
            inputs.put(fnName, fnInputs);
        }
    }

    /**
     * Registers the given resource among the output resources of the given function.
     * 
     * @param fnName name of the function
     * @param output output resource
     */
    public void registerOutput(String fnName, DslResource output) {
        List<DslResource> fnOutputs = outputs.get(fnName);
        if (fnOutputs == null) {
            List<DslResource> newOutputs = new ArrayList<>();
            newOutputs.add(output);
            outputs.put(fnName, newOutputs);
        } else {
            // TODO check whether resource already in list
            fnOutputs.add(output);
            outputs.put(fnName, fnOutputs);
        }
    }

    public List<DslResource> getInputs(String fnName) {
        return inputs.get(fnName);
    }

    public List<DslResource> getOutputs(String fnName) {
        return outputs.get(fnName);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Connections:\n");
        sb.append("\t-- inputs --\n");
        for (String input : inputs.keySet()) {
            sb.append("\t\t" + input + "=>" + inputs.get(input).toString() + "\n");
        }
        sb.append("\t-- outptus --\n");
        for (String output : outputs.keySet()) {
            sb.append("\t\t" + output + "=>" + outputs.get(output).toString() + "\n");
        }

        return sb.toString();
    }
}
