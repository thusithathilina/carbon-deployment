/*
* Copyright 2004,2013 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.wso2.carbon.javaee.tomee.scan;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.JarScanType;
import org.apache.tomcat.JarScannerCallback;
import org.apache.tomcat.util.res.StringManager;
import org.apache.tomcat.util.scan.Constants;
import org.apache.tomee.loader.TomEEJarScanner;
import org.eclipse.osgi.framework.adaptor.BundleClassLoader;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

/**
 * TomEE jar scanner with Carbon bits.
 * In addition to what TomEE and Tomcat does, we need to scan the jars
 * under plugins
 */
public class ASTomEEJarScanner extends TomEEJarScanner {

	private static final Log log = LogFactory.getLog(ASTomEEJarScanner.class);
	private static final StringManager sm = StringManager.getManager(Constants.Package);

	private static final String CARBON_PLUGINS_DIR_PATH = System.getProperty("carbon.home") +
	                                                      File.separator + "repository" + File.separator +
	                                                      "components" + File.separator + "plugins";

	@Override
	public void scan(JarScanType scanType, ServletContext context, JarScannerCallback callback) {

		ClassLoader loader = Thread.currentThread().getContextClassLoader();

		while (loader != null) {
			// WSO2 Carbon specific code snippet
			// Setting the plugins directory only if the parent classLoader is a bundleClassLoader.
			if (loader instanceof BundleClassLoader) {
				File pluginsDir = new File(CARBON_PLUGINS_DIR_PATH);
				File[] jarFiles = pluginsDir.listFiles(new FileFilter() {
					public boolean accept(File file) {
						return file.getName().endsWith(Constants.JAR_EXT);
					}
				});
				// processing collected jar files for tldListeners
				for (File jarFile : jarFiles) {
					/*try {
						process(callback, jarFile.toURI().toURL()); //TODO fix this
					} catch (IOException e) {
						log.warn(sm.getString("jarScan.classloaderFail"), e);
					}*/
				}
			}
		}

	}

	/**
	 * Scan a URL for JARs with the optional extensions to look at all files
	 * and all directories.
	 *//*
	private void process(JarScannerCallback callback, URL url, String webappPath, boolean isWebapp) throws IOException {

		if (log.isTraceEnabled()) {
			log.trace(sm.getString("jarScan.jarUrlStart", url));
		}

		URLConnection conn = url.openConnection();
		if (conn instanceof JarURLConnection) {
			callback.scan((JarURLConnection) conn, webappPath, isWebapp);
		} else {
			String urlStr = url.toString();
			if (urlStr.startsWith("file:") || urlStr.startsWith("jndi:")) {
				if (urlStr.endsWith(Constants.JAR_EXT)) {
					URL jarURL = new URL("jar:" + urlStr + "!/");
					callback.scan((JarURLConnection) jarURL.openConnection(), webappPath, isWebapp);
				} else {
					File f;
					try {
						f = new File(url.toURI());
						if (f.isFile() && isScanAllFiles()) {
							// Treat this file as a JAR
							URL jarURL = new URL("jar:" + urlStr + "!/");
							callback.scan((JarURLConnection) jarURL.openConnection(), webappPath, isWebapp);
						} else if (f.isDirectory() && isScanAllDirectories()) {
							File metainf = new File(f.getAbsoluteFile() +
							                        File.separator + "META-INF");
							if (metainf.isDirectory()) {
								callback.scan(f, webappPath, isWebapp);
							}
						}
					} catch (URISyntaxException e) {
						// Wrap the exception and re-throw
						IOException ioe = new IOException();
						ioe.initCause(e);
						throw ioe;
					}
				}
			}
		}

	}*/

}
