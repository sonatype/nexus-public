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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConfiguration.Customizer;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.ServerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.regex.Pattern.compile;

public class NexusRequestCustomizer
    implements Customizer
{
  private static final Logger log = LoggerFactory.getLogger(NexusRequestCustomizer.class);

  private static final String REPOSITORY = "repository";

  private static final String DOCKER_V1_REQUEST_PREFIX = "/v1";

  private static final String DOCKER_V2_REQUEST_PREFIX = "/v2";

  private static final String DOCKER_TOKEN_REQUEST_SUFFIX = "/v2/token";

  private final int jettyPort;

  private final int jettySslPort;

  private final String repositoryRequestPathPrefix;

  private final Pattern dockerBehindReverseProxyTokenRequestPattern;

  public NexusRequestCustomizer(final String contextPath, final int jettyPort, final int jettySslPort) {
    this.jettyPort = jettyPort;
    this.jettySslPort = jettySslPort;
    String nexusContextPath = checkNotNull(contextPath) + (contextPath.endsWith("/") ? "" : "/");
    this.repositoryRequestPathPrefix = nexusContextPath + REPOSITORY;
    this.dockerBehindReverseProxyTokenRequestPattern = initDockerBehindReverseProxyTokenRequestPattern();
  }

  @Override
  public void customize(final Connector connector, final HttpConfiguration channelConfig, final Request request) {
    HttpURI uri = request.getHttpURI();
    String path = uri.getPath();

    if (path == null) {
      log.debug("Invalid URL for request: {}", request);
      return;
    }

    if (path.startsWith(DOCKER_V1_REQUEST_PREFIX) || path.startsWith(DOCKER_V2_REQUEST_PREFIX)) {
      customizeDockerSubdomainRequest(connector, request, path, uri);
    }
    else if (path.endsWith(DOCKER_TOKEN_REQUEST_SUFFIX)) {
      customizeDockerBehindReverseProxyTokenRequest(request, uri, path);
    }
  }

  private void customizeDockerSubdomainRequest(
      final Connector connector,
      final Request request,
      final String path,
      final HttpURI uri)
  {
    String version = path.substring(0, 3);
    log.debug("Found {} for {}", version, uri);

    if (isJettyPort(connector)) {
      String repositoryName = DockerSubdomainRepositoryMapping.get(request.getHeader("Host"));
      if (repositoryName != null) {
        String dockerLocation = uri.getScheme() != null ? uri.toString() : request.getScheme() + ":" + uri;
        log.debug("For {} dockerLocation {}", repositoryName, dockerLocation);

        request.setAttribute("dockerLocation", dockerLocation);
        String newPath = path.replaceFirst(version, repositoryRequestPathPrefix + '/' + repositoryName + version);
        setNewRequestURI(request, uri, path, newPath);
      }
    }
  }

  private boolean isJettyPort(final Connector connector) {
    if (connector instanceof ServerConnector serverConnector) {
      int localPort = serverConnector.getLocalPort();
      return localPort == jettyPort || localPort == jettySslPort;
    }
    return false;
  }

  private Pattern initDockerBehindReverseProxyTokenRequestPattern() {
    // e.g. /nexus-context/repository/docker-repo/nexus-context/repository/docker-repo/v2/token
    return compile(format("^%s/([^/]+)%s/([^/]+)/v2/token$", repositoryRequestPathPrefix, repositoryRequestPathPrefix));
  }

  private void customizeDockerBehindReverseProxyTokenRequest(
      final Request request,
      final HttpURI uri,
      final String path)
  {
    Matcher matcher = dockerBehindReverseProxyTokenRequestPattern.matcher(path);
    if (matcher.matches() && matcher.group(1).equals(matcher.group(2))) {
      // e.g. /nexus-context/repository/docker-repo/nexus-context/repository/docker-repo/v2/token
      // -> /nexus-context/repository/docker-repo/v2/token
      String newPath = path.substring(path.lastIndexOf(repositoryRequestPathPrefix));
      setNewRequestURI(request, uri, path, newPath);
    }
  }

  private void setNewRequestURI(final Request request, final HttpURI uri, final String path, final String newPath) {
    request.setHttpURI(new HttpURI(uri.toString().replace(path, newPath)));
    request.setMetaData(request.getMetaData());
  }
}
