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

import java.io.IOException;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.maven.tasks.descriptors.RebuildMavenMetadataTaskDescriptor;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.test.utils.NexusRequestMatchers;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.apache.maven.index.artifact.Gav;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.restlet.data.Response;

public abstract class AbstractLegacyRulesIT
    extends AbstractPrivilegeTest
{

  protected static final String NEXUS1560_GROUP = "nexus1560-group";

  protected Gav gavArtifact1;

  protected Gav gavArtifact2;

  @Before
  public void createGav1()
      throws Exception
  {
    this.gavArtifact1 =
        new Gav("nexus1560", "artifact", "1.0", null, "jar", null, null, null, false, null, false, null);
    this.gavArtifact2 =
        new Gav("nexus1560", "artifact", "2.0", null, "jar", null, null, null, false, null, false, null);
  }

  @Override
  protected void runOnce()
      throws Exception
  {
    super.runOnce();

    ScheduledServicePropertyResource repo = new ScheduledServicePropertyResource();
    repo.setKey("repositoryId");
    repo.setValue(REPO_TEST_HARNESS_REPO);
    TaskScheduleUtil.runTask("nexus1560-repo", RebuildMavenMetadataTaskDescriptor.ID, repo);
    ScheduledServicePropertyResource repo2 = new ScheduledServicePropertyResource();
    repo2.setKey("repositoryId");
    repo2.setValue(REPO_TEST_HARNESS_REPO2);
    TaskScheduleUtil.runTask("nexus1560-repo2", RebuildMavenMetadataTaskDescriptor.ID, repo2);
  }

  protected void download(String downloadUrl, Matcher<Response> matcher)
      throws IOException
  {
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    RequestFacade.doGet(downloadUrl, matcher);
  }

  protected void assertDownloadFails(String downloadUrl)
      throws IOException
  {
    download(downloadUrl, NexusRequestMatchers.inError());
  }

  protected void assertDownloadSucceeds(String downloadUrl)
      throws IOException
  {
    download(downloadUrl, NexusRequestMatchers.isSuccessful());
  }

}