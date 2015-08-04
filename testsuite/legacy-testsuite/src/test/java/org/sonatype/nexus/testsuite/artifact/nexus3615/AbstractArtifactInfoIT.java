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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.RepositoryUrlResource;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

public abstract class AbstractArtifactInfoIT
    extends AbstractNexusIntegrationTest
{

  public AbstractArtifactInfoIT() {
    super();
  }

  public AbstractArtifactInfoIT(String testRepositoryId) {
    super(testRepositoryId);
  }

  @Override
  protected void deployArtifacts()
      throws Exception
  {
    super.deployArtifacts();

    File pom = getTestFile("artifact.pom");
    File jar = getTestFile("artifact.jar");
    getDeployUtils().deployUsingPomWithRest(REPO_TEST_HARNESS_REPO, jar, pom, null, null);
    getDeployUtils().deployUsingPomWithRest(REPO_TEST_HARNESS_REPO2, jar, pom, null, null);
    getDeployUtils().deployUsingPomWithRest(REPO_TEST_HARNESS_RELEASE_REPO, jar, pom, null, null);

    TaskScheduleUtil.waitForAllTasksToStop();
    getEventInspectorsUtil().waitForCalmPeriod();
  }

  protected Iterable<String> getRepositoryId(List<RepositoryUrlResource> repositories) {
    List<String> repoIds = new ArrayList<String>();
    for (RepositoryUrlResource repositoryUrlResource : repositories) {
      repoIds.add(repositoryUrlResource.getRepositoryId());
    }

    return repoIds;
  }

}