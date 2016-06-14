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
package org.sonatype.nexus.proxy;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.tests.http.server.api.ServerProvider.FileContext;
import org.sonatype.tests.http.server.fluent.Server;

import com.google.common.base.Throwables;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Test helper to build remote repositories and serve them with Jetty.
 *
 * @author cstamas
 * @since 2.13.1
 */
public class RemoteRepositories
{
  public static Builder builder() {
    return new Builder();
  }

  private final Map<String, RemoteRepository> repositoryMap;

  private Server server;

  private RemoteRepositories(final Map<String, RemoteRepository> repositoryMap) throws Exception {
    this.repositoryMap = Collections.unmodifiableMap(repositoryMap);
    startServer();
  }

  private void startServer() throws Exception {
    server = Server.server();
    for (RemoteRepository remoteRepository : repositoryMap.values()) {
      FileContext fileContext = new FileContext(
          new File(remoteRepository.getResourceBase()).getAbsoluteFile(),
          remoteRepository.isIndexEnabled()
      );
      server.getServerProvider().serveFiles(remoteRepository.getName() + "/**", fileContext);
    }
    server.start();
  }

  private void stopServer() throws Exception {
    if (server != null) {
      server.stop();
      server = null;
    }
  }

  public Map<String, RemoteRepository> getRepositoryMap() {
    return repositoryMap;
  }

  public void start() throws Exception {
  }

  public void stop() throws Exception {
    stopServer();
  }

  public String getUrl(String repoName) {
    checkArgument(repositoryMap.containsKey(repoName), "no repository with name %s", repoName);
    checkArgument(server != null, "Server not started");
    try {
      return server.getUrl().toExternalForm() + "/" + repoName + "/";
    }
    catch (MalformedURLException e) {
      throw Throwables.propagate(e);
    }
  }

  public int getPort() {
    checkArgument(server != null, "Server not started");
    return server.getPort();
  }

  public static class RemoteRepository
  {
    private final String name;

    private final String resourceBase;

    private final boolean indexEnabled;

    public RemoteRepository(final String name, final String resourceBase, final boolean indexEnabled) {
      this.name = name;
      this.resourceBase = resourceBase;
      this.indexEnabled = indexEnabled;
    }

    public String getName() {
      return name;
    }

    public String getResourceBase() {
      return resourceBase;
    }

    public boolean isIndexEnabled() {
      return indexEnabled;
    }
  }

  public static class Builder
  {
    private final Map<String, RemoteRepository> repositoryMap = new HashMap<>();

    public RemoteRepositories build() throws Exception {
      return new RemoteRepositories(repositoryMap);
    }

    public Builder repo(String name, String resourceBase) {
      return repo(name, resourceBase, true);
    }

    public Builder repo(String name, String resourceBase, boolean indexEnabled) {
      repositoryMap.put(name, new RemoteRepository(name, resourceBase, indexEnabled));
      return this;
    }
  }
}
