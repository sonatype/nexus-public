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
package org.sonatype.nexus.testsuite.security.nexus1071;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.ITGroups.SECURITY;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.test.utils.FileTestingUtils;

import org.apache.maven.index.artifact.Gav;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertTrue;

/**
 * @author Juven Xu
 */
public class Nexus1071AnonAccessIT
    extends AbstractPrivilegeTest
{

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  @Category(SECURITY.class)
  public void downloadArtifactFromPublicGroup()
      throws Exception
  {
    Gav gav =
        new Gav(this.getTestId(), "release-jar", "1", null, "jar", 0, new Date().getTime(), "Release Jar",
            false, null, false, null);

    File artifact = this.downloadArtifactFromGroup("public", gav, "./target/downloaded-jars");

    assertTrue(artifact.exists());

    File originalFile =
        this.getTestResourceAsFile("projects/" + gav.getArtifactId() + "/" + gav.getArtifactId() + "."
            + gav.getExtension());

    Assert.assertTrue(FileTestingUtils.compareFileSHA1s(originalFile, artifact));

  }

  @Test
  @Category(SECURITY.class)
  public void downloadArtifactFromInternalRepo()
      throws Exception
  {
    if (true) {
      printKnownErrorButDoNotFail(getClass(), "downloadArtifactFromInternalRepo");
      return;
    }

    Gav gav =
        new Gav(this.getTestId(), "release-jar-internal", "1", null, "jar", 0, new Date().getTime(),
            "Release Jar Internal", false, null, false, null);
    try {
      downloadArtifactFromRepository("Internal", gav, "./target/downloaded-jars");

      Assert.fail("Should throw 401 error");
    }
    catch (IOException e) {
      Assert.assertTrue(e.getMessage().contains("401"));
    }

  }
}
