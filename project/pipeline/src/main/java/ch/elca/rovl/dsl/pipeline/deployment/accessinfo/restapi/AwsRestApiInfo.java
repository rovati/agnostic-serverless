package ch.elca.rovl.dsl.pipeline.deployment.accessinfo.restapi;

import java.util.List;

public class AwsRestApiInfo extends RestApiInfo {

    private final String restId;
    private final String rootResourceId;
    private final List<String> resourceMethods;
    private final String stageName;

    public AwsRestApiInfo(String url, String restId, String rootResourceId,
            List<String> resourceMethods, String stageName) {
        super(url);
        this.restId = restId;
        this.rootResourceId = rootResourceId;
        this.resourceMethods = resourceMethods;
        this.stageName = stageName;
    }

    public String getId() {
        return restId;
    }

    public String getRootResourceId() {
        return rootResourceId;
    }

    public List<String> getResourceMethods() {
        return resourceMethods;
    }

    public String getStageName() {
        return stageName;
    }

}
