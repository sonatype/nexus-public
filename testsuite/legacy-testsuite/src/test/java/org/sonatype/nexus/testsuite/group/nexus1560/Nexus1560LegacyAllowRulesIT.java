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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URL;

import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeDescriptor;
import org.sonatype.nexus.test.utils.ResponseMatchers;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class Nexus1560LegacyAllowRulesIT
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
    addPriv(TEST_USER_NAME, REPO_TEST_HARNESS_REPO + "-priv", TargetPrivilegeDescriptor.TYPE, "1",
        REPO_TEST_HARNESS_REPO, null, "read");

    // Now need the view priv as well
    addPrivilege(TEST_USER_NAME, "repository-" + REPO_TEST_HARNESS_REPO);
  }

  @Test
  public void fromRepository()
      throws Exception
  {
    String downloadUrl =
        REPOSITORY_RELATIVE_URL + REPO_TEST_HARNESS_REPO + "/" + getRelitiveArtifactPath(gavArtifact1);

    download(downloadUrl, ResponseMatchers.isSuccessful());
  }

  @Test
  public void fromGroup()
      throws Exception
  {
    String downloadUrl =
        GROUP_REPOSITORY_RELATIVE_URL + NEXUS1560_GROUP + "/" + getRelitiveArtifactPath(gavArtifact1);

    download(downloadUrl, ResponseMatchers.respondsWithStatusCode(403));
  }

  @Test(expected = FileNotFoundException.class)
  public void checkMetadataOnGroup()
      throws Exception
  {
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    downloadFile(new URL(nexusBaseUrl + GROUP_REPOSITORY_RELATIVE_URL + NEXUS1560_GROUP
        + "/nexus1560/artifact/maven-metadata.xml"), "./target/downloads/nexus1560/repo-maven-metadata.xml");
  }

  @Test
  public void checkMetadataOnRepository()
      throws Exception
  {
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    File file =
        downloadFile(new URL(nexusBaseUrl + REPOSITORY_RELATIVE_URL + REPO_TEST_HARNESS_REPO
            + "/nexus1560/artifact/maven-metadata.xml"), "./target/downloads/nexus1560/repo-maven-metadata.xml");
    Xpp3Dom dom = Xpp3DomBuilder.build(new FileReader(file));
    Xpp3Dom[] versions = dom.getChild("versioning").getChild("versions").getChildren("version");
    for (Xpp3Dom version : versions) {
      Assert.assertEquals("Invalid version available on metadata" + dom.toString(), version.getValue(), "1.0");
    }
  }

  @Test
  public void artifact2FromGroup()
      throws Exception
  {
    String downloadUrl =
        GROUP_REPOSITORY_RELATIVE_URL + NEXUS1560_GROUP + "/" + getRelitiveArtifactPath(gavArtifact2);

    assertDownloadFails(downloadUrl);
  }

  @Test
  public void artifact2FromRepo()
      throws Exception
  {
    String downloadUrl =
        REPOSITORY_RELATIVE_URL + REPO_TEST_HARNESS_REPO + "/" + getRelitiveArtifactPath(gavArtifact2);

    assertDownloadFails(downloadUrl);
  }

  @Test
  public void artifact2FromRepo2()
      throws Exception
  {
    String downloadUrl =
        REPOSITORY_RELATIVE_URL + REPO_TEST_HARNESS_REPO2 + "/" + getRelitiveArtifactPath(gavArtifact2);

    assertDownloadFails(downloadUrl);
  }

}
