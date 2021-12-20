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

import javax.annotation.Nullable;

import org.sonatype.sisu.goodies.testsupport.TestData;
import org.sonatype.tests.http.server.jetty.behaviour.PathRecorderBehaviour;

import com.bolyuba.nexus.plugin.npm.client.NpmProxyRepository;
import org.junit.After;
import org.junit.Before;

/**
 * Support for NPM integration tests for trivial cases, when one single NPM mock registry should be started. This test
 * should not be used for cases that would involve multiple registries, like testing NPM groups of multiple NPM
 * proxies, etc. This test uses JUnit {@link @Before} and {@link @After} to start and stop the NPM registry and offers
 * one method to query it's URL.
 */
public abstract class NpmMockRegistryITSupport
    extends NpmITSupport
{
  private MockNpmRegistry mockNpmRegistry;

  public NpmMockRegistryITSupport(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  /**
   * Starts mock NPM registry if directory named "registry" is found among test data.
   *
   * @see TestData
   */
  @Before
  public void startMockNpmRegistryServer()
      throws Exception
  {
    final File registryRoot = testData().resolveFile("registry");
    if (registryRoot.isDirectory()) {
      mockNpmRegistry = new MockNpmRegistry(registryRoot, null).start();
    }
  }

  /**
   * Stops mock NPM registry if it was started.
   *
   * @see #startMockNpmRegistryServer()
   */
  @After
  public void stopMockNpmRegistryServer()
      throws Exception
  {
    if (mockNpmRegistry != null) {
      mockNpmRegistry.stop();
    }
  }

  /**
   * If mock NPM registry is started, returns it's root URL as String, {@code null} otherwise.
   *
   * @see #startMockNpmRegistryServer()
   */
  @Nullable
  public String mockRegistryServerUrl() {
    if (mockNpmRegistry == null) {
      return null;
    }
    return mockNpmRegistry.getUrl();
  }

  /**
   * Exposes path recorder to perform assertions. See {@link MockNpmRegistry#getPathRecorder()} for details.
   */
  protected PathRecorderBehaviour getPathRecorder() {
    return mockNpmRegistry.getPathRecorder();
  }

  /**
   * When set to true requests are served as usual, when set to false 404 is always returned.
   */
  public void mockRegistryServeRequests(boolean enabled) {
    mockNpmRegistry.serveRequests(enabled);
  }

  /**
   * Creates a NPM Proxy repository in NX instance for this ITs mock NPM registry..
   */
  public NpmProxyRepository createNpmProxyRepository(final String id) {
    return createNpmProxyRepository(id, mockRegistryServerUrl());
  }
}
