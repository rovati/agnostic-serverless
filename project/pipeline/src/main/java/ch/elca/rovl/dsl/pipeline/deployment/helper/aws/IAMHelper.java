package ch.elca.rovl.dsl.pipeline.deployment.helper.aws;

import java.util.List;
import java.util.stream.Collectors;

import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.AttachRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.CreatePolicyRequest;
import software.amazon.awssdk.services.iam.model.CreateRoleRequest;
import software.amazon.awssdk.services.iam.model.GetRoleRequest;
import software.amazon.awssdk.services.iam.model.ListAttachedRolePoliciesRequest;
import software.amazon.awssdk.services.iam.model.NoSuchEntityException;
import software.amazon.awssdk.services.iam.model.Policy;

public class IAMHelper {
    
    IamClient client;
    List<Policy> policies;

    public IAMHelper() {
        this.client =  IamClient.builder()
            .httpClientBuilder(ApacheHttpClient.builder())
            .build();
    }

    /**
     * Gets the arn of the policy with the given name, return null if the policy does not exist.
     * @param policyName name of the policy
     * @param refresh whether to refresh the cached list of policies or not
     * @return the arn of the policy, or null if the policy does not exist
     */
    public String getPolicyArn(String policyName, boolean refresh) {
        if (policies == null || refresh)
            policies = client.listPolicies().policies();

        for (Policy p : policies) {
            if (policyName.equals(p.policyName()))
            return p.arn();
        }

        return null;
    }

    /**
     * Creates a new policy given a name, a description and the content as a string of the json.
     * @param policyName
     * @param policyDescription
     * @param policyContent
     * @return 
     */
    public String createPolicy(String policyName, String policyDescription, String policyContent) {
        return client.createPolicy(CreatePolicyRequest.builder()
                .policyName(policyName)
                .description(policyDescription)
                .policyDocument(policyContent)
                .build()).policy().arn();
    }

    /**
     * Gets the arn of the policy with the given name, or creates a new one if it does not exist.
     * @param policyName
     * @param policyDescription
     * @param policyContent
     * @return the arn or the given policy, or of the newly created one if it does not exist
     */
    public String getOrCreatePolicyArn(String policyName, String policyDescription, String policyContent) {
        String policyArn = getPolicyArn(policyName, true);
        return policyArn != null ?
            policyArn :
            createPolicy(policyName, policyDescription, policyContent);
    }

    /**
     * Gets the arn of the role with the given name, creates it if it doesn't exists
     * @param roleName
     * @param assumePolicyDocument
     * @return the arn of the role
     */
    public String getOrCreateRole(String roleName, String assumePolicyDocument) {
        try {
            // check if role already exists
            return client.getRole(GetRoleRequest.builder()
                .roleName(roleName)
                .build()).role().arn();
        } catch(NoSuchEntityException e) {
            // create role if not present
            return client.createRole(CreateRoleRequest.builder()
                .roleName(roleName)
                .assumeRolePolicyDocument(assumePolicyDocument)
                .build()).role().arn();
        }
    }

    /**
     * Attaches the given policy to the given role, if it isn't already attached
     * @param roleName
     * @param policyName
     * @param policyArn
     */
    public void attachPolicyToRole(String roleName, String policyName, String policyArn) {
        // check if already has policy attached
        List<String> rolePoliciesNames = client.listAttachedRolePolicies(ListAttachedRolePoliciesRequest.builder()
            .roleName(roleName)
            .build()).attachedPolicies()
                .stream().map(policy -> policy.policyName()).collect(Collectors.toList());

        if (!rolePoliciesNames.contains(policyName)) {
            // add policy to role
            client.attachRolePolicy(AttachRolePolicyRequest.builder()
                .roleName(roleName)
                .policyArn(policyArn)
                .build());
        }
    }

    public void close() {
        client.close();
    }
}
