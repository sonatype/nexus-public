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

import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;

import junit.framework.Assert;
import org.apache.maven.index.context.ContextMemberProvider;
import org.junit.Test;

/**
 * IT for Indexer related to Group repositories.
 */
public class GroupUpdateIndexerManagerIT
    extends AbstractIndexerManagerTest
{

  /**
   * Testing is a group member change correctly propagated to Maven Indexer IndexingContexts.
   */
  @Test
  public void testGroupUpdate()
      throws Exception
  {
    // This test was disabled when Nexus had issues with merging group indexes (back, when MergedIndexingContext
    // was not existing in MI (groups had "normal", usually huuge indexes, and they had issues on group member
    // changes as huge index was alway recreated.
    // Uncommented and updated

    // add repo content
    fillInRepo();

    // our patient
    final GroupRepository group = (GroupRepository) repositoryRegistry.getRepository("public");

    // reindex
    indexerManager.reindexAllRepositories(null, true);

    // Note: public by default contains snapshots repository as member
    // Snapshots repository is the only one that contains GroupID "org.sonatype.plexus".
    // Meaning, the presence of this search (is in result set or not) is used
    // to validate the group member changes.
    // Similarly, Apache Snapshots repository by default is NOT member of
    // Public, and it is the only one having artifact with groupID "org.sonatype.test-evict".

    // assure context does exists (if Igor comments out few lines or so, as MI will plainly swallow targeted search
    // against nonexistent context!)
    Assert.assertNotNull(((DefaultIndexerManager) indexerManager).getRepositoryIndexContext(group));

    // do searches
    searchFor("org.sonatype.plexus", 1, "snapshots"); // is in this repo
    searchFor("org.sonatype.plexus", 1, group.getId()); // snapshots is member of public
    searchFor("org.sonatype.test-evict", 1, "apache-snapshots"); // is in this repo
    searchFor("org.sonatype.test-evict", 0, group.getId()); // apache-snapshots not a member

    // reconfigure group: remove snapshots
    group.removeMemberRepositoryId(snapshots.getId());
    nexusConfiguration().saveConfiguration();
    waitForTasksToStop();
    wairForAsyncEventsToCalmDown();

    // assure context does exists (if Igor comments out few lines or so, as MI will plainly swallow targeted search
    // against nonexistent context!)
    Assert.assertNotNull(((DefaultIndexerManager) indexerManager).getRepositoryIndexContext(group));

    // do searches
    searchFor("org.sonatype.plexus", 1, "snapshots"); // is in this repo
    searchFor("org.sonatype.plexus", 0, group.getId()); // snapshots is not member of public
    searchFor("org.sonatype.test-evict", 1, "apache-snapshots"); // is in this repo
    searchFor("org.sonatype.test-evict", 0, group.getId()); // apache-snapshots not a member

    // reconfigure group: add apache-snapshots
    group.addMemberRepositoryId(apacheSnapshots.getId());
    nexusConfiguration().saveConfiguration();
    waitForTasksToStop();
    wairForAsyncEventsToCalmDown();

    // assure context does exists (if Igor comments out few lines or so, as MI will plainly swallow targeted search
    // against nonexistent context!)
    Assert.assertNotNull(((DefaultIndexerManager) indexerManager).getRepositoryIndexContext(group));

    // do searches
    searchFor("org.sonatype.plexus", 1, "snapshots"); // is in this repo
    searchFor("org.sonatype.plexus", 0, group.getId()); // snapshots is not member of public
    searchFor("org.sonatype.test-evict", 1, "apache-snapshots"); // is in this repo
    searchFor("org.sonatype.test-evict", 1, group.getId()); // apache-snapshots is a member of public
  }

  /**
   * Testing is a group member's configuration change correctly propagated to Maven Indexer IndexingContexts. This
   * propagation become needed as {@link ContextMemberProvider} implementations were changed to serve up unmodified
   * pre-built lists. But in special case that is NOT member removal (config change made on group, see above), but
   * rather making a repository non-indexable (config change made against one of the member), the context of given
   * member repo is being shut down and removed. Hence, the list returned by ContextMemberProvider needs change too,
   * as it would return a "shut down" context.
   */
  @Test
  public void testGroupMemberUpdate()
      throws Exception
  {
    // add repo content
    fillInRepo();

    // our patients, group and one member
    final GroupRepository group = (GroupRepository) repositoryRegistry.getRepository("public");
    final Repository member = repositoryRegistry.getRepository("snapshots");

    // reindex
    indexerManager.reindexAllRepositories(null, true);

    // Note: public by default contains snapshots repository as member
    // Snapshots repository is the only one that contains GroupID "org.sonatype.plexus".
    // Meaning, the presence of this search (is in result set or not) is used
    // to validate the group member changes.

    // assure context does exists (if Igor comments out few lines or so, as MI will plainly swallow targeted search
    // against nonexistent context!)
    Assert.assertNotNull(((DefaultIndexerManager) indexerManager).getRepositoryIndexContext(group));
    Assert.assertNotNull(((DefaultIndexerManager) indexerManager).getRepositoryIndexContext(member));

    // do searches
    searchFor("org.sonatype.plexus", 1, member.getId()); // is in this repo
    searchFor("org.sonatype.plexus", 1, group.getId()); // snapshots is member of public

    // reconfigure group member: make it non-indexable
    member.setSearchable(false);
    member.setIndexable(false);
    nexusConfiguration().saveConfiguration();
    waitForTasksToStop();
    wairForAsyncEventsToCalmDown();

    // assure context does exists (if Igor comments out few lines or so, as MI will plainly swallow targeted search
    // against nonexistent context!)
    Assert.assertNotNull(((DefaultIndexerManager) indexerManager).getRepositoryIndexContext(group));
    Assert.assertNull(((DefaultIndexerManager) indexerManager).getRepositoryIndexContext(member));

    // do searches
    searchFor("org.sonatype.plexus", 0, member.getId()); // is in this repo
    searchFor("org.sonatype.plexus", 0, group.getId()); // snapshots is not member of public

    // reconfigure group: add apache-snapshots
    member.setSearchable(true);
    member.setIndexable(true);
    nexusConfiguration().saveConfiguration();
    waitForTasksToStop();
    wairForAsyncEventsToCalmDown();

    // assure context does exists (if Igor comments out few lines or so, as MI will plainly swallow targeted search
    // against nonexistent context!)
    Assert.assertNotNull(((DefaultIndexerManager) indexerManager).getRepositoryIndexContext(group));
    Assert.assertNotNull(((DefaultIndexerManager) indexerManager).getRepositoryIndexContext(member));

    // do searches
    searchFor("org.sonatype.plexus", 1, member.getId()); // is in this repo
    searchFor("org.sonatype.plexus", 1, group.getId()); // snapshots is not member of public
  }

}
