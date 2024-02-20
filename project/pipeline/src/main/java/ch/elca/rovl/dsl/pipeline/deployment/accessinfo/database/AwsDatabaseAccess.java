package ch.elca.rovl.dsl.pipeline.deployment.accessinfo.database;

import java.util.ArrayList;
import java.util.List;

public class AwsDatabaseAccess extends DatabaseAccess {
    List<String> subnetIds;
    List<String> securityGroupIds;

    public AwsDatabaseAccess() {
        super();
        subnetIds = new ArrayList<>();
        securityGroupIds = new ArrayList<>();
    }

    public void addSubnetIds(List<String> ids) {
        subnetIds.addAll(ids);
    }

    public void addSecurityGroupIds(List<String> ids) {
        securityGroupIds.addAll(ids);
    }

    public List<String> getSubnetIds() {
        return subnetIds;
    }

    public List<String> getSecurityGroupIds() {
        return securityGroupIds;
    }

}
