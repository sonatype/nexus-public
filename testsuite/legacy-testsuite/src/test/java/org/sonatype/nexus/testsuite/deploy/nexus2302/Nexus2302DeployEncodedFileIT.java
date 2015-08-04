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
package org.sonatype.nexus.testsuite.deploy.nexus2302;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.ContentListResource;
import org.sonatype.nexus.rest.model.NexusArtifact;
import org.sonatype.nexus.test.utils.ContentListMessageUtil;
import org.sonatype.nexus.test.utils.FileTestingUtils;
import org.sonatype.nexus.test.utils.MavenDeployer;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.apache.commons.io.IOUtils;
import org.apache.maven.index.artifact.Gav;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.restlet.data.MediaType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class Nexus2302DeployEncodedFileIT
    extends AbstractNexusIntegrationTest
{

  public Nexus2302DeployEncodedFileIT() {
    super.setTestRepositoryId(REPO_TEST_HARNESS_REPO);
  }

  @Test
  public void plusSign()
      throws Exception
  {
    Gav gav = new Gav("nexus2302", "artifact", "1.0", "c++", "jar", null, null, null, false, null, false, null);
    testIt(gav);
  }

  @Test
  public void version()
      throws Exception
  {
    Gav gav = new Gav("nexus2302", "artifact", "1++0", null, "jar", null, null, null, false, null, false, null);
    testIt(gav);
  }

  @Test
  public void dolarSign()
      throws Exception
  {
    Gav gav =
        new Gav("nexus2302", "artifact", "$dolar", "void", "jar", null, null, null, false, null, false, null);
    testIt(gav);
  }

  public void testIt(Gav gav)
      throws VerificationException, IOException, Exception
  {
    final File file = getTestFile("artifact.jar");
    Verifier v =
        MavenDeployer.deployAndGetVerifier(gav, getRepositoryUrl(REPO_TEST_HARNESS_REPO), file,
            getOverridableFile("settings.xml"));
    v.verifyErrorFreeLog();

    getEventInspectorsUtil().waitForCalmPeriod();
    TaskScheduleUtil.waitForAllTasksToStop();

    // direct download
    assertTrue(FileTestingUtils.compareFileSHA1s(file,
        downloadArtifact(gav, "target/nexus2302/" + gav.getArtifactId() + ".jar")));

    // redirect download
    assertTrue(FileTestingUtils.compareFileSHA1s(file,
        downloadSnapshotArtifact(REPO_TEST_HARNESS_REPO, gav, new File("target/nexus2302"))));

    checkFileSystem(gav);
    checkIndex(gav);
    checkRepoBrowse(gav);
    checkRepoBrowse(gav);
    checkBrowse(gav);
    checkContentBrowse(gav, null);
    checkContentBrowse(gav, "?abc=321");
    delete(gav);
  }

  private void checkContentBrowse(Gav gav, String query)
      throws Exception
  {
    if (query == null) {
      query = "";
    }

    URL url = new URL(nexusBaseUrl + "content/repositories/" + REPO_TEST_HARNESS_REPO + "/");
    String content = IOUtils.toString(url.openStream());
    assertThat(content, containsString(gav.getGroupId()));

    url = new URL(url.toString() + gav.getGroupId() + "/");
    assertThat(content, containsString(url.toString()));
    content = IOUtils.toString(url.openStream());
    assertThat(content, containsString(gav.getArtifactId()));

    url = new URL(url.toString() + gav.getArtifactId() + "/");
    assertThat(content, containsString(url.toString()));
    content = IOUtils.toString(url.openStream());
    assertThat(content, containsString(gav.getVersion()));

    url = new URL(url.toString() + gav.getVersion() + "/");
    assertThat(content, containsString(url.toString()));
    content = IOUtils.toString(url.openStream());
    assertThat(content, containsString(gav.getArtifactId()));

    String clas = gav.getClassifier() == null ? "" : "-" + gav.getClassifier();
    url = new URL(url.toString() + gav.getArtifactId() + "-" + gav.getVersion() + clas + "." + gav.getExtension());
    assertThat(content, containsString(url.toString()));
  }

  private void checkBrowse(Gav gav)
      throws Exception
  {
    URL url = new URL(nexusBaseUrl + "service/local/repositories/" + REPO_TEST_HARNESS_REPO + "/content/");
    String content = IOUtils.toString(url.openStream());
    assertThat(content, containsString(url.toString()));
    assertThat(content, containsString(gav.getGroupId()));

    url = new URL(url.toString() + gav.getGroupId() + "/");
    content = IOUtils.toString(url.openStream());
    assertThat(content, containsString(url.toString()));
    assertThat(content, containsString(gav.getArtifactId()));

    url = new URL(url.toString() + gav.getArtifactId() + "/");
    content = IOUtils.toString(url.openStream());
    assertThat(content, containsString(url.toString()));
    assertThat(content, containsString(gav.getVersion()));

    url = new URL(url.toString() + gav.getVersion() + "/");
    content = IOUtils.toString(url.openStream());
    assertThat(content, containsString(url.toString()));
    assertThat(content, containsString(gav.getArtifactId()));
    if (gav.getClassifier() != null) {
      assertThat(content, containsString(gav.getClassifier()));
    }
  }

  private void checkRepoBrowse(Gav gav)
      throws Exception
  {
    ContentListMessageUtil contentUtil =
        new ContentListMessageUtil(this.getXMLXStream(), MediaType.APPLICATION_XML);

    List<ContentListResource> result = contentUtil.getContentListResource(REPO_TEST_HARNESS_REPO, "/", false);

    ContentListResource g = select(result, gav.getGroupId());
    assertThat(g.getResourceURI(), equalTo(nexusBaseUrl + "service/local/repositories/" + REPO_TEST_HARNESS_REPO
        + "/content/" + gav.getGroupId() + "/"));

    result = contentUtil.getContentListResource(REPO_TEST_HARNESS_REPO, g.getRelativePath(), false);

    ContentListResource a = select(result, gav.getArtifactId());
    assertThat(a.getResourceURI(), equalTo(nexusBaseUrl + "service/local/repositories/" + REPO_TEST_HARNESS_REPO
        + "/content/" + gav.getGroupId() + "/" + gav.getArtifactId() + "/"));

    result = contentUtil.getContentListResource(REPO_TEST_HARNESS_REPO, a.getRelativePath(), false);

    ContentListResource v = select(result, gav.getVersion());
    assertThat(v.getResourceURI(), equalTo(nexusBaseUrl + "service/local/repositories/" + REPO_TEST_HARNESS_REPO
        + "/content/" + gav.getGroupId() + "/" + gav.getArtifactId() + "/" + gav.getVersion() + "/"));

    result = contentUtil.getContentListResource(REPO_TEST_HARNESS_REPO, v.getRelativePath(), false);

    String clas = gav.getClassifier() == null ? "" : "-" + gav.getClassifier();
    ContentListResource c =
        select(result, gav.getArtifactId() + "-" + gav.getVersion() + clas + "." + gav.getExtension());
    assertNotNull(c);
    assertThat(
        c.getResourceURI(),
        equalTo(nexusBaseUrl + "service/local/repositories/" + REPO_TEST_HARNESS_REPO + "/content/"
            + gav.getGroupId() + "/" + gav.getArtifactId() + "/" + gav.getVersion() + "/" + gav.getArtifactId()
            + "-" + gav.getVersion() + clas + "." + gav.getExtension()));

  }

  private ContentListResource select(List<ContentListResource> result, String text) {
    assertFalse(result.isEmpty());
    ContentListResource g = null;
    for (ContentListResource content : result) {
      if (content.getText().equals(text)) {
        g = content;
      }
    }
    assertNotNull(text + " not found", g);
    assertThat(g.getResourceURI(), containsString(text));

    return g;
  }

  private void checkIndex(Gav gav)
      throws Exception
  {
    List<NexusArtifact> result =
        getSearchMessageUtil().searchForGav(gav.getGroupId(), gav.getArtifactId(), gav.getVersion(),
            REPO_TEST_HARNESS_REPO);
    assertResult(gav, result, false);

    result =
        getSearchMessageUtil().searchForGav(gav.getGroupId(), gav.getArtifactId(), gav.getVersion(),
            gav.getExtension(), gav.getClassifier(), REPO_TEST_HARNESS_REPO);
    assertResult(gav, result, true);
  }

  private void assertResult(Gav gav, List<NexusArtifact> result, boolean assertClassifier) {
    assertFalse(result.isEmpty());

    assertThat(result.get(0).getGroupId(), equalTo(gav.getGroupId()));
    assertThat(result.get(0).getArtifactId(), equalTo(gav.getArtifactId()));
    assertThat(result.get(0).getVersion(), equalTo(gav.getVersion()));
    if (assertClassifier) {
      assertThat(result.get(0).getClassifier(), equalTo(gav.getClassifier()));
    }
  }

  private void checkFileSystem(Gav gav)
      throws IOException
  {
    File artifact =
        new File(nexusWorkDir, "storage/" + REPO_TEST_HARNESS_REPO + "/" + getRelitiveArtifactPath(gav));

    assertTrue("File not found: " + artifact.getAbsolutePath(), artifact.exists());
  }

  private void delete(Gav gav)
      throws IOException
  {
    assertTrue(deleteFromRepository(REPO_TEST_HARNESS_REPO, getRelitiveArtifactPath(gav)));
  }

}
