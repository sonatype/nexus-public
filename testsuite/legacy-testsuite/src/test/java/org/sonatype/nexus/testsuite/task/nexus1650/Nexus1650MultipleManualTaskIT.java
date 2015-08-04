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
package org.sonatype.nexus.testsuite.task.nexus1650;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.maven.tasks.descriptors.SnapshotRemovalTaskDescriptor;
import org.sonatype.nexus.rest.model.ScheduledServiceBaseResource;
import org.sonatype.nexus.rest.model.ScheduledServiceListResource;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.restlet.data.Status;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;

public class Nexus1650MultipleManualTaskIT
    extends AbstractNexusIntegrationTest
{

  @SuppressWarnings("unchecked")
  public static Collection<File> listFiles(File directory, String[] extensions, boolean recursive) {
    return FileUtils.listFiles(directory, extensions, recursive);
  }

  protected File artifactFolder;

  protected File repositoryPath;

  public Nexus1650MultipleManualTaskIT() {
    super("nexus-test-harness-snapshot-repo");
  }

  @Before
  public void deploySnapshotArtifacts()
      throws Exception
  {
    initFolders();

    File oldSnapshot = getTestFile("repo");

    // Copying to keep an old timestamp
    FileUtils.copyDirectory(oldSnapshot, repositoryPath);

    RepositoryMessageUtil.updateIndexes("nexus-test-harness-snapshot-repo");
  }

  public void initFolders()
      throws Exception
  {
    repositoryPath = new File(nexusWorkDir, "storage/nexus-test-harness-snapshot-repo");
    artifactFolder = new File(repositoryPath, "nexus634/artifact/1.0-SNAPSHOT");
  }

  protected void createSnapshotTask(String name)
      throws Exception
  {
    ScheduledServicePropertyResource repositoryProp = new ScheduledServicePropertyResource();
    repositoryProp.setKey("repositoryId");
    repositoryProp.setValue("nexus-test-harness-snapshot-repo");

    ScheduledServicePropertyResource keepSnapshotsProp = new ScheduledServicePropertyResource();
    keepSnapshotsProp.setKey("minSnapshotsToKeep");
    keepSnapshotsProp.setValue(String.valueOf(0));

    ScheduledServicePropertyResource ageProp = new ScheduledServicePropertyResource();
    ageProp.setKey("removeOlderThanDays");
    ageProp.setValue(String.valueOf(0));

    ScheduledServicePropertyResource removeReleasedProp = new ScheduledServicePropertyResource();
    removeReleasedProp.setKey("removeIfReleaseExists");
    removeReleasedProp.setValue(String.valueOf(true));

    ScheduledServiceBaseResource scheduledTask = new ScheduledServiceBaseResource();
    scheduledTask.setEnabled(true);
    scheduledTask.setId(null);
    scheduledTask.setName(name);
    scheduledTask.setTypeId(SnapshotRemovalTaskDescriptor.ID);
    scheduledTask.setSchedule("manual");
    scheduledTask.addProperty(repositoryProp);
    scheduledTask.addProperty(keepSnapshotsProp);
    scheduledTask.addProperty(ageProp);
    scheduledTask.addProperty(removeReleasedProp);

    Status status = TaskScheduleUtil.create(scheduledTask);

    Assert.assertTrue(status.isSuccess());
  }

  @Test
  public void testMultipleManualInstances()
      throws Exception
  {
    TaskScheduleUtil.waitForAllTasksToStop();
    getEventInspectorsUtil().waitForCalmPeriod();

    createSnapshotTask("Nexus1650Task1");
    createSnapshotTask("Nexus1650Task2");
    createSnapshotTask("Nexus1650Task3");

    List<ScheduledServiceListResource> tasks = TaskScheduleUtil.getTasks();

    assertThat(tasks, hasSize(3));

    long startTimestamp = System.currentTimeMillis();

    for (ScheduledServiceListResource resource : tasks) {
      TaskScheduleUtil.run(resource.getId());
    }

    waitForOneTaskSleeping();

    TaskScheduleUtil.waitForAllTasksToStop();

    assertAllTasksWereRunning(startTimestamp);
  }

  private void assertAllTasksWereRunning(long startTimestamp)
      throws IOException
  {
    final List<ScheduledServiceListResource> tasks = TaskScheduleUtil.getTasks();

    for (ScheduledServiceListResource task : tasks) {
      assertThat("task did not run properly!", task.getLastRunTimeInMillis(), greaterThan(startTimestamp));
    }
  }

  private void waitForOneTaskSleeping()
      throws Exception
  {
    final long end = System.currentTimeMillis() + 10000;

    while (!isAtLeastOneSleeping()) {
      Thread.sleep(200);
      if (System.currentTimeMillis() > end) {
        assertThat("no task was seen sleeping in 10s", System.currentTimeMillis(), lessThan(end));
      }
    }
  }

  private boolean isAtLeastOneSleeping()
      throws Exception
  {
    List<ScheduledServiceListResource> tasks = TaskScheduleUtil.getTasks();

    for (ScheduledServiceListResource resource : tasks) {
      if (resource.getStatus().equals("SLEEPING")) {
        return true;
      }
    }

    return false;
  }

}
