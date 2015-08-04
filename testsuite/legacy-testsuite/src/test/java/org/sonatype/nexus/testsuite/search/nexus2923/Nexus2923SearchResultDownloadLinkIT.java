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
package org.sonatype.nexus.testsuite.search.nexus2923;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.sonatype.nexus.index.tasks.descriptors.UpdateIndexTaskDescriptor;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.rest.model.NexusArtifact;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.test.utils.ResponseMatchers;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.Method;

/**
 * Test the 'pom' and 'artifact' download link in the search result panel
 *
 * @author juven
 */
public class Nexus2923SearchResultDownloadLinkIT
    extends AbstractNexusIntegrationTest
{
  public Nexus2923SearchResultDownloadLinkIT() {
    super("nexus2923");
  }

  @Override
  public void runOnce()
      throws Exception
  {
    File testRepo = new File(nexusWorkDir, "storage/" + this.getTestRepositoryId());
    File testFiles = getTestFile("repo");
    FileUtils.copyDirectory(testFiles, testRepo);

    ScheduledServicePropertyResource prop = new ScheduledServicePropertyResource();
    prop.setKey("repositoryId");
    prop.setValue(this.getTestRepositoryId());

    TaskScheduleUtil.runTask(UpdateIndexTaskDescriptor.ID, prop);
  }

  @Test
  public void testDownnloadLinks()
      throws Exception
  {
    List<NexusArtifact> artifacts = getSearchMessageUtil().searchFor("xbean-server");
    Assert.assertEquals("The artifact should be indexed", artifacts.size(), 3);

    for (NexusArtifact artifact : artifacts) {
      if (StringUtils.isNotEmpty(artifact.getPomLink())) {
        assertLinkAvailable(artifact.getPomLink());
      }

      if (StringUtils.isNotEmpty(artifact.getArtifactLink())) {
        assertLinkAvailable(artifact.getArtifactLink());
      }
    }
  }

  private void assertLinkAvailable(String link)
      throws Exception
  {
    // use sendMessage because this is a non-service-URL (?)
    RequestFacade.sendMessage(new URL(link), Method.GET, null, ResponseMatchers.respondsWithStatusCode(307)).release();
  }
}
