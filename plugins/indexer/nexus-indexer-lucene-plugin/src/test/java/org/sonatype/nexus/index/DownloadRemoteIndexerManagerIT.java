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
package org.sonatype.nexus.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.repository.ProxyMode;
import org.sonatype.nexus.test.http.RemoteRepositories;

import org.apache.commons.io.FileUtils;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.treeview.DefaultTreeNodeFactory;
import org.apache.maven.index.treeview.TreeNode;
import org.junit.Assert;
import org.junit.Test;

// This is an IT just because it runs longer then 15 seconds
public class DownloadRemoteIndexerManagerIT
    extends AbstractIndexerManagerTest
{
  private RemoteRepositories remoteRepositories;

  private File fakeCentral;

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();

    fakeCentral = new File(getBasedir(), "target/repos/fake-central");
    fakeCentral.mkdirs();

    // create proxy server
    remoteRepositories = RemoteRepositories.builder()
        .repo("central", fakeCentral.getAbsolutePath())
        .build().start();

    // update central to use proxy server
    central.setDownloadRemoteIndexes(true);
    central.setRemoteUrl(remoteRepositories.getUrl("central"));
    central.setRepositoryPolicy(RepositoryPolicy.SNAPSHOT);

    nexusConfiguration().saveConfiguration();

    Thread.sleep(100);

    wairForAsyncEventsToCalmDown();
    waitForTasksToStop();
  }

  @Override
  protected void tearDown()
      throws Exception
  {
    remoteRepositories.stop();
    FileUtils.forceDelete(fakeCentral);
    super.tearDown();
  }

  @Test
  public void testRepoReindex()
      throws Exception
  {
    File index1 = new File(getBasedir(), "src/test/resources/repo-index/index");
    File index2 = new File(getBasedir(), "src/test/resources/repo-index/index2");
    File centralIndex = new File(fakeCentral, ".index");

    // copy index 02
    overwriteIndex(index2, centralIndex);

    super.indexerManager.reindexRepository(null, central.getId(), true);

    searchFor("org.sonatype.nexus", 8, central.getId());

    assertRootGroups();

    // copy index 01
    overwriteIndex(index1, centralIndex);

    super.indexerManager.reindexRepository(null, central.getId(), true);

    searchFor("org.sonatype.nexus", 1, central.getId());

    assertRootGroups();

    // copy index 02
    overwriteIndex(index2, centralIndex);

    super.indexerManager.reindexRepository(null, central.getId(), true);

    searchFor("org.sonatype.nexus", 8, central.getId());

    assertRootGroups();
  }

  /**
   * All set okay, but repo in question has ProxyMode that does not allow remote access.
   *
   * @since 2.7.0
   */
  @Test
  public void testRepoNoReindexProxyModeNotAllowsRemoteAccess()
      throws Exception
  {
    File index2 = new File(getBasedir(), "src/test/resources/repo-index/index2");
    File centralIndex = new File(fakeCentral, ".index");

    // copy index 02
    overwriteIndex(index2, centralIndex);

    central.setProxyMode(ProxyMode.BLOCKED_MANUAL);
    central.commitChanges();

    super.indexerManager.reindexRepository(null, central.getId(), true);

    // nothing found as no remote access allowed, index was NOT downloaded
    searchFor("org.sonatype.nexus", 0, central.getId());
  }

  private void assertRootGroups()
      throws NoSuchRepositoryException, IOException
  {
    TreeNode node = indexerManager.listNodes(new DefaultTreeNodeFactory(central.getId()), "/", central.getId());
    Assert.assertEquals(1, node.getChildren().size());
    Assert.assertEquals("/org/", node.getChildren().get(0).getPath());
  }

  private void overwriteIndex(File source, File destination)
      throws Exception
  {
    File indexFile = new File(destination, "nexus-maven-repository-index.gz");
    File indexProperties = new File(destination, "nexus-maven-repository-index.properties");

    long lastMod = -1;
    if (destination.exists()) {
      FileUtils.forceDelete(destination);
      lastMod = indexFile.lastModified();
    }
    FileUtils.copyDirectory(source, destination);
    long lastMod2 = indexFile.lastModified();
    assertTrue(lastMod < lastMod2);

    Properties p = new Properties();
    InputStream input = new FileInputStream(indexProperties);
    p.load(input);
    input.close();

    p.setProperty("nexus.index.time", format(new Date()));
    p.setProperty("nexus.index.timestamp", format(new Date()));

    OutputStream output = new FileOutputStream(indexProperties);
    p.store(output, null);
    output.close();
  }

  private String format(Date d) {
    SimpleDateFormat df = new SimpleDateFormat(IndexingContext.INDEX_TIME_FORMAT);
    df.setTimeZone(TimeZone.getTimeZone("GMT"));
    return df.format(d);
  }
}
