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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;

import org.sonatype.nexus.AbstractMavenRepoContentTests;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.scheduling.NexusScheduler;

import org.apache.lucene.search.Query;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.IteratorSearchResponse;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.SearchType;
import org.apache.maven.index.context.IndexingContext;

public abstract class AbstractIndexerManagerTest
    extends AbstractMavenRepoContentTests
{
  protected DefaultIndexerManager indexerManager;

  protected NexusScheduler nexusScheduler;

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();

    indexerManager = (DefaultIndexerManager) lookup(IndexerManager.class);

    nexusScheduler = lookup(NexusScheduler.class);
  }

  @Override
  protected boolean runWithSecurityDisabled() {
    return true;
  }

  protected void searchFor(String groupId, int expected)
      throws IOException
  {
    Query query = indexerManager.constructQuery(MAVEN.GROUP_ID, groupId, SearchType.EXACT);

    IteratorSearchResponse response;

    try {
      response = indexerManager.searchQueryIterator(query, null, null, null, null, false, null);
    }
    catch (NoSuchRepositoryException e) {
      // will not happen since we are not selecting a repo to search
      throw new IOException("Huh?");
    }

    try {
      ArrayList<ArtifactInfo> results = new ArrayList<ArtifactInfo>(response.getTotalHits());

      for (ArtifactInfo hit : response) {
        results.add(hit);
      }

      assertEquals("Query \"" + query + "\" returned wrong results: " + results + "!", expected, results.size());
    }
    finally {
      response.close();
    }
  }

  protected void searchForKeywordNG(String term, int expected)
      throws Exception
  {
    IteratorSearchResponse result =
        indexerManager.searchArtifactIterator(term, null, null, null, null, false, SearchType.SCORED, null);

    try {
      if (expected != result.getTotalHits()) {
        // dump the stuff
        StringBuilder sb = new StringBuilder("Found artifacts:\n");

        for (ArtifactInfo ai : result) {
          sb.append(ai.context).append(" : ").append(ai.toString()).append("\n");
        }

        fail(sb.toString() + "\nUnexpected result set size! We expected " + expected + " but got "
            + result.getTotalHits());
      }
    }
    finally {
      result.close();
    }
  }

  protected void searchFor(String groupId, int expected, String repoId)
      throws IOException, Exception
  {
    Query q = indexerManager.constructQuery(MAVEN.GROUP_ID, groupId, SearchType.EXACT);

    IteratorSearchResponse response = indexerManager.searchQueryIterator(q, repoId, null, null, null, false, null);
    try {
      ArrayList<ArtifactInfo> ais = new ArrayList<ArtifactInfo>(response.getTotalHits());

      for (ArtifactInfo ai : response) {
        ais.add(ai);
      }

      assertEquals(ais.toString(), expected, ais.size());
    }
    finally {
      response.close();
    }
  }

  protected void assertTemporatyContexts(final Repository repo)
      throws Exception
  {
    IndexingContext context =
        ((DefaultIndexerManager) indexerManager).getRepositoryIndexContext(repo.getId());
    File dir = context.getIndexDirectoryFile().getParentFile();

    File[] contextDirs = dir.listFiles(new FilenameFilter()
    {
      public boolean accept(File dir, String name) {
        return name.startsWith(repo.getId() + "-ctx");
      }
    });

    assertEquals(1, contextDirs.length);
  }
}