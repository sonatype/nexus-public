/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.bootstrap.jetty;

import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConfiguration.Customizer;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.ServerConnector;

import static com.google.common.base.Preconditions.checkNotNull;

public class DockerSubdomainRequestCustomizer
    implements Customizer
{
  private final String nexusContextPath;

  private final int jettyPort;

  private final int jettySslPort;

  public DockerSubdomainRequestCustomizer(final String contextPath, final int jettyPort, final int jettySslPort)
  {
    this.nexusContextPath = checkNotNull(contextPath) + (contextPath.endsWith("/") ? "" : "/");
    this.jettyPort = jettyPort;
    this.jettySslPort = jettySslPort;
  }

  @Override
  public void customize(final Connector connector, final HttpConfiguration channelConfig, final Request request) {
    HttpURI uri = request.getHttpURI();
    String version = null;
    if (uri.getPath().startsWith("/v1")) {
      version = "/v1";
    }
    else if (uri.getPath().startsWith("/v2")) {
      version = "/v2";
    }
    if (version != null) {
      if (connector instanceof ServerConnector) {
        int localPort = ((ServerConnector) connector).getLocalPort();
        if (localPort != jettyPort && localPort != jettySslPort) {
          return;
        }
      }
      String repositoryName = DockerSubdomainRepositoryMapping.get(request.getHeader("Host"));
      if (repositoryName != null) {
        String dockerLocation = uri.getScheme() != null ? uri.toString() : request.getScheme() + ":" + uri;
        request.setAttribute("dockerLocation", dockerLocation);
        request.setHttpURI(
            new HttpURI(
                uri.toString().replaceFirst(version, nexusContextPath + "repository/" + repositoryName + version)
            )
        );
        // this will reset request internals (such as pathInfo) to the new url
        request.setMetaData(request.getMetaData());
      }
    }
  }
}
