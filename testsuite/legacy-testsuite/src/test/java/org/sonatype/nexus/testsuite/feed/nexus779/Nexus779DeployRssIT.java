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
package org.sonatype.nexus.testsuite.feed.nexus779;

import java.io.File;

import org.junit.Test;

public class Nexus779DeployRssIT
    extends AbstractRssIT
{

  @Test
  public void restDeployRssCheck()
      throws Exception
  {
    deployRest("artifact1");
    feedListContainsArtifact("nexus779", "artifact1", "1.0");
    deployRest("artifact2");
    feedListContainsArtifact("nexus779", "artifact2", "1.0");
  }

  @Test
  public void wagonDeployRSSCheck()
      throws Exception
  {
    deployWagon("artifact3");
    feedListContainsArtifact("nexus779", "artifact3", "1.0");

    deployWagon("artifact4");
    feedListContainsArtifact("nexus779", "artifact4", "1.0");
  }

  private void deployWagon(String artifactName)
      throws Exception
  {
    File jarFile = getTestFile(artifactName + ".jar");
    File pomFile = getTestFile(artifactName + ".pom");

    String deployUrl = nexusBaseUrl + "content/repositories/" + REPO_TEST_HARNESS_REPO;
    getDeployUtils().deployWithWagon("http", deployUrl, jarFile, "nexus779/" + artifactName + "/1.0/"
        + artifactName + "-1.0.jar");
    getDeployUtils().deployWithWagon("http", deployUrl, pomFile, "nexus779/" + artifactName + "/1.0/"
        + artifactName + "-1.0.pom");

  }

  private int deployRest(String artifactName)
      throws Exception
  {
    File jarFile = getTestFile(artifactName + ".jar");
    File pomFile = getTestFile(artifactName + ".pom");

    int status = getDeployUtils().deployUsingPomWithRest(REPO_TEST_HARNESS_REPO, jarFile, pomFile, "", "jar");
    return status;
  }

}
