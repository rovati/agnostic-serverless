package ch.elca.rovl.dsl.pipeline.deployment.helper.aws;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elca.rovl.dsl.pipeline.deployment.DeploymentConstants;
import ch.elca.rovl.dsl.pipeline.deployment.accessinfo.database.AwsDatabaseAccess;
import ch.elca.rovl.dsl.pipeline.deployment.accessinfo.database.DatabaseRole;
import ch.elca.rovl.dsl.pipeline.templating.resource.DeployableDatabase;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateSubnetRequest;
import software.amazon.awssdk.services.ec2.model.CreateSubnetResponse;
import software.amazon.awssdk.services.ec2.model.CreateVpcRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcResponse;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.CreateDbInstanceRequest;
import software.amazon.awssdk.services.rds.model.CreateDbInstanceResponse;
import software.amazon.awssdk.services.rds.model.CreateDbSubnetGroupRequest;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DbInstanceNotFoundException;
import software.amazon.awssdk.services.rds.model.DbSubnetGroupNotFoundException;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.rds.model.DescribeDbSubnetGroupsRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbSubnetGroupsResponse;
import software.amazon.awssdk.services.rds.model.VpcSecurityGroupMembership;

public class RdsHelper {
    static final Logger LOG = LoggerFactory.getLogger("Deployment (AWS)");

    final RdsClient rdsClient;
    final Ec2Client vpcClient;

    public RdsHelper() {
        rdsClient = RdsClient.builder().httpClientBuilder(ApacheHttpClient.builder()).build();
        vpcClient = Ec2Client.builder().httpClientBuilder(ApacheHttpClient.builder()).build();
    }

    /**
     * Checks whether the default subnet group exists, and creates it otherwise. In that case, it
     * also creates a private vpc and two subnets.
     * 
     * @return ids of the subnets
     */
    public List<String> createSubnetGroupIfNotExisting() {
        // check if subnet group exists
        try {
            DescribeDbSubnetGroupsResponse resp =
                    rdsClient.describeDBSubnetGroups(DescribeDbSubnetGroupsRequest.builder()
                            .dbSubnetGroupName(DeploymentConstants.AWS_DB_SUBNET_GROUP).build());

            return resp.dbSubnetGroups().get(0).subnets().stream()
                    .map(subnet -> subnet.subnetIdentifier()).collect(Collectors.toList());
        } catch (DbSubnetGroupNotFoundException e) {
            // create vpc and subnets
            CreateVpcResponse vpcResp = vpcClient.createVpc(CreateVpcRequest.builder()
                    .cidrBlock(DeploymentConstants.AWS_DB_VPC_CIDR).build());
            CreateSubnetResponse subnetResp1 = vpcClient.createSubnet(CreateSubnetRequest.builder()
                    .availabilityZone(DeploymentConstants.AWS_DB_SUBNET_REGION_ONE)
                    .cidrBlock(DeploymentConstants.AWS_DB_SUBNET_ONE_CIDR)
                    .vpcId(vpcResp.vpc().vpcId()).build());
            CreateSubnetResponse subnetResp2 = vpcClient.createSubnet(CreateSubnetRequest.builder()
                    .availabilityZone(DeploymentConstants.AWS_DB_SUBNET_REGION_TWO)
                    .cidrBlock(DeploymentConstants.AWS_DB_SUBNET_TWO_CIDR)
                    .vpcId(vpcResp.vpc().vpcId()).build());

            List<String> subnetIds =
                    List.of(subnetResp1.subnet().subnetId(), subnetResp2.subnet().subnetId());

            // create subnet group
            rdsClient.createDBSubnetGroup(CreateDbSubnetGroupRequest.builder()
                    .dbSubnetGroupName(DeploymentConstants.AWS_DB_SUBNET_GROUP)
                    .dbSubnetGroupDescription(DeploymentConstants.AWS_DB_SUBNET_GROUP_DESC)
                    .subnetIds(subnetIds).build());

            return subnetIds;
        }
    }


    /**
     * Checks whether the given database already exists. If it is not the case, it provisions a new
     * server for the database. Returns the endpoint address of the database.
     * 
     * @param db
     * @return the endpoint address of the database
     * @throws InterruptedException
     */
    public List<String> createDatabaseIfNotExisting(DeployableDatabase db) throws InterruptedException {
        List<String> result = new ArrayList<>();

        // check if server exists
        try {
            DescribeDbInstancesResponse response =
                    rdsClient.describeDBInstances(DescribeDbInstancesRequest.builder()
                            .dbInstanceIdentifier(db.getCloudName()).build());
            result.add(response.dbInstances().get(0).endpoint().address());
            for (VpcSecurityGroupMembership vpcGroup : response.dbInstances().get(0).vpcSecurityGroups()) {
                result.add(vpcGroup.vpcSecurityGroupId());
            }

            LOG.info(String.format("Database '%s' already exists, skipping provisioning.", db.getName()));
        } catch (DbInstanceNotFoundException e) {
            // TODO change settings based on db config
            // create db
            CreateDbInstanceResponse response = rdsClient.createDBInstance(
                    // use burstable instance -> free tier
                    CreateDbInstanceRequest.builder().dbInstanceIdentifier(db.getCloudName())
                            .allocatedStorage(100).dbName(db.getName()).engine("postgres")
                            .engineVersion("13.12").dbInstanceClass("db.t3.micro")
                            .storageType("gp2").masterUsername(db.getDatabase().getUsername())
                            .masterUserPassword(db.getDatabase().getPassword())
                            .dbSubnetGroupName(DeploymentConstants.AWS_DB_SUBNET_GROUP)
                            .networkType("IPV4").allocatedStorage(20) // min
                            .build());

            result.add(waitForDatabaseAvailability(db.getCloudName()));
            for (VpcSecurityGroupMembership vpcGroup : response.dbInstance().vpcSecurityGroups()) {
                result.add(vpcGroup.vpcSecurityGroupId());
            }
        }

        return result;
    }

    // TODO change signature an create reader + writer roles
    /**
     * Returns access roles to the given database
     * 
     * @param database
     * @return access roles
     */
    public AwsDatabaseAccess getAccessRoles(DeployableDatabase db) {
        // NOTE for the time being return admin perms
        DatabaseRole reader =
                new DatabaseRole(db.getDatabase().getUsername(), db.getDatabase().getPassword());

        AwsDatabaseAccess access = new AwsDatabaseAccess();
        access.setSuperUser(reader);

        return access;
    }

    /**
     * Regularly polls the server until its status is available, then returns its endpoint.
     * 
     * @param serverName
     * @return the database endpoint
     * @throws InterruptedException
     */
    private String waitForDatabaseAvailability(String serverName) throws InterruptedException {
        // NOTE copied from sdk example
        boolean instanceReady = false;
        String instanceReadyStr;

        DescribeDbInstancesRequest instanceRequest =
                DescribeDbInstancesRequest.builder().dbInstanceIdentifier(serverName).build();

        String dbEndpoint = "";

        // Loop until the cluster is ready.
        while (!instanceReady) {
            DescribeDbInstancesResponse response = rdsClient.describeDBInstances(instanceRequest);
            List<DBInstance> instanceList = response.dbInstances();
            for (DBInstance instance : instanceList) {
                instanceReadyStr = instance.dbInstanceStatus();
                if (instanceReadyStr.contains("available")) {
                    instanceReady = true;
                    dbEndpoint = instance.endpoint().address();
                } else {
                    // TODO review
                    Thread.sleep(20000);
                }
            }
        }

        return dbEndpoint;
    }

    public void close() {
        rdsClient.close();
        vpcClient.close();
    }
}
