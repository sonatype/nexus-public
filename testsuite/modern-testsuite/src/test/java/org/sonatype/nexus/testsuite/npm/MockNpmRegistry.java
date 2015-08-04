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
package org.sonatype.nexus.testsuite.npm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.tests.http.server.api.Behaviour;
import org.sonatype.tests.http.server.fluent.Server;
import org.sonatype.tests.http.server.jetty.behaviour.PathRecorderBehaviour;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.common.primitives.Ints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * A mock NPM registry.
 */
public class MockNpmRegistry
{
  public static final String TARBALL_BASE_URL = "npmRegistryUrl";

  private final Logger logger = LoggerFactory.getLogger(MockNpmRegistry.class);

  private final File registryRoot;

  private final Map<String, Callable<String>> properties;

  private Server server;

  private PathRecorderBehaviour pathRecorderBehaviour;

  private boolean serveRequests = true;

  /**
   * Constructor that access an existing directory as input.
   */
  public MockNpmRegistry(final File registryRoot, final Map<String, Callable<String>> properties) {
    this.registryRoot = checkNotNull(registryRoot);
    checkArgument(registryRoot.isDirectory(),
        "The registry root must point to existing directory (does not exists or is not directory)");
    this.properties = Maps.newHashMap();
    if (properties != null) {
      this.properties.putAll(properties);
    }
    if (!this.properties.containsKey(TARBALL_BASE_URL)) {
      // default it to "us"
      this.properties.put(TARBALL_BASE_URL, new Callable<String>()
      {
        @Override
        public String call() throws Exception {
          return getUrl();
        }
      });
    }
  }

  /**
   * Starts up the registry. Might be invoked only when registry was not already started or was stopped.
   */
  public synchronized MockNpmRegistry start() {
    checkState(server == null, "Server already started");
    try {
      pathRecorderBehaviour = new PathRecorderBehaviour();
      server = Server.withPort(0).serve("/*")
          .withBehaviours(pathRecorderBehaviour, new NpmGet(registryRoot, properties)).start();
      logger.info("Starting mock NPM registry with root {} at {}", registryRoot, getUrl());
      return this;
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  /**
   * Stops the registry. Might be invoked only when registry was started.
   */
  public synchronized MockNpmRegistry stop() {
    checkState(server != null, "Server not started");
    try {
      logger.info("Stopping mock NPM registry");
      server.stop();
      server = null;
      return this;
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  /**
   * Returns the URL of the registry. Might be invoked only if registry is running. The URL changes between
   * invocations, so caching return value makes sense only during one single "start" of registry.
   */
  public synchronized String getUrl() {
    checkState(server != null, "Server not started");
    return "http://localhost:" + server.getPort();
  }

  /**
   * Exposes path recorder to perform assertions. Returns {@code null} if mock registry never started. Is
   * re-initialized at each start. Meaning, if you start and then stop registry, you can still inspect recorded
   * requests
   * from last run.
   */
  public PathRecorderBehaviour getPathRecorder() {
    return pathRecorderBehaviour;
  }

  /**
   * When set to true requests are served as usual, when set to false 404 is always returned.
   */
  public void serveRequests(boolean enabled) {
    serveRequests = enabled;
  }

  // ==

  /**
   * A mock NPM GET behaviour (handle HTTP GETs only). It basically serves up files from it's supplied root
   * directory, with one trick: of the incoming request path maps to a directory, it will look for a file
   * called {@code data.json} in that directory and serve that instead. This allows one to lay down a directory
   * that resembles NPM registry.
   */
  private class NpmGet
      implements Behaviour
  {
    private final Logger log = LoggerFactory.getLogger(NpmGet.class);

    private final File root;

    private final Map<String, Callable<String>> properties;

    public NpmGet(final File root, final Map<String, Callable<String>> properties) {
      this.root = root;
      this.properties = properties;
    }

    @Override
    public boolean execute(final HttpServletRequest request, final HttpServletResponse response,
                           final Map<Object, Object> ctx)
        throws Exception
    {
      if ("GET".equals(request.getMethod())) {
        log.info("Requested {} {}", request.getMethod(), request.getPathInfo());
        if (MockNpmRegistry.this.serveRequests) {
          final File file = new File(root, request.getPathInfo());
          if (file.isFile()) {
            log.info("Serving file {}", file.getAbsolutePath());
            sendFile(request, response, file);
            return false;
          }
          else if (file.isDirectory()) {
            final File metadata = new File(file, "data.json");
            if (metadata.isFile()) {
              log.info("Serving metadata {}", metadata.getAbsolutePath());
              sendMetadataFile(request, response, metadata);
              return false;
            }
          }
        }
        response.sendError(404);
        return false;
      }

      return true;
    }

    private void sendFile(final HttpServletRequest request, final HttpServletResponse response, final File file)
        throws IOException
    {
      response.setContentType("application/octet-stream");
      response.setContentLength(Ints.checkedCast(file.length()));
      try (InputStream in = new FileInputStream(file); OutputStream out = response.getOutputStream()) {
        ByteStreams.copy(in, out);
      }
    }

    private void sendMetadataFile(final HttpServletRequest request, final HttpServletResponse response, final File file)
        throws Exception
    {
      response.setContentType("application/json");
      // very primitive "interpolation" for now, if we need more, we can add it later
      String jsonMetadata = Files.toString(file, Charsets.UTF_8);
      for (Entry<String, Callable<String>> entry : properties.entrySet()) {
        jsonMetadata = jsonMetadata.replace("${" + entry.getKey() + "}", entry.getValue().call());
      }
      response.setContentLength(jsonMetadata.length()); // file length might change due to replacements above
      try (OutputStream out = response.getOutputStream()) {
        out.write(jsonMetadata.getBytes(Charsets.UTF_8));
        out.flush();
      }
    }
  }
}
