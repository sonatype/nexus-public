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
package org.sonatype.nexus.testsuite.repo.nexus1581;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.proxy.maven.ChecksumPolicy;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;
import org.sonatype.nexus.repository.metadata.model.RepositoryMetadata;
import org.sonatype.nexus.repository.metadata.model.RepositoryMirrorMetadata;
import org.sonatype.nexus.repository.metadata.model.io.xpp3.RepositoryMetadataXpp3Reader;
import org.sonatype.nexus.repository.metadata.model.io.xpp3.RepositoryMetadataXpp3Writer;
import org.sonatype.nexus.rest.model.MirrorResource;
import org.sonatype.nexus.rest.model.MirrorResourceListResponse;
import org.sonatype.nexus.rest.model.RepositoryResource;
import org.sonatype.nexus.rest.model.RepositoryResourceRemoteStorage;
import org.sonatype.nexus.test.utils.MirrorMessageUtils;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.restlet.data.MediaType;

public class Nexus1581MirrorMetadataIT
    extends AbstractNexusIntegrationTest
{
  private MirrorMessageUtils mirrorUtils;

  private static final String PROXY_REPO_ID = "nexus1581-proxy";

  public Nexus1581MirrorMetadataIT() {

  }

  @Before
  public void startUp() {
    mirrorUtils = new MirrorMessageUtils(this.getJsonXStream(), MediaType.APPLICATION_JSON);
  }

  @Test
  public void testMetadata()
      throws Exception
  {
    createProxyRepository();

    File metadata =
        new File(AbstractNexusIntegrationTest.nexusWorkDir
            + "/storage/nexus-test-harness-repo/.meta/repository-metadata.xml");

    RepositoryMetadataXpp3Reader reader = new RepositoryMetadataXpp3Reader();
    RepositoryMetadataXpp3Writer writer = new RepositoryMetadataXpp3Writer();

    FileInputStream fis = new FileInputStream(metadata);
    FileWriter fw = new FileWriter(metadata);

    RepositoryMetadata rm = null;

    try {
      rm = reader.read(fis);
    }
    catch (Exception e) {
      rm = new RepositoryMetadata();
    }
    finally {
      fis.close();
    }

    rm.getMirrors().clear();

    RepositoryMirrorMetadata rmm = new RepositoryMirrorMetadata();
    rmm.setId("id1");
    rmm.setUrl("http://localhost:8081/somemirror/");

    rm.addMirror(rmm);

    rmm = new RepositoryMirrorMetadata();
    rmm.setId("id2");
    rmm.setUrl("http://localhost:8086/somemirror2/");

    rm.addMirror(rmm);

    rmm = new RepositoryMirrorMetadata();
    rmm.setId("id3");
    rmm.setUrl("http://localhost:8086/somemirror3/");

    rm.addMirror(rmm);

    try {
      writer.write(fw, rm);
    }
    finally {
      fw.close();
    }

    MirrorResourceListResponse response = mirrorUtils.getPredefinedMirrors(PROXY_REPO_ID);

    Assert.assertEquals(3, response.getData().size());
    Assert.assertEquals(((MirrorResource) response.getData().get(0)).getId(), "id1");
    Assert.assertEquals(((MirrorResource) response.getData().get(0)).getUrl(),
        "http://localhost:8081/somemirror/");
    Assert.assertEquals(((MirrorResource) response.getData().get(1)).getId(), "id2");
    Assert.assertEquals(((MirrorResource) response.getData().get(1)).getUrl(),
        "http://localhost:8086/somemirror2/");
    Assert.assertEquals(((MirrorResource) response.getData().get(2)).getId(), "id3");
    Assert.assertEquals(((MirrorResource) response.getData().get(2)).getUrl(),
        "http://localhost:8086/somemirror3/");
  }

  protected void createProxyRepository()
      throws Exception
  {
    RepositoryResource resource = new RepositoryResource();

    resource.setProvider("maven2");
    resource.setFormat("maven2");
    resource.setRepoPolicy("release");
    resource.setChecksumPolicy("ignore");
    resource.setBrowseable(false);
    resource.setIndexable(false);

    resource.setId(PROXY_REPO_ID);
    resource.setName(PROXY_REPO_ID);
    resource.setRepoType("proxy");
    resource.setWritePolicy(RepositoryWritePolicy.READ_ONLY.name());
    resource.setDownloadRemoteIndexes(true);
    RepositoryResourceRemoteStorage remoteStorage = new RepositoryResourceRemoteStorage();
    remoteStorage.setRemoteStorageUrl(getBaseNexusUrl() + "content/repositories/nexus-test-harness-repo/");
    resource.setRemoteStorage(remoteStorage);
    resource.setRepoPolicy(RepositoryPolicy.RELEASE.name());
    resource.setChecksumPolicy(ChecksumPolicy.IGNORE.name());
    new RepositoryMessageUtil(this, this.getJsonXStream(), MediaType.APPLICATION_JSON).createRepository(resource);
  }
}
