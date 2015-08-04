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
package org.sonatype.nexus.testsuite.repo.nexus4579;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.maven.tasks.descriptors.SnapshotRemovalTaskDescriptor;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.test.utils.GavUtil;
import org.sonatype.nexus.test.utils.MavenDeployer;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * IT testing move to trash/delete immediately behavior of the SnapshotRemovalTask.
 *
 * @see org.sonatype.nexus.proxy.wastebasket.DeleteOperation
 */
public class Nexus4579OptionalTrashForSnapshotsIT
    extends AbstractNexusIntegrationTest
{

  private File artifactFolder;

  private File repositoryPath;

  private File trashArtifactFolder;

  private File groupFolder;

  private File trashGroupIdFolder;

  private File trashPath;

  public Nexus4579OptionalTrashForSnapshotsIT() {
    super("nexus-test-harness-snapshot-repo");
  }

  @Before
  public void deploySnapshotArtifacts()
      throws Exception
  {
    repositoryPath = new File(nexusWorkDir, "storage/nexus-test-harness-snapshot-repo");

    trashPath = new File(repositoryPath, ".nexus/trash");

    final String groupIdRelativePath = "nexus4579/";
    final String artifactRelativePath = groupIdRelativePath + "artifact/1.0-SNAPSHOT";

    groupFolder = new File(repositoryPath, groupIdRelativePath);
    artifactFolder = new File(repositoryPath, artifactRelativePath);
    trashGroupIdFolder = new File(trashPath, groupIdRelativePath);
    trashArtifactFolder = new File(trashPath, artifactRelativePath);

    File oldSnapshot = getTestFile("repo");

    // Copying to keep an old timestamp
    FileUtils.copyDirectory(oldSnapshot, repositoryPath);

    RepositoryMessageUtil.updateIndexes(getTestRepositoryId());

    MavenDeployer.deployAndGetVerifier(GavUtil.newGav("nexus4579", "artifact", "1.0-SNAPSHOT"),
        getRepositoryUrl(getTestRepositoryId()),
        getTestFile("deploy/artifact/artifact.jar"), null);
    MavenDeployer.deployAndGetVerifier(GavUtil.newGav("nexus4579", "artifact", "1.0-SNAPSHOT", "pom"),
        getRepositoryUrl(getTestRepositoryId()),
        getTestFile("deploy/artifact/pom.xml"), null);

    TaskScheduleUtil.waitForAllTasksToStop();
    getEventInspectorsUtil().waitForCalmPeriod();
  }

  @After
  public void cleanTrash()
      throws IOException
  {
    // clean trash before tests to remove possible leftovers from previous tests
    // trashPath may be null if super.@Before failed
    if (trashPath != null) {
      FileUtils.deleteDirectory(trashPath);
    }
  }

  @Test
  public void removeAllSnapshotsToTrash()
      throws Exception
  {
    runSnapshotRemover("move-to-trash", false);

    assertThat("artifact folder was not removed", artifactFolder.list(), nullValue());
    assertThat("removed snapshots did not go into trash", trashArtifactFolder.isDirectory(), is(true));
    Collection<?> jars = FileUtils.listFiles(trashArtifactFolder, new String[]{"jar"}, false);
    assertThat("removed snapshots did not go into trash: " + jars, jars, hasSize(2));
  }

  @Test
  public void removeAllSnapshotsDirectly()
      throws Exception
  {
    Matcher<Collection<? extends Object>> empty = empty();

    // need local variables because generics won't compile otherwise
    Collection<? extends Object> files = FileUtils.listFiles(groupFolder, null, true);
    assertThat(files, not(empty));

    runSnapshotRemover("remove-directly", true);

    // need local variables because generics won't compile otherwise
    files = FileUtils.listFiles(groupFolder, null, true);
    assertThat(files, empty);

    // IT is kinda wrong since fixed NEXUS-5773
    // As the md rebuild is NOT invoked anymore for every file removal (that happens a LOT during snap removal)
    // that was reading/deleting md files, trash is not touched anymore, and since trash folder is created
    // lazily, it is not SURE that trash folder will exist (before pull nexus-oss/pull/52 it did, as MD files
    // were deleted then purged from here). After fix it will not even be created. In future, it MIGHT be again created.
    // but it does not change the semantic of this test. So, to make this test more reliable and/or
    // resilient to these future changes, we broaden the "trash folder must be empty" (as direct removal
    // happened) to assertion "trash folder might not exists (as trash was not ever used) OR if exists
    // then it has to be empty". Needed, as FileUtils.listFiles IAEx-es when invoked with non existent
    // folder.
    if (trashArtifactFolder.isDirectory()) {
      files = FileUtils.listFiles(trashArtifactFolder, null, true);
      assertThat("Files in trash folder", files, empty);
    }
    else {
      assertThat(true, is(true)); // see comment above IF, just here for easier spotting
    }
  }

  protected void runSnapshotRemover(String name,
                                    boolean deleteImmediately)
      throws Exception
  {
    ScheduledServicePropertyResource repositoryProp = new ScheduledServicePropertyResource();
    repositoryProp.setKey(SnapshotRemovalTaskDescriptor.REPO_OR_GROUP_FIELD_ID);
    repositoryProp.setValue(getTestRepositoryId());

    ScheduledServicePropertyResource keepSnapshotsProp = new ScheduledServicePropertyResource();
    keepSnapshotsProp.setKey(SnapshotRemovalTaskDescriptor.MIN_TO_KEEP_FIELD_ID);
    keepSnapshotsProp.setValue("-1");

    ScheduledServicePropertyResource ageProp = new ScheduledServicePropertyResource();
    ageProp.setKey(SnapshotRemovalTaskDescriptor.KEEP_DAYS_FIELD_ID);
    ageProp.setValue("-1");

    ScheduledServicePropertyResource removeReleasedProp = new ScheduledServicePropertyResource();
    removeReleasedProp.setKey(SnapshotRemovalTaskDescriptor.REMOVE_WHEN_RELEASED_FIELD_ID);
    removeReleasedProp.setValue(Boolean.toString(true));

    ScheduledServicePropertyResource deleteImmediatelyProp = new ScheduledServicePropertyResource();
    deleteImmediatelyProp.setKey(SnapshotRemovalTaskDescriptor.DELETE_IMMEDIATELY);
    deleteImmediatelyProp.setValue(Boolean.toString(deleteImmediately));

    TaskScheduleUtil.runTask(name, SnapshotRemovalTaskDescriptor.ID, repositoryProp,
        keepSnapshotsProp, ageProp,
        removeReleasedProp, deleteImmediatelyProp);
    TaskScheduleUtil.waitForAllTasksToStop();
  }
}
