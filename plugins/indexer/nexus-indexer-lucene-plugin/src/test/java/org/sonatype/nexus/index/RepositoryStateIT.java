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
import java.io.IOException;
import java.io.InputStream;

import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.AbstractContentLocator;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.FileContentLocator;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.Field;
import org.apache.maven.index.IteratorSearchResponse;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.expr.SourcedSearchExpression;
import org.junit.Assert;
import org.junit.Test;

public class RepositoryStateIT
    extends AbstractIndexerManagerTest
{
  private NexusIndexer mavenIndexer;

  private static final ContentLocator contentLocator = new AbstractContentLocator("", false, ContentLocator.UNKNOWN_LENGTH)
  {
    @Override
    public InputStream getContent()
        throws IOException
    {
      return null;
    }
  };

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();

    mavenIndexer = lookup(NexusIndexer.class);
  }

  @Override
  protected void tearDown()
      throws Exception
  {
    mavenIndexer = null;

    super.tearDown();
  }

  @Test
  public void testNotIndexable()
      throws Exception
  {
    releases.setIndexable(false);
    nexusConfiguration().saveConfiguration();
    wairForAsyncEventsToCalmDown();
    waitForTasksToStop();

    // ideally this test should verify that indexer manager ignores non-indexable repositories
    // but at least make sure indexer manager does not throw exceptions

    indexerManager.addRepositoryIndexContext(releases.getId());

    // not-indexable repository do not have indexing context
    Assert.assertNull(indexerManager.getRepositoryIndexContext(releases));

    // TODO figure out how to assert these operations don't do anything

    ResourceStoreRequest request = new ResourceStoreRequest("item", true, false);
    StorageFileItem item =
        new DefaultStorageFileItem(releases, request, true /* canRead */, true/* canWrite */, contentLocator);

    indexerManager.addItemToIndex(releases, item);
    indexerManager.removeItemFromIndex(apacheSnapshots, item);

    indexerManager.reindexRepository("/", releases.getId(), true);
    indexerManager.optimizeRepositoryIndex(releases.getId());

    Query query = new MatchAllDocsQuery();
    IteratorSearchResponse iterator =
        indexerManager.searchQueryIterator(query, releases.getId(), null /* from */, null/* count */,
            null/* hitLimit */, false/* uniqueRGA */, null/* filters */);
    iterator.close();

    iterator =
        indexerManager.searchQueryIterator(query, null/* repositoryId */, null /* from */, null/* count */,
            null/* hitLimit */, false/* uniqueRGA */, null/* filters */);
    iterator.close();

    indexerManager.removeRepositoryIndexContext(releases.getId(), false);
  }

  @Test
  public void testNotSearchable()
      throws Exception
  {
    // searchable flag only affects untargeted queries

    fillInRepo();

    snapshots.setSearchable(false);
    nexusConfiguration().saveConfiguration();
    wairForAsyncEventsToCalmDown();
    waitForTasksToStop();

    IndexingContext ctx = indexerManager.getRepositoryIndexContext(snapshots);

    Assert.assertNotNull(ctx);

    String itemPath =
        "/org/sonatype/plexus/plexus-plugin-manager/1.1-SNAPSHOT/plexus-plugin-manager-1.1-20081125.071530-1.pom";
    deployFile(snapshots, "apache-snapshots-2", itemPath);

    StorageItem item = createItem(snapshots, itemPath);

    indexerManager.addItemToIndex(snapshots, item);
    Assert.assertNull(search((MavenRepository) null, "org.sonatype.plexus", "plexus-plugin-manager",
        "1.1-SNAPSHOT"));
    Assert.assertNotNull(search(snapshots, "org.sonatype.plexus", "plexus-plugin-manager", "1.1-SNAPSHOT"));

    indexerManager.removeItemFromIndex(snapshots, item);
    Assert.assertNull(search((MavenRepository) null, "org.sonatype.plexus", "plexus-plugin-manager",
        "1.1-SNAPSHOT"));
    Assert.assertNull(search(snapshots, "org.sonatype.nexus", "plexus-plugin-manager", "1.1-SNAPSHOT"));

    indexerManager.reindexRepository(null, snapshots.getId(), true);
    // TODO assert

    indexerManager.optimizeRepositoryIndex(snapshots.getId());
    // TODO assert

    indexerManager.removeRepositoryIndexContext(snapshots.getId(), false);

    Assert.assertNull(indexerManager.getRepositoryIndexContext(snapshots));
  }

  private void deployFile(MavenRepository repository, String repoFrom, String path)
      throws IOException, UnsupportedStorageOperationException
  {
    File repos = new File(getBasedir(), "src/test/resources/reposes").getCanonicalFile();
    FileContentLocator content = new FileContentLocator(new File(repos, repoFrom + path), "mime-type");
    ResourceStoreRequest request = new ResourceStoreRequest(path);
    StorageItem item = new DefaultStorageFileItem(repository, request, true, true, content);
    repository.getLocalStorage().storeItem(repository, item);
  }

  private BooleanQuery newGavQuery(String groupId, String artifactId, String version) {
    BooleanQuery query = new BooleanQuery();
    addBooleanClause(query, MAVEN.GROUP_ID, groupId);
    addBooleanClause(query, MAVEN.ARTIFACT_ID, artifactId);
    addBooleanClause(query, MAVEN.VERSION, version);
    return query;
  }

  private void addBooleanClause(BooleanQuery query, Field field, String value) {
    query.add(mavenIndexer.constructQuery(field, new SourcedSearchExpression(value)),
        BooleanClause.Occur.MUST);
  }

  private ArtifactInfo search(MavenRepository repository, String groupId, String artifactId, String version)
      throws NoSuchRepositoryException, IOException
  {
    BooleanQuery query = newGavQuery(groupId, artifactId, version);
    String repoId = repository != null ? repository.getId() : null;
    IteratorSearchResponse iterator =
        indexerManager.searchQueryIterator(query, repoId, null, null, null, false, null);
    try {
      if (iterator.getTotalHitsCount() == 0) {
        return null;
      }
      Assert.assertEquals(1, iterator.getTotalHitsCount());
      ArtifactInfo ai = iterator.iterator().next();
      Assert.assertEquals(version, ai.version);
      return ai;
    }
    finally {
      iterator.close();
    }
  }

  private StorageItem createItem(MavenRepository repository, String path)
      throws Exception
  {
    return repository.retrieveItem(new ResourceStoreRequest(path));
  }
}
