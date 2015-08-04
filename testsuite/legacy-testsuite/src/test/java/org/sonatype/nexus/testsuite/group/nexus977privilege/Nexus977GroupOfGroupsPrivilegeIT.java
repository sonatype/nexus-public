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
package org.sonatype.nexus.testsuite.group.nexus977privilege;

import java.io.File;
import java.io.FileNotFoundException;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeDescriptor;
import org.sonatype.nexus.test.utils.FileTestingUtils;
import org.sonatype.nexus.test.utils.GavUtil;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;

import org.apache.maven.index.artifact.Gav;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class Nexus977GroupOfGroupsPrivilegeIT
    extends AbstractPrivilegeTest
{

  @Override
  protected void runOnce()
      throws Exception
  {
    super.runOnce();

    RepositoryMessageUtil.updateIndexes("g4");
  }

  @Test
  public void testReadAll()
      throws Exception
  {
    TestContainer.getInstance().getTestContext().useAdminForRequests();
    giveUserRole(TEST_USER_NAME, "repo-all-read");

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    Gav gav = GavUtil.newGav(this.getTestId(), "project", "1.0.1");

    File artifact = downloadArtifactFromGroup("g4", gav, "./target/downloaded-jars");

    assertTrue(artifact.exists());

    File originalFile = this.getTestResourceAsFile("projects/p1/project.jar");

    Assert.assertTrue(FileTestingUtils.compareFileSHA1s(originalFile, artifact));
  }

  @Test
  public void testReadG4()
      throws Exception
  {
    TestContainer.getInstance().getTestContext().useAdminForRequests();
    addPriv(TEST_USER_NAME, "g4" + "-read-priv", TargetPrivilegeDescriptor.TYPE, "1", null, "g4", "read");

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    Gav gav = GavUtil.newGav(this.getTestId(), "project", "0.8");

    File artifact = downloadArtifactFromGroup("g4", gav, "./target/downloaded-jars");

    assertTrue(artifact.exists());

    File originalFile = this.getTestResourceAsFile("projects/p5/project.jar");

    Assert.assertTrue(FileTestingUtils.compareFileSHA1s(originalFile, artifact));
  }

  @Test
  public void testNoAccess()
      throws Exception
  {
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    Gav gav = GavUtil.newGav(this.getTestId(), "project", "2.1");

    try {
      downloadArtifactFromGroup("g4", gav, "./target/downloaded-jars");
      Assert.fail();
    }
    catch (FileNotFoundException e) {
      Assert.assertTrue(e.getMessage().contains("403"));
    }

  }
}
