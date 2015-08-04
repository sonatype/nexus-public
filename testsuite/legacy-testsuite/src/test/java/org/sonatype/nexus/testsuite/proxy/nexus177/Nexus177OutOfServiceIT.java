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
package org.sonatype.nexus.testsuite.proxy.nexus177;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;

import org.sonatype.nexus.integrationtests.AbstractNexusProxyIntegrationTest;
import org.sonatype.nexus.integrationtests.ITGroups.PROXY;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.test.utils.FileTestingUtils;

import org.apache.maven.index.artifact.Gav;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Create three repositories, deploys a different artifact with the same name in each repo. Add each repo to a group
 * Access each repo and group, take one out of service. Access each repo and the group.
 */
public class Nexus177OutOfServiceIT
    extends AbstractNexusProxyIntegrationTest
{

  public static final String TEST_RELEASE_REPO = "release-proxy-repo-1";

  public Nexus177OutOfServiceIT() {
    super(TEST_RELEASE_REPO);
  }

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  @Category(PROXY.class)
  public void outOfServiceTest()
      throws Exception
  {

    // get an artifact
    Gav gav =
        new Gav(this.getTestId(), "out-of-service", "0.1.8-four-beta18", null, "jar", 0, new Date().getTime(),
            "Simple Test Artifact", false, null, false, null);

    // download an artifact
    File originalFile = this.downloadArtifact(gav, "target/downloads/original");

    // put proxy out of service
    repositoryUtil.setOutOfServiceProxy(TEST_RELEASE_REPO, true);

    // redownload artifact
    try {
      // download it
      downloadArtifact(gav, "./target/downloaded-jars");
      Assert.fail("Out Of Service Command didn't do anything.");
    }
    catch (FileNotFoundException e) {
    }

    // put proxy back in service
    repositoryUtil.setOutOfServiceProxy(TEST_RELEASE_REPO, false);

    // redownload artifact
    File newFile = this.downloadArtifact(gav, "target/downloads/original");

    // compare the files just for kicks
    Assert.assertTrue(FileTestingUtils.compareFileSHA1s(originalFile, newFile));

  }

}
