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
package org.sonatype.nexus.testsuite.repo.nexus970;

import java.io.File;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.Method;
import org.restlet.data.Status;

/**
 * When deleting a repository folders related to it is should be removed from disk
 */
public class Nexus970DeleteRepositoryIT
    extends AbstractNexusIntegrationTest
{

  /*
   * a) if repo is created using Nexus default storage (within ${sonatype-work}/nexus/storage), then Nexus could trash
   * the storage directory with all the files (but again, there might be involved a lot of FS operations if repo is
   * big)
   */
  @Test
  public void deleteOnDefaultStorage()
      throws Exception
  {
    File storageDir = new File(nexusWorkDir, "storage/nexus970-default");
    File artifactFile = new File(storageDir, "nexus970/artifact/1.0.0/artifact-1.0.0.jar");

    // sanity check
    Assert.assertTrue(storageDir.isDirectory());
    Assert.assertTrue(artifactFile.isFile());

    String uri = "service/local/repositories/nexus970-default";
    Status status = RequestFacade.sendMessage(uri, Method.DELETE).getStatus();
    Assert.assertTrue("Unable to delete nexus970-default repository", status.isSuccess());

    // give a chance to wait for task to start
    Thread.sleep(1500);
    getEventInspectorsUtil().waitForCalmPeriod();
    TaskScheduleUtil.waitForAllTasksToStop();


    Assert.assertFalse("Artifacts shouldn't exists on deleted repo", artifactFile.exists());
    Assert.assertFalse("Storage dir should be deleted", storageDir.exists());

    File trashStorageDir = new File(nexusWorkDir, "trash/nexus970-default");
    File trashArtifactFile = new File(trashStorageDir, "nexus970/artifact/1.0.0/artifact-1.0.0.jar");

    Assert.assertTrue("Storage should be moved to trash", trashStorageDir.isDirectory());
    Assert.assertTrue("Artifacts should be moved to trash", trashArtifactFile.isFile());
  }

  /*
   * b) if repo is created outside Nexus default storage, then repo files should remain intact.
   */
  @Test
  public void deleteOnOverwroteStorage()
      throws Exception
  {
    File storageDir = getTestFile("overwrote-repo");
    File artifactFile = new File(storageDir, "nexus970/artifact/1.0.0/artifact-1.0.0.jar");

    // sanity check
    Assert.assertTrue(storageDir.isDirectory());
    Assert.assertTrue(artifactFile.isFile());

    String uri = "service/local/repositories/nexus970-overwrote";
    Status status = RequestFacade.sendMessage(uri, Method.DELETE).getStatus();
    Assert.assertTrue("Unable to delete nexus970-default repository", status.isSuccess());

    // give a chance to wait for task to start
    Thread.sleep(500);
    TaskScheduleUtil.waitForAllTasksToStop();

    Assert.assertTrue("Artifacts should exists on deleted repo", artifactFile.isFile());
    Assert.assertTrue("Storage dir shouldn't be deleted", storageDir.isDirectory());

    File trashStorageDir = new File(nexusWorkDir, "trash/nexus970-overwrote");
    File trashArtifactFile = new File(trashStorageDir, "nexus970/artifact/1.0.0/artifact-1.0.0.jar");

    Assert.assertFalse("Storage shouldn't be moved to trash", trashStorageDir.exists());
    Assert.assertFalse("Artifacts shouldn't be moved to trash", trashArtifactFile.exists());
  }

  @BeforeClass
  public static void clean()
      throws Exception
  {
    cleanWorkDir();
  }

}
