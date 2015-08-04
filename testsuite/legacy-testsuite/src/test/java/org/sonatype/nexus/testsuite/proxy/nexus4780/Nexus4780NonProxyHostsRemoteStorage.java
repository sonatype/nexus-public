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
package org.sonatype.nexus.testsuite.proxy.nexus4780;

import java.io.File;
import java.io.IOException;

import org.sonatype.nexus.integrationtests.ITGroups.PROXY;
import org.sonatype.nexus.testsuite.proxy.AbstractNexusWebProxyIntegrationTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.sonatype.nexus.test.utils.FileTestingUtils.createSHA1FromFile;

/**
 * This test ensures that the commons-httpclient (v3) and httpclient v4 remote storage implementations
 * work in a proxied environment with non-proxy-hosts.
 *
 * A web proxy is set up in the nexus configuration but localhost is set up as a NonProxyHost, so the web proxy should
 * not be accessed..
 */
public class Nexus4780NonProxyHostsRemoteStorage
    extends AbstractNexusWebProxyIntegrationTest
{

  /**
   * Retrieve two artifacts from a proxy repo set up with the httpclient v3 remote storage.
   */
  @Test
  @Category(PROXY.class)
  public void apache3xDownloadArtifactNonProxyHost()
      throws Exception
  {
    retrieveAndAssertArtifacts(getNexusTestRepoUrl("apache3x"));
  }

  /**
   * Retrieve artifacts from a proxy repo set up with the httpclient v4 remote storage.
   */
  @Test
  @Category(PROXY.class)
  public void apache4xDownloadArtifactNonProxyHost()
      throws Exception
  {
    retrieveAndAssertArtifacts(getNexusTestRepoUrl("apache4x"));
  }

  private void retrieveAndAssertArtifacts(final String repoUrl)
      throws IOException
  {
    File pomFile = this.getLocalFile("release-proxy-repo-1", "nexus4780", "artifact", "1.0", "pom");
    File jarFile = this.getLocalFile("release-proxy-repo-1", "nexus4780", "artifact", "1.0", "jar");

    File pomArtifact = this.downloadArtifact(repoUrl, "nexus4780", "artifact", "1.0", "pom", null, "target/downloads");
    File jarArtifact = this.downloadArtifact(repoUrl, "nexus4780", "artifact", "1.0", "jar", null, "target/downloads");

    assertThat(createSHA1FromFile(pomArtifact), equalTo(createSHA1FromFile(pomFile)));
    assertThat(createSHA1FromFile(jarArtifact), equalTo(createSHA1FromFile(jarFile)));
    assertThat("proxy was used", server.getAccessedUris(), hasSize(0));
  }
}
