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
package org.sonatype.nexus.testsuite.search.nexus602;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;

import org.apache.maven.index.artifact.Gav;
import org.junit.Test;
import org.restlet.data.Method;
import org.restlet.data.Response;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.test.utils.ResponseMatchers.isRedirecting;
import static org.sonatype.nexus.test.utils.ResponseMatchers.isSuccessful;
import static org.sonatype.nexus.test.utils.ResponseMatchers.redirectLocation;
import static org.sonatype.nexus.test.utils.ResponseMatchers.respondsWithStatusCode;
import static org.sonatype.nexus.test.utils.StatusMatchers.isNotFound;

/**
 * Test snapshot search results can be downloaded.
 */
public class Nexus602SearchSnapshotArtifactIT
    extends AbstractNexusIntegrationTest
{
  /**
   * Which contains both, the release and snap repo where this test has artifacts deployed, see resources/nexus602
   * and
   * it's test-config
   */
  private static final String NEXUS_602_GROUP = "nexus-602-nexus-test-harness-group";

  private final Gav gav;

  public Nexus602SearchSnapshotArtifactIT()
      throws Exception
  {
    gav = new Gav("nexus602", "artifact", "1.0-SNAPSHOT", null, "jar", 0, 0L, null, false, null, false, null);
  }

  @Override
  protected void runOnce()
      throws Exception
  {
    // we must add GA level repository metadata, something not done by IT when it "deploys"
    // GA level metadata is needed by two searchWildcardLatestOverAGroup() and searchWildcardReleaseOverAGroup()
    // to perform proper maven-like resolution
    final File gamdRelease = getTestFile("gamd-release.xml");
    getDeployUtils().deployWithWagon("http",
        RequestFacade.toNexusURL("content/repositories/nexus-test-harness-repo").toString(), gamdRelease,
        gav.getGroupId() + "/" + gav.getArtifactId() + "/maven-metadata.xml");

    final File gamdSnapshot = getTestFile("gamd-snapshot.xml");
    getDeployUtils().deployWithWagon("http",
        RequestFacade.toNexusURL("content/repositories/nexus-test-harness-snapshot-repo").toString(),
        gamdSnapshot, gav.getGroupId() + "/" + gav.getArtifactId() + "/maven-metadata.xml");
  }

  @Test
  public void searchSnapshot()
      throws Exception
  {
    String serviceURI =
        "service/local/artifact/maven/redirect?r=" + REPO_TEST_HARNESS_SNAPSHOT_REPO + "&g=" + gav.getGroupId()
            + "&a=" + gav.getArtifactId() + "&v=" + gav.getVersion();
    assertRedirection(serviceURI);
  }

  @Test
  public void searchRelease()
      throws Exception
  {
    String serviceURI =
        "service/local/artifact/maven/redirect?r=" + REPO_TEST_HARNESS_REPO + "&g=" + gav.getGroupId() + "&a="
            + "artifact" + "&v=" + "1.0";

    assertRedirection(serviceURI);
  }

  @Test
  public void searchSnapshotOverAGroup()
      throws Exception
  {
    String serviceURI =
        "service/local/artifact/maven/redirect?r=" + NEXUS_602_GROUP + "&g=" + gav.getGroupId() + "&a="
            + gav.getArtifactId() + "&v=" + gav.getVersion();
    assertRedirection(serviceURI);
  }

  @Test
  public void searchReleaseOverAGroup()
      throws Exception
  {
    String serviceURI =
        "service/local/artifact/maven/redirect?r=" + NEXUS_602_GROUP + "&g=" + gav.getGroupId() + "&a="
            + "artifact" + "&v=" + "1.0";

    assertRedirection(serviceURI);
  }

  @Test
  public void searchWildcardLatestOverAGroup()
      throws Exception
  {
    String serviceURI =
        "service/local/artifact/maven/redirect?r=" + NEXUS_602_GROUP + "&g=" + gav.getGroupId() + "&a="
            + gav.getArtifactId() + "&v=LATEST";
    assertRedirection(serviceURI);
  }

  @Test
  public void searchWildcardReleaseOverAGroup()
      throws Exception
  {
    String serviceURI =
        "service/local/artifact/maven/redirect?r=" + NEXUS_602_GROUP + "&g=" + gav.getGroupId() + "&a="
            + gav.getArtifactId() + "&v=RELEASE";
    assertRedirection(serviceURI);
  }

  @Test
  public void searchInvalidArtifact()
      throws Exception
  {
    String serviceURI =
        "service/local/artifact/maven/redirect?r=" + REPO_TEST_HARNESS_REPO + "&g=" + "invalidGroupId" + "&a="
            + "invalidArtifact" + "&v=" + "32.64";

    RequestFacade.doGetForStatus(serviceURI, isNotFound());
  }

  private void assertRedirection(final String serviceURI)
      throws IOException
  {
    Response response = null;

    try {
      response = RequestFacade.doGetRequest(serviceURI);
      assertThat(
          response,
          allOf(isRedirecting(), respondsWithStatusCode(307), redirectLocation(notNullValue(String.class))));

      RequestFacade.releaseResponse(response);

      response =
          RequestFacade.sendMessage(new URL(response.getLocationRef().toString()), Method.GET, null,
              isSuccessful());
    }
    finally {
      RequestFacade.releaseResponse(response);
    }
  }

}
