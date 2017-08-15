/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wso2.carbon.deployment.notifier;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQTopic;
import org.easymock.EasyMock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.wso2.carbon.config.ConfigProviderFactory;
import org.wso2.carbon.config.provider.ConfigProvider;
import org.wso2.carbon.deployment.engine.Artifact;
import org.wso2.carbon.deployment.engine.ArtifactType;
import org.wso2.carbon.deployment.engine.LifecycleListener;
import org.wso2.carbon.deployment.engine.exception.DeployerRegistrationException;
import org.wso2.carbon.deployment.engine.exception.DeploymentEngineException;
import org.wso2.carbon.deployment.engine.internal.DeploymentEngine;
import org.wso2.carbon.deployment.engine.internal.RepositoryScanner;
import org.wso2.carbon.deployment.notifier.deployers.CustomDeployer;
import org.wso2.carbon.deployment.notifier.deployers.FaultyDeployer1;
import org.wso2.carbon.deployment.notifier.deployers.FaultyDeployer2;
import org.wso2.carbon.deployment.notifier.internal.DataHolder;
import org.wso2.carbon.deployment.notifier.listeners.CustomLifecycleListener;
import org.wso2.carbon.kernel.CarbonRuntime;
import org.wso2.carbon.kernel.internal.context.CarbonRuntimeFactory;
import org.wso2.carbon.secvault.SecureVault;

import java.io.File;
import java.util.ArrayList;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;

/**
 * Deployment Engine Test class.
 *
 * @since 5.0.0
 */
public class DeploymentEngineTest extends BaseTest {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentEngineTest.class);

    private static final String CARBON_REPO = "carbon-repo";
    private static final String RUNTIME_REPO = "deployment";
    private static final String DEPLOYER_REPO = CARBON_REPO + File.separator + "text-files";
    private static final String RUNTIME_DEPLOYER_REPO = RUNTIME_REPO + File.separator + "text-files";
    private static final String DEPLOYMENT_YAML = "deployment.yaml";
    private DeploymentEngine deploymentEngine;
    private CustomDeployer customDeployer;
    private FaultyDeployer1 faultyDeployer1;
    private FaultyDeployer2 faultyDeployer2;
    private ArrayList<Artifact> artifactsList;
    private RepositoryScanner repositoryScanner;
    private Artifact artifact;
    private Artifact artifact2;

    private BrokerService brokerService;
    private TopicSubscriber topicSubscriber;

    /**
     * @param testName name of the test case
     */
    public DeploymentEngineTest(String testName) {
        super(testName);

    }

    @BeforeTest
    public void setup() throws Exception {
        customDeployer = new CustomDeployer();
        artifactsList = new ArrayList<>();
        artifact = new Artifact(new File(getTestResourceFile(DEPLOYER_REPO).getAbsolutePath()
                + File.separator + "sample1.txt"));
        artifact.setType(new ArtifactType<>("txt"));
        artifactsList.add(artifact);
        artifact2 = new Artifact(new File(getTestResourceFile(RUNTIME_DEPLOYER_REPO).getAbsolutePath()
                                         + File.separator + "sample2.txt"));
        artifact2.setType(new ArtifactType<>("txt"));
        artifactsList.add(artifact2);

        SecureVault secureVault = EasyMock.mock(SecureVault.class);
        System.setProperty(org.wso2.carbon.utils.Constants.CARBON_HOME, getTestResourceFile("yaml")
                .getAbsolutePath());
        System.setProperty(org.wso2.carbon.utils.Constants.RUNTIME_PATH, getTestResourceFile("deployment/conf")
                .getAbsolutePath());
        ConfigProvider configProvider = ConfigProviderFactory.getConfigProvider(getResourcePath
                ("yaml", "conf", DEPLOYMENT_YAML).get(), secureVault);
        CarbonRuntime carbonRuntime = CarbonRuntimeFactory
                .createCarbonRuntime(configProvider);
        DataHolder.getInstance().setCarbonRuntime(carbonRuntime);

        DataHolder.getInstance().setConfigProvider(configProvider);

        //start a test message broker
        brokerService = new BrokerService();
        brokerService.setUseJmx(false);
        brokerService.setBrokerName("carbon");
        brokerService.setDataDirectory("target/activemq-data");
        brokerService.start();

        Topic topic = new ActiveMQTopic("topic0");
//        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://carbon?create=false");
        Connection connection = connectionFactory.createConnection();
        connection.setClientID("carbon");
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        topicSubscriber = session.createDurableSubscriber(topic, "durable");
        connection.start();

    }

    @Test(expectedExceptions = DeploymentEngineException.class,
            expectedExceptionsMessageRegExp = "Cannot find repository : .*")
    public void testUninitializedDeploymentEngine() throws DeploymentEngineException {
        DeploymentEngine engine = new DeploymentEngine();
        engine.start("/fake/path", "/fake/path2");
    }

    @Test
    public void testCarbonDeploymentEngine() throws DeploymentEngineException {
        deploymentEngine = new DeploymentEngine();
        deploymentEngine.start(getTestResourceFile(CARBON_REPO).getAbsolutePath(),
                               getTestResourceFile(RUNTIME_REPO).getAbsolutePath());
        repositoryScanner = new RepositoryScanner(deploymentEngine);
    }

    @Test(expectedExceptions = DeployerRegistrationException.class,
            expectedExceptionsMessageRegExp = "Failed to add Deployer : Deployer Class Name is null")
    public void testDummyDeployer1() throws DeployerRegistrationException {
        deploymentEngine.registerDeployer(null);
    }

    @Test(expectedExceptions = DeployerRegistrationException.class,
            expectedExceptionsMessageRegExp = "Failed to add Deployer .*: missing 'directory' " +
                                              "attribute in deployer instance")
    public void testDummyDeployer2() throws DeployerRegistrationException {
        CustomDeployer dummy = new CustomDeployer();
        dummy.setLocation(null);
        deploymentEngine.registerDeployer(dummy);
    }

    @Test(expectedExceptions = DeployerRegistrationException.class,
            expectedExceptionsMessageRegExp = "Artifact Type for Deployer : .* is null")
    public void testDummyDeployer3() throws DeployerRegistrationException {
        CustomDeployer dummy = new CustomDeployer();
        dummy.setArtifactType(null);
        deploymentEngine.registerDeployer(dummy);
    }

    @Test(dependsOnMethods = {"testCarbonDeploymentEngine"})
    public void testAddDeployer() throws DeployerRegistrationException {
        deploymentEngine.registerDeployer(customDeployer);
        Assert.assertNotNull(deploymentEngine.getDeployer(customDeployer.getArtifactType()));
    }

    @Test(dependsOnMethods = {"testAddDeployer"})
    public void testRepositoryScanner() {
        repositoryScanner.scan();
        Assert.assertTrue(CustomDeployer.sample1Deployed);
        Assert.assertTrue(CustomDeployer.sample2Deployed);
    }

    @Test(dependsOnMethods = {"testRepositoryScanner"})
    public void testAddLifecycleListener() {
        LifecycleListener listener = new CustomLifecycleListener();
        deploymentEngine.registerDeploymentLifecycleListener(listener);

        listener = new DeploymentNotifierLifecycleListener();
        deploymentEngine.registerDeploymentLifecycleListener(listener);
    }

    @Test(dependsOnMethods = {"testAddLifecycleListener"})
    public void testDeployArtifacts() throws JMSException {
        deploymentEngine.deployArtifacts(artifactsList);
        Assert.assertTrue(CustomDeployer.sample1Deployed);
        Assert.assertTrue(CustomDeployer.sample2Deployed);

        //wait 20 seconds max
        TextMessage message = (TextMessage) topicSubscriber.receive(10000);
        Assert.assertNotNull(message, "The deployment status has not been published to the JMS topic.");
        Assert.assertTrue(message.getText().contains("<currentDeploymentResult>SUCCESSFUL</currentDeploymentResult>"));
        Assert.assertTrue(message.getText().contains("<lifecycleState>AFTER_START_EVENT</lifecycleState>"));
        logger.info("Message received - " + message.getText());
    }

    @Test(dependsOnMethods = {"testDeployArtifacts"})
    public void testUpdateArtifacts() {
        deploymentEngine.updateArtifacts(artifactsList);
        Assert.assertTrue(CustomDeployer.sample1Updated);
        Assert.assertTrue(CustomDeployer.sample2Updated);
    }

    @Test(dependsOnMethods = {"testUpdateArtifacts"})
    public void testUndeployArtifacts() {
        deploymentEngine.undeployArtifacts(artifactsList);
        Assert.assertFalse(CustomDeployer.sample1Deployed);
        Assert.assertFalse(CustomDeployer.sample2Deployed);
    }

    @Test
    public void testRemoveDummyDeployer() {
        try {
            CustomDeployer dummy = new CustomDeployer();
            dummy.setArtifactType(null);
            deploymentEngine.unregisterDeployer(dummy);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Artifact Type"));
        }
    }

    @Test(dependsOnMethods = {"testUndeployArtifacts"})
    public void testRemoveDeployer() throws DeploymentEngineException {
        deploymentEngine.unregisterDeployer(customDeployer);
        Assert.assertNull(deploymentEngine.getDeployer(customDeployer.getArtifactType()));
    }

    @Test(dependsOnMethods = {"testRemoveDeployer"})
    public void testDeployWithoutDeployerInstance() throws JMSException {
        deploymentEngine.deployArtifacts(artifactsList);
        deploymentEngine.updateArtifacts(artifactsList);
        deploymentEngine.undeployArtifacts(artifactsList);
        Assert.assertTrue(deploymentEngine.getFaultyArtifacts().containsValue(artifact));

        //wait 20 seconds max
        TextMessage message = (TextMessage) topicSubscriber.receive(20000);
        Assert.assertNotNull(message, "The deployment status has not been published to the JMS topic.");
        Assert.assertTrue(message.getText().contains("sample2.txt"));
        logger.info("Message received - " + message.getText());
    }

    @Test(dependsOnMethods = {"testDeployWithoutDeployerInstance"})
    public void testFaultyDeployer1() throws DeployerRegistrationException {
        faultyDeployer1 = new FaultyDeployer1();
        deploymentEngine.registerDeployer(faultyDeployer1);
        Assert.assertNotNull(deploymentEngine.getDeployer(faultyDeployer1.getArtifactType()));
    }

    @Test(dependsOnMethods = {"testFaultyDeployer1"})
    public void testFaultyDeployerRepositoryScan1() {
        repositoryScanner.scan();
        Assert.assertFalse(FaultyDeployer1.sample1Deployed);
        Assert.assertFalse(FaultyDeployer1.sample2Deployed);
    }

    @Test(dependsOnMethods = {"testFaultyDeployerRepositoryScan1"})
    public void testFaultyDeployArtifacts1() {
        deploymentEngine.deployArtifacts(artifactsList);
        Assert.assertFalse(FaultyDeployer1.sample1Deployed);
        Assert.assertFalse(FaultyDeployer1.sample2Deployed);
    }

    @Test(dependsOnMethods = {"testFaultyDeployArtifacts1"})
    public void testFaultyUpdateArtifacts1() {
        deploymentEngine.updateArtifacts(artifactsList);
        Assert.assertTrue(FaultyDeployer1.sample1Updated);
        Assert.assertTrue(FaultyDeployer1.sample2Updated);
    }

    @Test(dependsOnMethods = {"testFaultyUpdateArtifacts1"})
    public void testFaultyUndeployArtifacts1() {
        deploymentEngine.undeployArtifacts(artifactsList);
        Assert.assertFalse(FaultyDeployer1.sample1Deployed);
        Assert.assertFalse(FaultyDeployer1.sample2Deployed);
    }

    @Test(dependsOnMethods = {"testFaultyUndeployArtifacts1"})
    public void testRemoveFaultyDeployer1() throws DeploymentEngineException {
        deploymentEngine.unregisterDeployer(faultyDeployer1);
        Assert.assertNull(deploymentEngine.getDeployer(faultyDeployer1.getArtifactType()));
    }


    @Test(dependsOnMethods = {"testRemoveFaultyDeployer1"})
    public void testFaultyDeployer2() throws DeployerRegistrationException {
        faultyDeployer2 = new FaultyDeployer2();
        deploymentEngine.registerDeployer(faultyDeployer2);
        Assert.assertNotNull(deploymentEngine.getDeployer(faultyDeployer2.getArtifactType()));
    }

    @Test(dependsOnMethods = {"testFaultyDeployer2"})
    public void testFaultyDeployerRepositoryScan2() {
        repositoryScanner.scan();
        Assert.assertFalse(FaultyDeployer2.sample1Deployed);
    }

    @Test(dependsOnMethods = {"testFaultyDeployerRepositoryScan2"})
    public void testFaultyDeployArtifacts2() {
        deploymentEngine.deployArtifacts(artifactsList);
        Assert.assertFalse(FaultyDeployer2.sample1Deployed);
        Assert.assertFalse(FaultyDeployer2.sample2Deployed);
    }

    @Test(dependsOnMethods = {"testFaultyDeployArtifacts2"})
    public void testFaultyUpdateArtifacts2() {
        deploymentEngine.updateArtifacts(artifactsList);
        Assert.assertFalse(FaultyDeployer2.sample1Updated);
        Assert.assertFalse(FaultyDeployer2.sample2Updated);
    }

    @Test(dependsOnMethods = {"testFaultyUpdateArtifacts2"})
    public void testFaultyUndeployArtifacts2() {
        deploymentEngine.undeployArtifacts(artifactsList);
        Assert.assertFalse(FaultyDeployer2.sample1Deployed);
        Assert.assertFalse(FaultyDeployer2.sample2Deployed);
    }

    @Test(dependsOnMethods = {"testFaultyUndeployArtifacts2"})
    public void testRemoveFaultyDeployer2() throws DeploymentEngineException {
        deploymentEngine.unregisterDeployer(faultyDeployer2);
        Assert.assertNull(deploymentEngine.getDeployer(faultyDeployer2.getArtifactType()));
    }

    @AfterTest
    public void cleanup() throws Exception {
        brokerService.stop();
        System.clearProperty(org.wso2.carbon.utils.Constants.CARBON_HOME);
    }
}
