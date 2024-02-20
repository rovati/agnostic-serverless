package ch.elca.rovl.dsl.pipeline.deployment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ch.elca.rovl.dsl.pipeline.deployment.resource.DeployedResource;
import ch.elca.rovl.dsl.pipeline.util.RequiredData;

/**
 * Data strcture for keeping track of all additional configuration needed by a resource.
 */
public final class UpdateInfo {
    final Map<RequiredData.Type, List<DeployedResource>> info;

    public UpdateInfo() {
        info = new HashMap<>();
    }

    /**
     * Registers a resource to be used to retrieve additional configuration info required.
     * 
     * @param type type of information
     * @param resource resource holding information
     */
    public void addInfo(RequiredData.Type type, DeployedResource resource) {
        List<DeployedResource> typeInfo = this.info.get(type);
        if (typeInfo == null) {
            typeInfo = new ArrayList<>();
            typeInfo.add(resource);
            this.info.put(type, typeInfo);
        } else {
            typeInfo.add(resource);
        }
    }

    public List<DeployedResource> getResourcesForType(RequiredData.Type type) {
        return info.get(type);
    }

    public boolean hasEntries() {
        return !info.keySet().isEmpty();
    }

    public Set<RequiredData.Type> getTypes() {
        return info.keySet();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Update Info:\n");
        for (RequiredData.Type type : info.keySet()) {
            sb.append("\t" + type + "\n");
            sb.append(info.get(type).stream().map(dr -> dr.toString() + " - ")
                    .collect(Collectors.toList()));
            sb.append("\n");
        }

        return sb.toString();
    }
}
