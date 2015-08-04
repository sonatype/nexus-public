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
package org.sonatype.nexus.testsuite.p2.nxcm0128;

import java.io.File;

import org.sonatype.nexus.testsuite.p2.AbstractNexusProxyP2IT;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.sisu.litmus.testsupport.hamcrest.FileMatchers.exists;
import static org.sonatype.sisu.litmus.testsupport.hamcrest.FileMatchers.isDirectory;
import static org.sonatype.sisu.litmus.testsupport.hamcrest.FileMatchers.readable;

public class NXCM0128P2GroupIT
    extends AbstractNexusProxyP2IT
{

  public NXCM0128P2GroupIT() {
    this("nxcm0128");
  }

  NXCM0128P2GroupIT(final String testRepositoryId) {
    super(testRepositoryId);
  }

  @Test
  public void test()
      throws Exception
  {
    final File installDir = new File("target/eclipse/" + getTestRepositoryId());

    installUsingP2(
        getGroupUrl(getTestRepositoryId()),
        "com.sonatype.nexus.p2.its.feature2.feature.group",
        installDir.getCanonicalPath()
    );

    final File feature = new File(installDir, "features/com.sonatype.nexus.p2.its.feature_1.0.0");
    assertThat(feature, exists());
    assertThat(feature, isDirectory());

    final File feature2 = new File(installDir, "features/com.sonatype.nexus.p2.its.feature2_1.0.0");
    assertThat(feature2, exists());
    assertThat(feature2, isDirectory());

    final File bundle = new File(installDir, "plugins/com.sonatype.nexus.p2.its.bundle_1.0.0.jar");
    assertThat(bundle, is(readable()));
  }

}
