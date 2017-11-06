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

import java.util.Collection;

import org.sonatype.nexus.proxy.RemoteStorageException;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.templates.TemplateManager;
import org.sonatype.nexus.templates.repository.RepositoryTemplate;
import org.sonatype.nexus.templates.repository.maven.Maven2ProxyRepositoryTemplate;

import org.junit.Assert;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.IteratorSearchResponse;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.treeview.DefaultTreeNodeFactory;
import org.apache.maven.index.treeview.TreeNode;
import org.junit.Test;

// This is an IT just because it runs longer then 15 seconds
public class DefaultIndexerManagerIT
    extends AbstractIndexerManagerTest
{
  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();
  }

  @Test
  public void testRepoReindex()
      throws Exception
  {
    fillInRepo();

    indexerManager.reindexAllRepositories("/", true);

    searchFor("org.sonatype.nexus", 10);

    assertTemporatyContexts(releases);
  }

  @Test
  public void testRepoKeywordSearch()
      throws Exception
  {
    fillInRepo();

    indexerManager.reindexAllRepositories("/", true);

    searchForKeywordNG("org.sonatype.nexus", 10);

    assertTemporatyContexts(releases);
  }

  @Test
  public void testRepoSha1Search()
      throws Exception
  {
    fillInRepo();

    indexerManager.reindexAllRepositories("/", true);

    // org.sonatype.nexus : nexus-indexer : 1.0-beta-4
    // sha1: 86e12071021fa0be4ec809d4d2e08f07b80d4877

    Collection<ArtifactInfo> ais =
        indexerManager.identifyArtifact(MAVEN.SHA1, "86e12071021fa0be4ec809d4d2e08f07b80d4877");

    assertTrue("The artifact has to be found!", ais.size() == 1);

    IteratorSearchResponse response;

    // this will be EXACT search, since we gave full SHA1 checksum of 40 chars
    response =
        indexerManager.searchArtifactSha1ChecksumIterator("86e12071021fa0be4ec809d4d2e08f07b80d4877", null, null,
            null, null, null);

    assertEquals("There should be one hit!", 1, response.getTotalHits());

    response.close();

    // this will be SCORED search, since we have just part of the SHA1 checksum
    response = indexerManager.searchArtifactSha1ChecksumIterator("86e12071021", null, null, null, null, null);

    assertEquals("There should be still one hit!", 1, response.getTotalHits());

    response.close();
  }

  @Test(expected = RemoteStorageException.class)
  public void testInvalidRemoteUrl()
      throws Exception
  {
    Maven2ProxyRepositoryTemplate t =
        (Maven2ProxyRepositoryTemplate) lookup(TemplateManager.class).getTemplate(RepositoryTemplate.class, "default_proxy_snapshot");
    t.getConfigurableRepository().setId("invalidUrlRepo");
    ProxyRepository r = t.create().adaptToFacet(ProxyRepository.class);
    r.setRemoteUrl("http://repository.sonatyp.org/content/repositories/snapshots");

    nexusConfiguration().saveConfiguration();

    indexerManager.reindexRepository("/", r.getId(), true);
  }

  @Test
  public void testDuplicateAddRepositoryRequest()
      throws Exception
  {
    MavenProxyRepository repo = central;

    IndexingContext repoCtx = indexerManager.getRepositoryIndexContext(repo);

    Assert.assertNotNull(repoCtx);

    indexerManager.addRepositoryIndexContext(repo);

    IndexingContext repoCtx2 = indexerManager.getRepositoryIndexContext(repo);

    Assert.assertNotSame(repoCtx, repoCtx2);
  }

  @Test
  public void testSearchIteratorAfterRepositoryDrop()
      throws Exception
  {
    fillInRepo();
    indexerManager.reindexAllRepositories("/", true);
    TreeNode node = indexerManager.listNodes(new DefaultTreeNodeFactory(central.getId()), "/", central.getId());
    indexerManager.removeRepositoryIndexContext(central, false);
    Assert.assertEquals(0, node.listChildren().size());
  }
}
