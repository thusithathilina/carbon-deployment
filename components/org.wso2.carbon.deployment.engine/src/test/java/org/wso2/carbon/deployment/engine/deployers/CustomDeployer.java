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
package org.wso2.carbon.deployment.engine.deployers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.deployment.engine.Artifact;
import org.wso2.carbon.deployment.engine.ArtifactType;
import org.wso2.carbon.deployment.engine.Deployer;
import org.wso2.carbon.deployment.engine.exception.CarbonDeploymentException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Custom Deployer class to test deployment.
 *
 * @since 5.0.0
 */
public class CustomDeployer implements Deployer {
    private static final Logger logger = LoggerFactory.getLogger(CustomDeployer.class);
    /**
     * Has init() been called?.
     */
    public static boolean initCalled;
    /**
     * Set to true if "XML1" has been deployed.
     */
    public static boolean sample1Deployed;
    /**
     * Set to true if "XML2" has been deployed.
     */
    public static boolean sample2Deployed;
    /**
     * Set to true if "XML1" has been updated.
     */
    public static boolean sample1Updated;
    /**
     * Set to true if "XML2" has been updated.
     */
    public static boolean sample2Updated;

    private String directory = "text-files";
    private URL directoryLocation;
    private ArtifactType artifactType;
    private String testDir = "src" + File.separator + "test" + File.separator + "resources" +
            File.separator + "carbon-repo" + File.separator + directory;
    private String testDir2 = "src" + File.separator + "test" + File.separator + "resources" +
                             File.separator + "deployment" + File.separator + directory;

    public CustomDeployer() {
        artifactType = new ArtifactType<String>("txt");
        try {
            directoryLocation = new URL("file:text-files");
        } catch (MalformedURLException e) {
            logger.error("Error while initializing directoryLocation", e);
        }
    }

    public void init() {
        logger.info("Initializing Deployer");
        initCalled = true;
    }

    public String deploy(Artifact artifact) throws CarbonDeploymentException {
        logger.info("Deploying : " + artifact.getName());
        String key = null;
        try (FileInputStream fis = new FileInputStream(artifact.getFile())) {
            int x = fis.available();
            byte b[] = new byte[x];
            fis.read(b);
            String content = new String(b);
            if (content.contains("sample1")) {
                sample1Deployed = true;
                key = artifact.getName();
            } else if (content.contains("sample2")) {
                sample2Deployed = true;
                key = artifact.getName();
            }
        } catch (IOException e) {
            throw new CarbonDeploymentException("Error while deploying : " + artifact.getName(), e);
        }
        return key;
    }

    public void undeploy(Object key) throws CarbonDeploymentException {
        if (!(key instanceof String)) {
            throw new CarbonDeploymentException("Error while Un Deploying : " + key +
                    "is not a String value");
        }
        logger.info("Undeploying : " + key);
        File fileToUndeploy;
        if ("sample1.txt".equals(key)) {
            fileToUndeploy = new File(testDir + File.separator + key);
        } else if ("sample2.txt".equals(key)) {
            fileToUndeploy = new File(testDir2 + File.separator + key);
        } else {
            throw new CarbonDeploymentException("Error while Un Deploying : " + key);
        }
        logger.info("File to undeploy : " + fileToUndeploy.getAbsolutePath());
        try (FileInputStream fis = new FileInputStream(fileToUndeploy)) {
            int x = fis.available();
            byte b[] = new byte[x];
            fis.read(b);
            String content = new String(b);
            if (content.contains("sample1")) {
                sample1Deployed = false;
            } else if (content.contains("sample2")) {
                sample2Deployed = false;
            }
        } catch (IOException e) {
            throw new CarbonDeploymentException("Error while Un Deploying : " + key, e);
        }
    }

    public String update(Artifact artifact) throws CarbonDeploymentException {
        logger.info("Updating : " + artifact.getName());
        try (FileInputStream fis = new FileInputStream(artifact.getFile())) {
            int x = fis.available();
            byte b[] = new byte[x];
            fis.read(b);
            String content = new String(b);
            if (content.contains("sample1")) {
                sample1Updated = true;
            } else if (content.contains("sample2")) {
                sample2Updated = true;
            }
        } catch (IOException e) {
            throw new CarbonDeploymentException("Error while updating : " + artifact.getName(), e);
        }
        return artifact.getName();
    }


    public URL getLocation() {
        return directoryLocation;
    }

    public void setLocation(URL directoryLocation) {
        this.directoryLocation = directoryLocation;
    }

    public ArtifactType getArtifactType() {
        return artifactType;
    }

    public void setArtifactType(ArtifactType artifactType) {
        this.artifactType = artifactType;
    }
}
