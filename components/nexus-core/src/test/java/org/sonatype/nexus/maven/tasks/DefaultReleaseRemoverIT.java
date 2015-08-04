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
package org.sonatype.nexus.maven.tasks;

import java.util.Collection;

import org.sonatype.nexus.AbstractMavenRepoContentTests;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.access.OpenAccessManager;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.maven2.Maven2ContentClass;
import org.sonatype.nexus.proxy.repository.AbstractRepository;
import org.sonatype.nexus.proxy.targets.Target;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

/**
 * @since 2.5
 */
public class DefaultReleaseRemoverIT
    extends AbstractMavenRepoContentTests
{
  // we have no index backend available here!
  private static final boolean INDEX_BACKEND = false;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private ReleaseRemover releaseRemover;

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();
    releaseRemover = lookup(ReleaseRemover.class);
  }

  @Test
  public void testDeletionOfReleases()
      throws Exception
  {
    fillInRepo();
    ((AbstractRepository) releases).setAccessManager(new OpenAccessManager());
    ReleaseRemovalResult releaseRemovalResult =
        releaseRemover.removeReleases(new ReleaseRemovalRequest(releases.getId(), 2, INDEX_BACKEND, ""));
    // pom + jar + sha1 for both
    assertThat(releaseRemovalResult.getDeletedFileCount(), is(6));
    assertThat(releaseRemovalResult.isSuccessful(), is(true));
    StorageItem item = null;
    try {
      item = releases.retrieveItem(new ResourceStoreRequest("org/sonatype/test/1.0"));
    }
    catch (ItemNotFoundException e) {
      //we expect this cannot be found
    }
    assertThat(item, nullValue());

    item = releases.retrieveItem(new ResourceStoreRequest("org/sonatype/test/1.0.1"));
    assertThat(item, notNullValue());
  }

  @Test
  public void testDeletionOfReleasesWithTarget()
      throws Exception
  {
    fillInRepo();
    ((AbstractRepository) releases).setAccessManager(new OpenAccessManager());
    targetRegistry.addRepositoryTarget(
        new Target("test", "test", new Maven2ContentClass(), Lists.newArrayList(".*/org/sonatype/.*")));
    targetRegistry.commitChanges();
    ReleaseRemovalResult releaseRemovalResult =
        releaseRemover.removeReleases(new ReleaseRemovalRequest(releases.getId(), 2, INDEX_BACKEND, "test"));
    assertThat(releaseRemovalResult.getDeletedFileCount(), is(6));
    assertThat(releaseRemovalResult.isSuccessful(), is(true));
  }

  @Test
  public void testDeletionOfReleasesWithNonMatchingTarget()
      throws Exception
  {
    fillInRepo();
    ((AbstractRepository) releases).setAccessManager(new OpenAccessManager());
    targetRegistry.addRepositoryTarget(
        new Target("test", "test", new Maven2ContentClass(), Lists.newArrayList(".*/com/sonatype/.*")));
    targetRegistry.commitChanges();
    ReleaseRemovalResult releaseRemovalResult =
        releaseRemover.removeReleases(new ReleaseRemovalRequest(releases.getId(), 2, INDEX_BACKEND, "test"));
    assertThat(releaseRemovalResult.getDeletedFileCount(), is(0));
    assertThat(releaseRemovalResult.isSuccessful(), is(true));
    try {
      StorageItem storageItem = releases.retrieveItem(new ResourceStoreRequest("org/sonatype/test/1.0"));
    }
    catch (ItemNotFoundException e) {
      Assert.fail("Files should not have been deleted, but we can't find them now");
    }
  }

  @Test
  public void testDeletionOfReleasesWithMissingTarget()
      throws Exception
  {
    thrown.expect(IllegalStateException.class);
    releaseRemover.removeReleases(new ReleaseRemovalRequest(releases.getId(), 2, INDEX_BACKEND, "test"));
  }


  @Test
  public void testDeletionOfReleasesExceptForSources()
      throws Exception
  {
    fillInRepo();
    ((AbstractRepository) releases).setAccessManager(new OpenAccessManager());
    ReleaseRemovalResult releaseRemovalResult =
        releaseRemover.removeReleases(new ReleaseRemovalRequest(releases.getId(), 2, INDEX_BACKEND, "3"));
    // pom + jar + sha1 for both, but sources jar and associated sha1 should be left alone
    assertThat(releaseRemovalResult.getDeletedFileCount(), is(4));
    assertThat(releaseRemovalResult.isSuccessful(), is(true));
    StorageItem item = releases.retrieveItem(new ResourceStoreRequest("org/sonatype/test/1.0"));
    assertThat(item, notNullValue());
    StorageCollectionItem coll = (StorageCollectionItem) item;
    Collection<StorageItem> storageItems = releases.list(false, coll);
    assertThat(storageItems, hasSize(2));
    for (StorageItem storageItem : storageItems) {
      assertThat(storageItem.getName(), containsString("sources"));
    }

    item = releases.retrieveItem(new ResourceStoreRequest("org/sonatype/test/1.0.1"));
    assertThat(item, notNullValue());
  }
}
