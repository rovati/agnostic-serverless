package ch.elca.rovl.dsl.pipeline.deployment.helper.azure;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.servicebus.fluent.QueuesClient;
import com.azure.resourcemanager.servicebus.fluent.models.SBQueueInner;
import com.azure.resourcemanager.servicebus.models.NamespaceAuthorizationRule;
import com.azure.resourcemanager.servicebus.models.NamespaceSku;
import com.azure.resourcemanager.servicebus.models.ServiceBusNamespace;
import com.azure.resourcemanager.servicebus.models.ServiceBusNamespaces;

import ch.elca.rovl.dsl.pipeline.deployment.DeploymentConstants;
import ch.elca.rovl.dsl.pipeline.deployment.helper.azure.model.ServiceBusQueueKeys;

public class ServiceBusHelper {
    final ServiceBusNamespaces client;

    public ServiceBusHelper(ServiceBusNamespaces namespaces) {
        this.client = namespaces;
    }

    /**
     * Returns whether a ServiceBus namespace with the given name already exists or not in the given
     * resource group.
     * 
     * @param namespaceName
     * @param resourceGroup
     * @return true if a namespace with the given name already exists in the given resource group
     */
    public boolean namespaceExists(String namespaceName, String resourceGroup) {
        // try to get namespace
        try {
            client.manager().namespaces().getByResourceGroup(resourceGroup, namespaceName);
            return true;
        } catch (ManagementException e) {
            return false;
        }
    }

    /**
     * Creates a namespace and a queue with the given names, in the default resource group and
     * region.
     * 
     * @param namespaceName
     * @param queueName
     * @return the namepsace
     */
    public ServiceBusNamespace createNamespaceAndQueue(String namespaceName, String queueName) {
        return client.define(namespaceName).withRegion(DeploymentConstants.AZURE_DEFAULT_REGION)
                .withExistingResourceGroup(DeploymentConstants.AZURE_DEFAULT_RESOURCE_GROUP)
                .withNewQueue(queueName, 1024).withSku(NamespaceSku.BASIC).create();
    }

    /**
     * Creates a queue with the given name in the given namespace if it does not already exist.
     * 
     * @param namespaceName
     * @param queueName
     * @return the namespace
     */
    public ServiceBusNamespace createQueueIfMissing(String namespaceName, String queueName) {
        ServiceBusNamespace ns = client.getByResourceGroup(
                DeploymentConstants.AZURE_DEFAULT_RESOURCE_GROUP, namespaceName);

        try {
            // try to get queue
            ns.manager().serviceClient().getQueues().get(
                    DeploymentConstants.AZURE_DEFAULT_RESOURCE_GROUP, namespaceName, queueName);
        } catch (ManagementException e) {
            if (e.getResponse().getStatusCode() == 404) {
                // if get queue throws and it is resource not found, create queue
                SBQueueInner q = new SBQueueInner().withMaxSizeInMegabytes(1024);
                QueuesClient qClient = ns.manager().serviceClient().getQueues();
                qClient.createOrUpdate(DeploymentConstants.AZURE_DEFAULT_RESOURCE_GROUP,
                        namespaceName, queueName, q);
            } else {
                throw e;
            }
        }

        return ns;
    }

    /**
     * Creates SAS keys for sending and listening to the given namespace.
     * 
     * @param namespace
     * @return primary connection strings of the send and listen SAS keys
     */
    public ServiceBusQueueKeys createAccessKeys(ServiceBusNamespace namespace) {
        NamespaceAuthorizationRule nsarSend = namespace.authorizationRules()
                .define(DeploymentConstants.AZURE_NAMESPACE_SEND_SAS).withSendingEnabled().create();
        NamespaceAuthorizationRule nsarListen =
                namespace.authorizationRules().define(DeploymentConstants.AZURE_NAMESPACE_RECV_SAS)
                        .withListeningEnabled().create();

        return new ServiceBusQueueKeys(nsarSend.getKeys().primaryConnectionString(),
                nsarListen.getKeys().primaryConnectionString());
    }
}
