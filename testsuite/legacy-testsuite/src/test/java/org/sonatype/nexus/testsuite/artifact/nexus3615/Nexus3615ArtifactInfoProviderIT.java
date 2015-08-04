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

import org.sonatype.nexus.rest.model.ArtifactInfoResource;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;

public class Nexus3615ArtifactInfoProviderIT
    extends AbstractArtifactInfoIT
{

  @Test
  public void repoInfo()
      throws Exception
  {
    ArtifactInfoResource info =
        getSearchMessageUtil().getInfo(REPO_TEST_HARNESS_REPO, "nexus3615/artifact/1.0/artifact-1.0.jar");

    Assert.assertEquals(info.getRepositoryId(), REPO_TEST_HARNESS_REPO);

    validate(info);
  }

  @Test
  public void groupInfo()
      throws Exception
  {
    ArtifactInfoResource info =
        getSearchMessageUtil().getInfo("public", "nexus3615/artifact/1.0/artifact-1.0.jar");

    // artifact is deployed to the 3 repos mentioned here. Depending on indexer order, any one of these may be the one for getRepositoryId()
    assertThat(info.getRepositoryId(), Matchers.isOneOf(REPO_TEST_HARNESS_REPO, REPO_TEST_HARNESS_REPO2,
        REPO_TEST_HARNESS_RELEASE_REPO));
    validate(info);

  }

  private void validate(ArtifactInfoResource info) {
    Assert.assertEquals(info.getRepositoryPath(), "/nexus3615/artifact/1.0/artifact-1.0.jar");
    Assert.assertEquals(info.getSha1Hash(), "b354a0022914a48daf90b5b203f90077f6852c68");
    Assert.assertEquals(3, info.getRepositories().size());
    MatcherAssert.assertThat(getRepositoryId(info.getRepositories()),
        hasItems(REPO_TEST_HARNESS_REPO, REPO_TEST_HARNESS_REPO2,
            REPO_TEST_HARNESS_RELEASE_REPO));
    Assert.assertEquals(info.getMimeType(), "application/java-archive");
    Assert.assertEquals(info.getSize(), 1364);
  }

}
