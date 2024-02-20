package ch.elca.rovl.dsl.pipeline.deployment.helper.aws.model;

import java.util.List;

public class RestApiInfo {
    String restUrl;
    String restId;
    String rootResourceId;
    List<String> resourceMethods;

    public RestApiInfo(String restUrl, String restId, String rootResourceId, List<String> resourceMethods) {
        this.restUrl = restUrl;
        this.restId = restId;
        this.rootResourceId = rootResourceId;
        this.resourceMethods = resourceMethods;
    }

    public String url() { return restUrl; }
    public String id() { return restId; }
    public String rootResourceId() { return rootResourceId; }
    public List<String> resourceMethods() { return resourceMethods; }
}
