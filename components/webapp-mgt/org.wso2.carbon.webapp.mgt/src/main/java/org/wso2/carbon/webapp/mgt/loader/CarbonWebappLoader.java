/*
 * Copyright (c) 2005-2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.webapp.mgt.loader;

import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Customized WebappLoader for Carbon.
 */
public class CarbonWebappLoader extends WebappLoader {
	private static final Log log = LogFactory.getLog(CarbonWebappLoader.class);

	public CarbonWebappLoader() {
		super();
	}

	public CarbonWebappLoader(ClassLoader parent) {
		super(parent);
	}

	@Override
	protected void startInternal() throws LifecycleException {
		WebappClassloadingContext webappClassloadingContext;
		try {
			webappClassloadingContext = ClassloadingContextBuilder.buildClassloadingContext(getWebappFilePath());
		} catch (Exception e) {
			throw new LifecycleException(e.getMessage(), e);
		}

		/*File classloadingXml;
		try {
			classloadingXml = new File(getWebappFilePath() + "/META-INF/webapp-classloading.xml");
			if (classloadingXml.exists()) {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = null;
				builder = factory.newDocumentBuilder();
				Document doc = builder.parse(classloadingXml);
				XPathFactory xPathfactory = XPathFactory.newInstance();
				XPath xpath = xPathfactory.newXPath();
				XPathExpression expr = xpath.compile("Classloading/Environments");
				String environments = expr.evaluate(doc, XPathConstants.STRING).toString();

				String lib = System.getProperty("carbon.home");
				WebResourceRoot webResourceRoot = new StandardRoot(getContext());
				DirResourceSet dirResourceSet = new DirResourceSet(webResourceRoot, "/WEB-INF/lib", "/home/thusitha/Training/Training_Project/wso2as-6.0.0-SNAPSHOT/lib/runtimes/cxf", getContext().getPath());
				webResourceRoot.addPreResources(dirResourceSet);

			}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}*/

		super.startInternal();

		//Adding provided classpath entries, if any
		for (String repository : webappClassloadingContext.getProvidedRepositories()) {
			try {
				((CarbonWebappClassLoader) getClassLoader()).addURL(new URL(repository));
			} catch (MalformedURLException e) {
				if (log.isDebugEnabled()) {
					String msg = "Malformed URL : " + repository;
					log.debug(msg, e);
				}
			}
		}

		//Adding the WebappClassloadingContext to the WebappClassloader
		((CarbonWebappClassLoader) getClassLoader()).setWebappCC(webappClassloadingContext);
	}

	@Override
	protected void stopInternal() throws LifecycleException {

		super.stopInternal();
	}

	//TODO Refactor
	private String getWebappFilePath() throws IOException {
		String webappFilePath = null;
		//Value of the following variable depends on various conditions. Sometimes you get just the webapp directory
		//name. Sometime you get absolute path the webapp directory or war file.
		Context ctx = getContext();
		String docBase = ctx.getDocBase();

		Host host = (Host) ctx.getParent();
		String appBase = host.getAppBase();
		File canonicalAppBase = new File(appBase);
		if (canonicalAppBase.isAbsolute()) {
			canonicalAppBase = canonicalAppBase.getCanonicalFile();
		} else {
			canonicalAppBase = new File(System.getProperty("carbon.home"), appBase).getCanonicalFile();
		}

		File webappFile = new File(docBase);
		if (webappFile.isAbsolute()) {
			webappFilePath = webappFile.getCanonicalPath();
		} else {
			webappFilePath = (new File(canonicalAppBase, docBase)).getPath();
		}
		return webappFilePath;
	}

}
