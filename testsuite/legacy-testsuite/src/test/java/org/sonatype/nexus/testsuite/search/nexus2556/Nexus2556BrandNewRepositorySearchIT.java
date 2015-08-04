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
package org.sonatype.nexus.testsuite.search.nexus2556;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.proxy.maven.ChecksumPolicy;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;
import org.sonatype.nexus.rest.model.NexusArtifact;
import org.sonatype.nexus.rest.model.RepositoryResource;
import org.sonatype.nexus.test.utils.GavUtil;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.sonatype.nexus.test.utils.XStreamFactory;

import org.apache.maven.index.artifact.Gav;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.restlet.data.MediaType;

public class Nexus2556BrandNewRepositorySearchIT
    extends AbstractNexusIntegrationTest
{

  private RepositoryMessageUtil repoUtil;

  public Nexus2556BrandNewRepositorySearchIT() {

  }

  @Before
  public void init()
      throws ComponentLookupException
  {
    this.repoUtil = new RepositoryMessageUtil(this, XStreamFactory.getXmlXStream(), MediaType.APPLICATION_XML);
  }

  @Test
  public void hostedTest()
      throws IOException, Exception
  {
    String repoId = "nexus2556-hosted";
    RepositoryResource repo = new RepositoryResource();
    repo.setProvider("maven2");
    repo.setFormat("maven2");
    repo.setRepoPolicy("release");
    repo.setChecksumPolicy("ignore");
    repo.setBrowseable(false);

    repo.setId(repoId);
    repo.setName(repoId);
    repo.setRepoType("hosted");
    repo.setWritePolicy(RepositoryWritePolicy.ALLOW_WRITE.name());
    repo.setDownloadRemoteIndexes(true);
    repo.setBrowseable(true);
    repo.setRepoPolicy(RepositoryPolicy.RELEASE.name());
    repo.setChecksumPolicy(ChecksumPolicy.IGNORE.name());

    repo.setIndexable(true); // being sure!!!
    repoUtil.createRepository(repo);

    repo = (RepositoryResource) repoUtil.getRepository(repoId);
    Assert.assertTrue(repo.isIndexable());

    TaskScheduleUtil.waitForAllTasksToStop();
    getEventInspectorsUtil().waitForCalmPeriod();

    Gav gav = GavUtil.newGav("nexus2556", "artifact", "1.0");
    getDeployUtils().deployUsingGavWithRest(repoId, gav, getTestFile("artifact.jar"));

    TaskScheduleUtil.waitForAllTasksToStop();
    getEventInspectorsUtil().waitForCalmPeriod();

    List<NexusArtifact> result = getSearchMessageUtil().searchForGav(gav, repoId);
    Assert.assertEquals("Results: \n" + XStreamFactory.getXmlXStream().toXML(result), result.size(), 1);

    result = getSearchMessageUtil().searchFor(Collections.singletonMap("q", "nexus2556"), repoId);
    Assert.assertEquals("Results: \n" + XStreamFactory.getXmlXStream().toXML(result), result.size(), 1);
  }

}
