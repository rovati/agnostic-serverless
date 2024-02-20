package ch.elca.rovl.dsl.pipeline.deployment.accessinfo.restapi;

import ch.elca.rovl.dsl.pipeline.deployment.accessinfo.AccessInfo;

public abstract class RestApiInfo extends AccessInfo {
    final String url;

    protected RestApiInfo(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
