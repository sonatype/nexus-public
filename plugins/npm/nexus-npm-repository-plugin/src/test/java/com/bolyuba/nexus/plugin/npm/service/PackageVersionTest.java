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
package com.bolyuba.nexus.plugin.npm.service;

import java.util.Map;

import org.sonatype.sisu.goodies.testsupport.TestSupport;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.Mock;

public class PackageVersionTest
    extends TestSupport
{

  @Mock
  PackageRoot packageRoot;

  PackageVersion underTest;

  private void buildPackageVersionForTest(String name) {
    Map<String, Object> raw = ImmutableMap.of("name", (Object) name,
        "version", "1.0.0",
        "dist", ImmutableMap.of("tarball", "generate-on-request"));
    underTest = new PackageVersion(packageRoot, "npm-hosted-1", raw);
  }

  @Test
  public void testGetDistTarballFilenameScoped() {
    buildPackageVersionForTest("@sonatype/test");
    MatcherAssert.assertThat(underTest.getDistTarballFilename(), Matchers.is("test-1.0.0.tgz"));
  }

  @Test
  public void testGetDistTarballFilenameNotScoped() {
    buildPackageVersionForTest("test");
    MatcherAssert.assertThat(underTest.getDistTarballFilename(), Matchers.is("test-1.0.0.tgz"));
  }
}
