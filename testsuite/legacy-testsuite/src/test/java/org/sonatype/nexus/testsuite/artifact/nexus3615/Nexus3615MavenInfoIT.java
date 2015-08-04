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
package org.sonatype.nexus.testsuite.artifact.nexus3615;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.rest.model.Maven2ArtifactInfoResource;
import org.sonatype.nexus.rest.model.Maven2ArtifactInfoResourceRespose;

import com.thoughtworks.xstream.XStream;
import org.apache.maven.index.artifact.Gav;
import org.junit.Assert;
import org.junit.Test;

import static org.sonatype.nexus.test.utils.NexusRequestMatchers.hasStatusCode;
import static org.sonatype.nexus.test.utils.NexusRequestMatchers.respondsWithStatusCode;

/**
 * Tests the ?describe=maven2 content view.
 *
 * @author Brian Demers
 */
public class Nexus3615MavenInfoIT
    extends AbstractNexusIntegrationTest
{

  /**
   * Positive release tests.
   */
  @Test
  public void deployAndRunReleaseTests()
      throws Exception
  {
    // deploy releases
    Gav simpleJarGav =
        new Gav("nexus3615", "simpleJar", "1.0.1", null, "jar", null, null, null, false, null, false, null);
    deployGav(simpleJarGav, getTestRepositoryId());
    downloadAndVerify(simpleJarGav, getTestRepositoryId());

    Gav withClassifierGav =
        new Gav("nexus3615", "simpleJar", "1.0.1", "classifier", "jar", null, null, null, false, null, false, null);
    deployGav(withClassifierGav, getTestRepositoryId());
    downloadAndVerify(withClassifierGav, getTestRepositoryId());

    Gav withExtentionGav =
        new Gav("nexus3615", "simpleJar", "1.0.1", null, "extention", null, null, null, false, null, false, null);
    deployGav(withExtentionGav, getTestRepositoryId());
    downloadAndVerify(withExtentionGav, getTestRepositoryId());

    Gav withClassifierAndExtentionGav =
        new Gav("nexus3615", "simpleJar", "1.0.1", "classifier", "extention", null, null, null, false, null,
            false, null);
    deployGav(withClassifierAndExtentionGav, getTestRepositoryId());
    downloadAndVerify(withClassifierAndExtentionGav, getTestRepositoryId());
  }

  /**
   * Positive release tests.
   */
  @Test
  public void deployAndRunSnapshotTests()
      throws Exception
  {
    // deploy releases
    Gav simpleJarGav =
        new Gav("nexus3615", "simpleJar", "1.0.1-SNAPSHOT", null, "jar", 1, System.currentTimeMillis(), null,
            false, null, false, null);
    deployGav(simpleJarGav, REPO_TEST_HARNESS_SNAPSHOT_REPO);
    downloadAndVerify(simpleJarGav, REPO_TEST_HARNESS_SNAPSHOT_REPO);

    Gav withClassifierGav =
        new Gav("nexus3615", "simpleJar", "1.0.1-SNAPSHOT", "classifier", "jar", 2, System.currentTimeMillis(),
            null, false, null, false, null);
    deployGav(withClassifierGav, REPO_TEST_HARNESS_SNAPSHOT_REPO);
    downloadAndVerify(withClassifierGav, REPO_TEST_HARNESS_SNAPSHOT_REPO);

    Gav withExtentionGav =
        new Gav("nexus3615", "simpleJar", "1.0.1-SNAPSHOT", null, "extention", 3, System.currentTimeMillis(),
            null, false, null, false, null);
    deployGav(withExtentionGav, REPO_TEST_HARNESS_SNAPSHOT_REPO);
    downloadAndVerify(withExtentionGav, REPO_TEST_HARNESS_SNAPSHOT_REPO);

    Gav withClassifierAndExtentionGav =
        new Gav("nexus3615", "simpleJar", "1.0.1-SNAPSHOT", "classifier", "extention", 4,
            System.currentTimeMillis(), null, false, null, false, null);
    deployGav(withClassifierAndExtentionGav, REPO_TEST_HARNESS_SNAPSHOT_REPO);
    downloadAndVerify(withClassifierAndExtentionGav, REPO_TEST_HARNESS_SNAPSHOT_REPO);
  }

  @Test
  public void testNonGavArtifacts()
      throws Exception
  {
    // deploy a non maven path
    getDeployUtils().deployWithWagon("http", getRepositoryUrl(getTestRepositoryId()),
        getTestFile("pom.xml"), "foo/bar");

    // now get the info for it
    String serviceURIpart =
        "service/local/repositories/" + getTestRepositoryId() + "/content/" + "foo/bar" + "?describe=maven2";
    RequestFacade.doGet(serviceURIpart, respondsWithStatusCode(404));
  }

  public void deployGav(Gav gav, String repoId)
      throws Exception
  {
    getDeployUtils().deployWithWagon("http", getRepositoryUrl(repoId), getTestFile("simpleJar.jar"),
        getRelitiveArtifactPath(gav));
  }

  @Test
  public void notFoundTest()
      throws Exception
  {
    Gav releaseNotFoundGav =
        new Gav("nexus3615", "notFound", "1.0.1", null, "jar", null, null, null, false, null, false, null);
    RequestFacade.doGetForStatus(getServiceUriPart(releaseNotFoundGav, "maven2", getTestRepositoryId()),
        hasStatusCode(404));

    Gav snapshotNotFoundGav =
        new Gav("nexus3615", "notFound", "1.0.1-SNAPSHOT", null, "jar", 1, System.currentTimeMillis(), null,
            false, null, false, null);
    RequestFacade.doGetForStatus(getServiceUriPart(snapshotNotFoundGav, "maven2", getTestRepositoryId()),
        hasStatusCode(404));
  }

  private void downloadAndVerify(Gav gav, String repoId)
      throws IOException
  {
    Maven2ArtifactInfoResource data = downloadViewResource(gav, repoId);

    Assert.assertEquals(data.getArtifactId(), gav.getArtifactId());
    Assert.assertEquals(data.getGroupId(), gav.getGroupId());

    if (gav.isSnapshot() && gav.getSnapshotTimeStamp() != null) {
      // time stamp snapshot
      String expectedVersion =
          gav.getVersion().replaceFirst(
              "SNAPSHOT",
              new SimpleDateFormat("yyyyMMdd.HHmmss").format(new Date(gav.getSnapshotTimeStamp())) + "-"
                  + gav.getSnapshotBuildNumber());
      Assert.assertEquals(data.getVersion(), expectedVersion);
    }
    else {
      // non snapshot
      Assert.assertEquals(data.getVersion(), gav.getVersion());
    }
    Assert.assertEquals(data.getBaseVersion(), gav.getBaseVersion());
    Assert.assertEquals(data.getClassifier(), gav.getClassifier());
    Assert.assertEquals(data.getExtension(), gav.getExtension());

    Assert.assertEquals(buildExpectedDepBlock(gav), data.getDependencyXmlChunk());
  }

  private Maven2ArtifactInfoResource downloadViewResource(Gav gav, String repoId)
      throws IOException
  {
    XStream xstream = getXMLXStream();

    String responseText = RequestFacade.doGetForText(getServiceUriPart(gav, "maven2", repoId));

    return ((Maven2ArtifactInfoResourceRespose) xstream.fromXML(responseText)).getData();
  }

  private String buildExpectedDepBlock(Gav gav) {
    StringBuilder buffer = new StringBuilder();

    buffer.append("<dependency>\n");
    buffer.append("  <groupId>").append(gav.getGroupId()).append("</groupId>\n");
    buffer.append("  <artifactId>").append(gav.getArtifactId()).append("</artifactId>\n");
    buffer.append("  <version>").append(gav.getBaseVersion()).append("</version>\n");

    if (gav.getClassifier() != null) {
      buffer.append("  <classifier>").append(gav.getClassifier()).append("</classifier>\n");
    }

    if (gav.getExtension() != null && !gav.getExtension().equals("jar")) {
      buffer.append("  <type>").append(gav.getExtension()).append("</type>\n");
    }

    buffer.append("</dependency>");

    return buffer.toString();
  }

  private String getServiceUriPart(Gav gav, String describeKey, String repoId)
      throws FileNotFoundException
  {
    return "service/local/repositories/" + repoId + "/content/" + getRelitiveArtifactPath(gav) + "?describe="
        + describeKey;
  }
}
