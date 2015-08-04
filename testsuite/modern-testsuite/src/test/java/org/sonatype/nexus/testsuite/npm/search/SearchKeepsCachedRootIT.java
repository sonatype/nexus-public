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
package org.sonatype.nexus.testsuite.npm.search;

import java.io.File;
import java.util.List;

import org.sonatype.nexus.client.core.subsystem.content.Location;
import org.sonatype.nexus.testsuite.npm.NpmMockRegistryITSupport;

import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.sonatype.sisu.litmus.testsupport.hamcrest.FileMatchers.isFile;

/**
 * IT for NPM search, ensuring that first pull of registry root populates the cache and second search request will
 * NOT refetch complete remote registry root.
 */
public class SearchKeepsCachedRootIT
    extends NpmMockRegistryITSupport
{
  public SearchKeepsCachedRootIT(String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  private void assertRootRemainsCached(final String rootPath) throws Exception {
    // create a NPM Proxy repository that proxies mock NPM registry
    createNpmProxyRepository(testMethodName());

    // download registry root
    final File localDirectory = util.createTempDir();
    final File registryRootFile = new File(localDirectory, "registryRoot.json");
    content().download(Location.repositoryLocation(testMethodName(), rootPath), registryRootFile);
    assertThat(registryRootFile, isFile());

    // assert remote registry was hit only once with a GET
    {
      final List<String> paths = getPathRecorder().getPathsForVerb("GET");
      assertThat(paths, hasSize(1));
    }

    // rerequest the now cached root
    content().download(Location.repositoryLocation(testMethodName(), rootPath), registryRootFile);
    assertThat(registryRootFile, isFile());

    // assert remote registry was hit still only once with a GET, as this one came from cache
    {
      final List<String> paths = getPathRecorder().getPathsForVerb("GET");
      assertThat(paths, hasSize(1));
    }
  }

  /**
   * Verifies that using path "/" results in registry root being properly cached.
   */
  @Test
  @Ignore("Serving metadata from path '/' is currently disabled")
  public void rootRemainsCached() throws Exception {
    assertRootRemainsCached("/");
  }

  /**
   * Verifies that using path "/-/all" results in registry root being properly cached. Does very same what
   * #rootRemainsCached but uses "registry special" path that would NPM CLI use too.
   */
  @Test
  public void rootRemainsCachedWithSpecial() throws Exception {
    assertRootRemainsCached("/-/all");
  }

}