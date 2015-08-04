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

import org.sonatype.nexus.client.core.subsystem.content.Location;
import org.sonatype.nexus.testsuite.npm.NpmMockRegistryITSupport;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

/**
 * Smoke IT for NPM plugin. This IT just starts up NX with NPM plugin, creates a NPM proxy repo, and downloads
 * one single metadata file from it, and validates that URLs are properly rewritten to point back to NX instance. If
 * any
 * of these fails, this IT will fail too.
 */
public class SmokeNpmIT
    extends NpmMockRegistryITSupport
{
  public SmokeNpmIT(String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Test
  public void smoke() throws Exception {
    // create a NPM Proxy repository that proxies mock NPM registry
    createNpmProxyRepository(testMethodName());

    // download package root of commonjs
    final File localDirectory = util.createTempDir();
    final File commonjsPackageRootFile = new File(localDirectory, "commonjs.json");
    content().download(Location.repositoryLocation(testMethodName(), "commonjs"), commonjsPackageRootFile);
    final String commonjsPackageRoot = Files.toString(commonjsPackageRootFile, Charsets.UTF_8);

    // check are the URLs rewritten and point back to NX
    assertThat(commonjsPackageRoot, containsString(
        nexus().getUrl() + "content/repositories/" + testMethodName() + "/commonjs/-/commonjs-0.0.1.tgz"));

    // check that there are not traces of proxied registry URL
    assertThat(commonjsPackageRoot, not(containsString(mockRegistryServerUrl())));
  }
}