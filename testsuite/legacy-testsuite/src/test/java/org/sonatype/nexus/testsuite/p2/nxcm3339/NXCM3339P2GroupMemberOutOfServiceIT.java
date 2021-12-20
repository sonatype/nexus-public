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
package org.sonatype.nexus.testsuite.p2.nxcm3339;

import java.io.File;

import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.testsuite.p2.AbstractNexusProxyP2IT;

import org.junit.Test;
import org.restlet.data.MediaType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.sisu.goodies.testsupport.hamcrest.FileMatchers.exists;
import static org.sonatype.sisu.goodies.testsupport.hamcrest.FileMatchers.isDirectory;

public class NXCM3339P2GroupMemberOutOfServiceIT
    extends AbstractNexusProxyP2IT
{

  private final RepositoryMessageUtil repositoryMessageUtil;

  public NXCM3339P2GroupMemberOutOfServiceIT() {
    this("nxcm3339");
  }

  NXCM3339P2GroupMemberOutOfServiceIT(final String testRepositoryId) {
    super(testRepositoryId);
    repositoryMessageUtil = new RepositoryMessageUtil(this, getJsonXStream(), MediaType.APPLICATION_JSON);
  }

  /**
   * When one of member repositories is out of service the group repository should not fail and just use the valid
   * repositories.
   *
   * @throws Exception not expected
   */
  @Test
  public void outOfService()
      throws Exception
  {
    repositoryMessageUtil.setOutOfServiceProxy("nxcm3339-2", true);

    final File installDir = new File("target/eclipse/" + getTestRepositoryId());

    installUsingP2(
        getGroupUrl(getTestRepositoryId()),
        "com.sonatype.nexus.p2.its.feature.feature.group",
        installDir.getCanonicalPath()
    );

    final File feature = new File(installDir, "features/com.sonatype.nexus.p2.its.feature_1.0.0");
    assertThat(feature, exists());
    assertThat(feature, isDirectory());
  }

}
