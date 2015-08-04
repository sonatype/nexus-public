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
package org.sonatype.nexus.testsuite.index.nexus688;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Arrays;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;
import org.sonatype.nexus.rest.model.RepositoryProxyResource;
import org.sonatype.nexus.rest.model.RepositoryResource;
import org.sonatype.nexus.rest.model.RepositoryResourceRemoteStorage;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.MediaType;

public class Nexus688ReindexOnRepoAddIT
    extends AbstractNexusIntegrationTest
{
  private RepositoryMessageUtil messageUtil = new RepositoryMessageUtil(this, this.getXMLXStream(),
      MediaType.APPLICATION_XML);

  // Indexer stopped publishing "old" index for good
  // private static final String OLD_INDEX_FILE = ".index/nexus-maven-repository-index.zip";
  private static final String NEW_INDEX_FILE = ".index/nexus-maven-repository-index.gz";

  private static final String INDEX_PROPERTIES = ".index/nexus-maven-repository-index.properties";

  @BeforeClass
  public static void setSecureTest()
      throws ComponentLookupException
  {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void repoTestIndexable()
      throws Exception
  {

    // create a repo
    RepositoryResource resource = new RepositoryResource();

    resource.setId("nexus688-repoTestIndexable");
    resource.setRepoType("hosted");
    resource.setName("Create Test Repo");
    resource.setProvider("maven2");
    // format is neglected by server from now on, provider is the new guy in the town
    resource.setFormat("maven2");
    resource.setRepoPolicy(RepositoryPolicy.RELEASE.name());
    resource.setExposed(true);
    // invalid for hosted repo resource.setChecksumPolicy( "IGNORE" );
    resource.setBrowseable(true);
    resource.setIndexable(true);
    resource.setWritePolicy(RepositoryWritePolicy.ALLOW_WRITE.name());

    // this also validates
    this.messageUtil.createRepository(resource);

    TaskScheduleUtil.waitForAllTasksToStop();

    this.downloadIndexFromRepository(resource.getId(), true);
  }

  @Test
  public void repoTestNotIndexable()
      throws Exception
  {

    // create a repo
    RepositoryResource resource = new RepositoryResource();

    resource.setId("nexus688-repoTestNotIndexable");
    resource.setRepoType("hosted");
    resource.setName("Create Test Repo");
    resource.setProvider("maven2");
    // format is neglected by server from now on, provider is the new guy in the town
    resource.setFormat("maven2");
    resource.setRepoPolicy(RepositoryPolicy.RELEASE.name());
    resource.setExposed(true);
    // invalid for hosted repo resource.setChecksumPolicy( "IGNORE" );
    resource.setBrowseable(true);
    resource.setIndexable(false);
    resource.setWritePolicy(RepositoryWritePolicy.ALLOW_WRITE.name());

    // this also validates
    this.messageUtil.createRepository(resource);

    TaskScheduleUtil.waitForAllTasksToStop();

    this.downloadIndexFromRepository(resource.getId(), false);
  }

  @Test
  public void proxyRepoTestIndexableWithInvalidURL()
      throws Exception
  {

    // create a repo
    RepositoryProxyResource resource = new RepositoryProxyResource();

    resource.setId("nexus688-proxyRepoTestIndexableWithInvalidURL");
    resource.setRepoType("proxy");
    resource.setName("Create Test Repo");
    resource.setProvider("maven2");
    // format is neglected by server from now on, provider is the new guy in the town
    resource.setFormat("maven2");
    resource.setRepoPolicy(RepositoryPolicy.RELEASE.name());
    resource.setExposed(true);
    resource.setChecksumPolicy("IGNORE");
    resource.setBrowseable(true);
    resource.setIndexable(true);
    resource.setWritePolicy(RepositoryWritePolicy.ALLOW_WRITE.name());

    RepositoryResourceRemoteStorage remoteStorage = new RepositoryResourceRemoteStorage();
    remoteStorage.setRemoteStorageUrl("http://INVALID-URL/");
    resource.setRemoteStorage(remoteStorage);

    // this also validates
    this.messageUtil.createRepository(resource);

    TaskScheduleUtil.waitForAllTasksToStop();

    this.downloadIndexFromRepository(resource.getId(), true);
  }

  @Test
  public void proxyRepoTestIndexable()
      throws Exception
  {

    // create a repo
    RepositoryProxyResource resource = new RepositoryProxyResource();

    resource.setId("nexus688-proxyRepoTestIndexable");
    resource.setRepoType("proxy");
    resource.setName("Create Test Repo");
    resource.setProvider("maven2");
    // format is neglected by server from now on, provider is the new guy in the town
    resource.setFormat("maven2");
    resource.setRepoPolicy(RepositoryPolicy.RELEASE.name());
    resource.setChecksumPolicy("IGNORE");
    resource.setBrowseable(true);
    resource.setIndexable(true);
    resource.setExposed(true);
    resource.setWritePolicy(RepositoryWritePolicy.ALLOW_WRITE.name());

    RepositoryResourceRemoteStorage remoteStorage = new RepositoryResourceRemoteStorage();
    remoteStorage.setRemoteStorageUrl("http://INVALID-URL/");
    resource.setRemoteStorage(remoteStorage);

    // this also validates
    this.messageUtil.createRepository(resource);

    TaskScheduleUtil.waitForAllTasksToStop();

    this.downloadIndexFromRepository(resource.getId(), true);
  }

  @Test
  public void proxyRepoTestNotIndexable()
      throws Exception
  {

    // create a repo
    RepositoryProxyResource resource = new RepositoryProxyResource();

    resource.setId("nexus688-proxyRepoTestNotIndexable");
    resource.setRepoType("proxy");
    resource.setName("Create Test Repo");
    resource.setProvider("maven2");
    // format is neglected by server from now on, provider is the new guy in the town
    resource.setFormat("maven2");
    resource.setRepoPolicy(RepositoryPolicy.RELEASE.name());
    resource.setChecksumPolicy("IGNORE");
    resource.setBrowseable(true);
    resource.setIndexable(false);
    resource.setExposed(true);
    resource.setWritePolicy(RepositoryWritePolicy.ALLOW_WRITE.name());

    RepositoryResourceRemoteStorage remoteStorage = new RepositoryResourceRemoteStorage();
    remoteStorage.setRemoteStorageUrl("http://INVALID-URL/");
    resource.setRemoteStorage(remoteStorage);

    // this also validates
    this.messageUtil.createRepository(resource);

    TaskScheduleUtil.waitForAllTasksToStop();

    this.downloadIndexFromRepository(resource.getId(), false);
  }

  private void downloadIndexFromRepository(String repoId, boolean shouldSucceed)
      throws Exception
  {
    String repositoryUrl = this.getRepositoryUrl(repoId);

    URL url = null;

    // nexus does not publish old indexes anymore
    // URL url = new URL( repositoryUrl + OLD_INDEX_FILE );
    // downloadFromRepository( url, "target/downloads/index.zip", repoId, shouldSucceed );
    // url = new URL( repositoryUrl + OLD_INDEX_FILE + ".sha1" );
    // downloadFromRepository( url, "target/downloads/index.zip.sha1", repoId, shouldSucceed );
    // url = new URL( repositoryUrl + OLD_INDEX_FILE + ".md5" );
    // downloadFromRepository( url, "target/downloads/index.zip.md5", repoId, shouldSucceed );

    url = new URL(repositoryUrl + NEW_INDEX_FILE);
    downloadFromRepository(url, "target/downloads/index.gz", repoId, shouldSucceed);
    url = new URL(repositoryUrl + NEW_INDEX_FILE + ".sha1");
    downloadFromRepository(url, "target/downloads/index.gz.sha1", repoId, shouldSucceed);
    url = new URL(repositoryUrl + NEW_INDEX_FILE + ".md5");
    downloadFromRepository(url, "target/downloads/index.gz.md5", repoId, shouldSucceed);

    url = new URL(repositoryUrl + INDEX_PROPERTIES);
    downloadFromRepository(url, "target/downloads/index.properties", repoId, shouldSucceed);
    url = new URL(repositoryUrl + INDEX_PROPERTIES + ".sha1");
    downloadFromRepository(url, "target/downloads/index.properties.sha1", repoId, shouldSucceed);
    url = new URL(repositoryUrl + INDEX_PROPERTIES + ".md5");
    downloadFromRepository(url, "target/downloads/index.properties.md5", repoId, shouldSucceed);
  }

  private void downloadFromRepository(URL url, String target, String repoId, boolean shouldSucceed)
      throws Exception
  {
    try {
      this.downloadFile(url, target);
      if (!shouldSucceed) {
        Assert.fail("Expected 404, but file was downloaded");
      }
    }
    catch (FileNotFoundException e) {
      if (shouldSucceed) {
        Assert.fail(e.getMessage() + "\n Found files:\n"
            + Arrays.toString(new File(nexusWorkDir, "storage/" + repoId + "/.index").listFiles()));
      }
    }
  }

}
