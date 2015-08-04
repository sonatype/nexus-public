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
package org.sonatype.nexus.testsuite.repo.nexus1633;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.repository.metadata.model.RepositoryMetadata;
import org.sonatype.nexus.repository.metadata.model.RepositoryMirrorMetadata;
import org.sonatype.nexus.repository.metadata.model.io.xpp3.RepositoryMetadataXpp3Reader;
import org.sonatype.nexus.rest.model.MirrorResource;
import org.sonatype.nexus.rest.model.MirrorResourceListRequest;
import org.sonatype.nexus.rest.model.MirrorResourceListResponse;
import org.sonatype.nexus.test.utils.MirrorMessageUtils;

import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.MediaType;

public class Nexus1633HostedMirrorMetadataIT
    extends AbstractNexusIntegrationTest
{
  protected MirrorMessageUtils messageUtil;

  private String repositoryId = "nexus-test-harness-repo";

  public Nexus1633HostedMirrorMetadataIT() {
    this.messageUtil = new MirrorMessageUtils(this.getJsonXStream(), MediaType.APPLICATION_JSON);
  }

  @Test
  public void setMirrorTest()
      throws IOException
  {
    MirrorResourceListRequest request = new MirrorResourceListRequest();

    MirrorResource resource = new MirrorResource();
    resource.setUrl("http://setMirrorTest1");
    request.addData(resource);

    resource = new MirrorResource();
    resource.setUrl("http://setMirrorTest2");
    request.addData(resource);

    this.messageUtil.setMirrors(repositoryId, request);
  }

  @Test
  public void updateMirrorTest()
      throws IOException
  {
    MirrorResourceListRequest request = new MirrorResourceListRequest();

    MirrorResource resource = new MirrorResource();
    resource.setUrl("http://updateMirrorTest1");
    request.addData(resource);

    resource = new MirrorResource();
    resource.setUrl("http://updateMirrorTest2");
    request.addData(resource);

    MirrorResourceListResponse response = this.messageUtil.setMirrors(repositoryId, request);

    request.setData(response.getData());

    resource = new MirrorResource();
    resource.setUrl("http://updateMirrorTest3");
    request.addData(resource);

    (request.getData().iterator().next()).setUrl("http://updateMirrorTest4");

    response = this.messageUtil.setMirrors(repositoryId, request);

    MirrorResource one = response.getData().get(0);
    MirrorResource two = response.getData().get(1);
    MirrorResource three = response.getData().get(2);

    Assert.assertEquals(one.getUrl(), "http://updateMirrorTest4");
    Assert.assertEquals(two.getUrl(), "http://updateMirrorTest2");
    Assert.assertEquals(three.getUrl(), "http://updateMirrorTest3");
  }

  @Test
  public void getMirrorTest()
      throws IOException
  {
    MirrorResourceListRequest request = new MirrorResourceListRequest();

    MirrorResource resource = new MirrorResource();
    resource.setUrl("http://getMirrorTest1");
    request.addData(resource);

    resource = new MirrorResource();
    resource.setUrl("http://getMirrorTest2");
    request.addData(resource);

    this.messageUtil.setMirrors(repositoryId, request);

    MirrorResourceListResponse response = this.messageUtil.getMirrors(repositoryId);

    MirrorResource one = response.getData().get(0);
    MirrorResource two = response.getData().get(1);

    Assert.assertEquals(one.getUrl(), "http://getMirrorTest1");
    Assert.assertEquals(two.getUrl(), "http://getMirrorTest2");
  }

  @Test
  public void validateMetadataChangeOnUpdate()
      throws Exception
  {
    MirrorResourceListRequest request = new MirrorResourceListRequest();

    MirrorResource resource = new MirrorResource();
    resource.setUrl("http://updateMirrorTest1");
    request.addData(resource);

    resource = new MirrorResource();
    resource.setUrl("http://updateMirrorTest2");
    request.addData(resource);

    resource = new MirrorResource();
    resource.setUrl("http://updateMirrorTest3");
    request.addData(resource);

    MirrorResourceListResponse response = messageUtil.setMirrors(repositoryId, request);

    Assert.assertEquals(3, response.getData().size());
    Assert.assertEquals((response.getData().get(0)).getId(), "http://updateMirrorTest1");
    Assert.assertEquals((response.getData().get(0)).getUrl(), "http://updateMirrorTest1");
    Assert.assertEquals((response.getData().get(1)).getId(), "http://updateMirrorTest2");
    Assert.assertEquals((response.getData().get(1)).getUrl(), "http://updateMirrorTest2");
    Assert.assertEquals((response.getData().get(2)).getId(), "http://updateMirrorTest3");
    Assert.assertEquals((response.getData().get(2)).getUrl(), "http://updateMirrorTest3");

    // Now read the metadata
    File metadata =
        new File(AbstractNexusIntegrationTest.nexusWorkDir + "/storage/" + repositoryId
            + "/.meta/repository-metadata.xml");

    RepositoryMetadataXpp3Reader reader = new RepositoryMetadataXpp3Reader();

    FileInputStream fis = new FileInputStream(metadata);

    try {
      RepositoryMetadata rm = reader.read(fis);

      Assert.assertEquals(3, rm.getMirrors().size());
      Assert.assertEquals(((RepositoryMirrorMetadata) rm.getMirrors().get(0)).getId(),
          "http://updateMirrorTest1");
      Assert.assertEquals(((RepositoryMirrorMetadata) rm.getMirrors().get(0)).getUrl(),
          "http://updateMirrorTest1");
      Assert.assertEquals(((RepositoryMirrorMetadata) rm.getMirrors().get(1)).getId(),
          "http://updateMirrorTest2");
      Assert.assertEquals(((RepositoryMirrorMetadata) rm.getMirrors().get(1)).getUrl(),
          "http://updateMirrorTest2");
      Assert.assertEquals(((RepositoryMirrorMetadata) rm.getMirrors().get(2)).getId(),
          "http://updateMirrorTest3");
      Assert.assertEquals(((RepositoryMirrorMetadata) rm.getMirrors().get(2)).getUrl(),
          "http://updateMirrorTest3");
    }
    finally {
      fis.close();
    }
  }
}
