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
package org.sonatype.nexus.testsuite.npm.smoke;

import java.io.File;

import org.sonatype.nexus.bundle.launcher.NexusBundleConfiguration;
import org.sonatype.nexus.client.core.exception.NexusClientNotFoundException;
import org.sonatype.nexus.client.core.subsystem.content.Location;
import org.sonatype.nexus.testsuite.npm.NpmMockRegistryITSupport;
import org.sonatype.sisu.goodies.testsupport.hamcrest.FileMatchers;

import com.bolyuba.nexus.plugin.npm.client.NpmProxyRepository;
import org.junit.Test;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * NPM IT for NPM plugin verifying that on metadata fetch NFC is evicted for given package and children.
 */
public class NpmCacheNfcEvictIT
    extends NpmMockRegistryITSupport
{
  public NpmCacheNfcEvictIT(String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Override
  protected NexusBundleConfiguration configureNexus(final NexusBundleConfiguration configuration) {
    return super.configureNexus(configuration)
        .setLogLevel("remote.storage.outbound", "DEBUG");
  }

  /**
   * We ask for a tarball without asking for package metadata (something npm client would never do), and it will
   * result in tarball getting into NFC. Then we ask for package metadata and then again for tarball, and 2nd call
   * should succeed, as metadata fetch should evict NFC.
   */
  @Test
  public void cacheEvict() throws Exception {
    // create a NPM Proxy repository that proxies mock NPM registry
    NpmProxyRepository proxy = createNpmProxyRepository(testMethodName());

    final File irrelevant = util.createTempFile();
    final Location commonjs = Location.repositoryLocation(proxy.id(), "/commonjs");
    final Location commonjs001Tar = Location.repositoryLocation(proxy.id(), "/commonjs/-/commonjs-0.0.1.tgz");

    mockRegistryServeRequests(false);
    try {
      content().download(commonjs001Tar, irrelevant);
      fail("Nexus did not cache metadata, should not be able to get tarball");
    }
    catch (NexusClientNotFoundException e) {
      // we expect this
    }
    mockRegistryServeRequests(true);

    // Fetch metadata, should evict NFC too
    content().download(commonjs, irrelevant);

    // now try again the tarball, should succeed
    content().download(commonjs001Tar, irrelevant);

    // sanity check: tarball is here, exists, and is not 0 sized
    assertThat(irrelevant, FileMatchers.isFile());
    assertThat(irrelevant, not(FileMatchers.sized(0l)));
  }
}
