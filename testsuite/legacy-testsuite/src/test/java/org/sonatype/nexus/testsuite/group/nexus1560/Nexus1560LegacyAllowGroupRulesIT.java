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
package org.sonatype.nexus.testsuite.group.nexus1560;

import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeDescriptor;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class Nexus1560LegacyAllowGroupRulesIT
    extends AbstractLegacyRulesIT
{

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Before
  public void init()
      throws Exception
  {
    TestContainer.getInstance().getTestContext().useAdminForRequests();
    addPriv(TEST_USER_NAME, NEXUS1560_GROUP + "-priv", TargetPrivilegeDescriptor.TYPE, "1", null, NEXUS1560_GROUP,
        "read");
  }

  @Test
  public void fromGroup()
      throws Exception
  {
    // the user also needs the view priv
    addPrivilege(TEST_USER_NAME, "repository-" + NEXUS1560_GROUP);

    String downloadUrl = GROUP_REPOSITORY_RELATIVE_URL + NEXUS1560_GROUP + "/" + getRelitiveArtifactPath(gavArtifact1);

    assertDownloadSucceeds(downloadUrl);
  }

  @Test
  public void fromRepository()
      throws Exception
  {
    // the user also needs the view priv
    addPrivilege(TEST_USER_NAME, "repository-" + REPO_TEST_HARNESS_REPO);

    String downloadUrl =
        REPOSITORY_RELATIVE_URL + REPO_TEST_HARNESS_REPO + "/" + getRelitiveArtifactPath(gavArtifact1);

    assertDownloadSucceeds(downloadUrl);
  }

}
