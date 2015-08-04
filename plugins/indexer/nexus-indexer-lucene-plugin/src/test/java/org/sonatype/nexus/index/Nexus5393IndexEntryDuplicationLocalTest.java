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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import org.sonatype.nexus.proxy.ResourceStoreRequest;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.context.IndexingContext;
import org.junit.Assert;
import org.junit.Test;

/**
 * UT for NEXUS-5393: Hosted repositories are getting duplicate entries for locally stored entries.
 * <p/>
 * Manual steps to reproduce:
 * 1. start nexus
 * 2. deploy to nexus
 * 3. verify indexes are okay for log4j-1.2.13 (and have one entry)
 * 4. perform Update Indexes on Central
 * 5. verify you have two entries for log4j-1.2.13
 * <p/>
 * Validated against master on 9f9748aa9cdeefa9548b4487c2057223136c511b
 * this UT fails. Branch scanning-issues make it pass.
 */
public class Nexus5393IndexEntryDuplicationLocalTest
    extends AbstractIndexerManagerTest
{

  private static final String POM_PATH = "/log4j/log4j/1.2.13/log4j-1.2.13.pom";

  private static final String JAR_PATH = "/log4j/log4j/1.2.13/log4j-1.2.13.jar";

  private File fakeCentral;

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();
    fakeCentral = new File(getBasedir(), "target/test-classes/nexus-5393/remote-repository");

  }

  protected void waitForAsync()
      throws Exception
  {
    // wait a bit for async stuff
    Thread.sleep(100);
    wairForAsyncEventsToCalmDown();
    waitForTasksToStop();
  }

  protected void ensureUniqueness()
      throws IOException
  {
    final IndexingContext context = indexerManager.getRepositoryIndexContext("releases");
    final HashSet<String> uinfos = new HashSet<String>();
    final ArrayList<String> duplicates = new ArrayList<String>();
    final IndexSearcher indexSearcher = context.acquireIndexSearcher();
    try {
      final IndexReader r = indexSearcher.getIndexReader();
      for (int i = 0; i < r.maxDoc(); i++) {
        if (!r.isDeleted(i)) {
          final Document d = r.document(i);
          String uinfo = d.get(ArtifactInfo.UINFO);
          if (uinfo != null && !uinfos.add(uinfo)) {
            duplicates.add(uinfo);
          }
        }
      }
    }
    finally {
      context.releaseIndexSearcher(indexSearcher);
    }

    // remote proxy contains only one artifact: log4j-1.2.13: so we expect out index to have no
    // dupes and only one artifact
    if (!duplicates.isEmpty() || uinfos.size() > 1) {
      Assert.fail(
          "UINFOs are duplicated or we scanned some unexpected ones, duplicates=" + duplicates + ", uinfos="
              + uinfos);
    }
  }

  @Test
  public void entryDuplicationTestWithUpdateIndex()
      throws Exception
  {
    // check uniqueness
    ensureUniqueness();
    // simulate Maven deploy, get something stored
    releases.storeItem(new ResourceStoreRequest(POM_PATH),
        new FileInputStream(new File(fakeCentral, POM_PATH.substring(1))), null);
    releases.storeItem(new ResourceStoreRequest(JAR_PATH),
        new FileInputStream(new File(fakeCentral, JAR_PATH.substring(1))), null);
    waitForAsync(); // indexing happens async
    // check uniqueness
    ensureUniqueness();
    // update indexes, that will trigger buggy scan of local storage
    indexerManager.reindexRepository(null, central.getId(), false);
    // check uniqueness
    ensureUniqueness();
  }

  @Test
  public void entryDuplicationTestWithRepairIndex()
      throws Exception
  {
    // check uniqueness
    ensureUniqueness();
    // simulate Maven deploy, get something stored
    releases.storeItem(new ResourceStoreRequest(POM_PATH),
        new FileInputStream(new File(fakeCentral, POM_PATH.substring(1))), null);
    releases.storeItem(new ResourceStoreRequest(JAR_PATH),
        new FileInputStream(new File(fakeCentral, JAR_PATH.substring(1))), null);
    waitForAsync(); // indexing happens async
    // check uniqueness
    ensureUniqueness();
    // update indexes, that will trigger buggy scan of local storage
    indexerManager.reindexRepository(null, central.getId(), true);
    // check uniqueness
    ensureUniqueness();
  }
}
