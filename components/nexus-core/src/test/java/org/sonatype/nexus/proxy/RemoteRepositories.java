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

import javax.annotation.Nullable;

import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.tests.http.server.api.Behaviour;
import org.sonatype.tests.http.server.api.ServerProvider;
import org.sonatype.tests.http.server.api.ServerProvider.FileContext;
import org.sonatype.tests.http.server.fluent.Server;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Test helper to build remote repositories and serve them with Jetty.
 *
 * @author cstamas
 * @since 2.13.1
 */
public class RemoteRepositories
    extends ComponentSupport
{
  public static class Builder
  {
    /**
     * path spec -> Behaviour
     */
    private final Map<String, Behaviour[]> behaviourMap = new HashMap<>();

    /**
     * username -> password
     */
    private final Map<String, RemoteRepository> repositoryMap = new HashMap<>();

    public RemoteRepositories build() throws Exception {
      return new RemoteRepositories(behaviourMap, repositoryMap);
    }

    public Builder behave(String pathspec, Behaviour... behaviour) {
      behaviourMap.put(pathspec, behaviour);
      return this;
    }

    public Builder repo(String name) {
      repositoryMap.put(name, RemoteRepository.repo(name).build());
      return this;
    }

    public Builder repo(String name, String resourceBase) {
      repositoryMap.put(name, RemoteRepository.repo(name).resourceBase(resourceBase).build());
      return this;
    }

    public Builder repo(RemoteRepository repo) {
      repositoryMap.put(repo.getName(), repo);
      return this;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private final Map<String, Behaviour[]> behaviourMap;

  private final Map<String, RemoteRepository> repositoryMap;

  private Server server;

  private RemoteRepositories(final Map<String, Behaviour[]> behaviourMap,
                             final Map<String, RemoteRepository> repositoryMap) throws Exception
  {
    this.behaviourMap = Collections.unmodifiableMap(behaviourMap);
    this.repositoryMap = Collections.unmodifiableMap(repositoryMap);
    startServer();
  }

  private void startServer() throws Exception {
    server = Server.server();
    for (Map.Entry<String, Behaviour[]> behaviourEntry : behaviourMap.entrySet()) {
      log.info("Behavior: {} with {}", behaviourEntry.getKey(), behaviourEntry.getValue());
      server.serve(behaviourEntry.getKey()).withBehaviours(behaviourEntry.getValue());
    }
    for (RemoteRepository remoteRepository : repositoryMap.values()) {
      ServerProvider serverProvider = server.getServerProvider();
      File baseDir = new File(remoteRepository.getResourceBase()).getAbsoluteFile();
      String pathSpec = "/" + remoteRepository.getName() + "/*";
      FileContext fileContext = new FileContext(
          baseDir,
          remoteRepository.isIndexEnabled()
      );
      serverProvider.serveFiles(pathSpec, fileContext);
      log.info("Serving file from {} at {}", baseDir, pathSpec);
      if (remoteRepository.getAuthInfo() != null) {
        AuthInfo authInfo = remoteRepository.getAuthInfo();
        serverProvider.addAuthentication("/*", authInfo.getName());
        for (Map.Entry<String, Object> user : authInfo.getUsers().entrySet()) {
          serverProvider.addUser(user.getKey(), user.getValue());
        }
      }
    }

    server.start();
    log.info("Started RemoteRepositories on port {}", server.getPort());
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

    private final AuthInfo authInfo;

    private RemoteRepository(final String name,
                             final String resourceBase,
                             final boolean indexEnabled,
                             @Nullable final AuthInfo authInfo)
    {
      this.name = name;
      this.resourceBase = resourceBase;
      this.indexEnabled = indexEnabled;
      this.authInfo = authInfo;
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

    @Nullable
    public AuthInfo getAuthInfo() {
      return authInfo;
    }

    public static Builder repo(String name) {
      return new Builder(name);
    }

    public static class Builder
    {
      private final String name;

      private String resourceBase;

      private boolean indexEnabled;

      private AuthInfo authInfo;

      private Builder(final String name) {
        this.name = name;
        this.resourceBase = "target/test-classes/" + name;
        this.indexEnabled = true;
        this.authInfo = null;
      }

      public Builder resourceBase(String resourceBase) {
        this.resourceBase = resourceBase;
        return this;
      }

      public Builder indexEnabled(boolean indexEnabled) {
        this.indexEnabled = indexEnabled;
        return this;
      }

      public Builder authInfo(@Nullable AuthInfo authInfo) {
        this.authInfo = authInfo;
        return this;
      }

      public RemoteRepository build() {
        return new RemoteRepository(name, resourceBase, indexEnabled, authInfo);
      }
    }
  }

  public static class AuthInfo
  {
    private final String name;

    private final Map<String, Object> users;

    public AuthInfo(final String name, final Map<String, Object> users) {
      this.name = name;
      this.users = ImmutableMap.copyOf(users);
    }

    public String getName() {
      return name;
    }

    public Map<String, Object> getUsers() {
      return users;
    }
  }
}
