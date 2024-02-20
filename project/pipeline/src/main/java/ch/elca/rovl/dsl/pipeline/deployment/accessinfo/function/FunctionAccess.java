package ch.elca.rovl.dsl.pipeline.deployment.accessinfo.function;

import javax.crypto.SecretKey;
import ch.elca.rovl.dsl.pipeline.deployment.accessinfo.AccessInfo;

/**
 * Object containing access information for a function.
 */
public class FunctionAccess extends AccessInfo {
    String url;
    // shared key used to sign JSON web tokens
    SecretKey sharedAuthKey;

    public FunctionAccess() {}

    public void setUrl(String url) {
        this.url = url;
    }

    public void setAuthKey(SecretKey key) {
        this.sharedAuthKey = key;
    }

    public SecretKey getAuthKey() {
        return sharedAuthKey;
    }

    public String getUrl() {
        return url;
    }
}
