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
package org.sonatype.nexus.tasklog;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static java.nio.file.Files.createTempDirectory;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class TaskLogCleanupTest
    extends TestSupport
{
  private static final Integer DAYS_AGO = 1;

  private static File tempTaskFolder;

  private TaskLogCleanup taskLogCleanup;

  private File todayFile;

  private File yesterdayFile;

  private File twoDaysOldFile;

  @BeforeClass
  public static void init() throws IOException {
    tempTaskFolder = createTempDirectory("tmp-task-folder").toFile();
  }

  @AfterClass
  public static void end() {
    tempTaskFolder.deleteOnExit();
  }

  @Before
  public void setup() throws IOException {
    taskLogCleanup = spy(new TaskLogCleanup(DAYS_AGO));

    todayFile = createFile("today", 0);
    yesterdayFile = createFile("yesterday", 1);
    twoDaysOldFile = createFile("twoDaysOld", 2);
  }

  @After
  public void tearDown() throws IOException {
    deleteQuietly(todayFile);
    deleteQuietly(yesterdayFile);
    deleteQuietly(twoDaysOldFile);
  }

  @Test
  public void cleanup_NoTaskLogHome() throws Exception {
    when(taskLogCleanup.getTaskLogHome()).thenReturn(null);

    taskLogCleanup.cleanup();

    // nothing is deleted
    assertTrue(todayFile.exists());
    assertTrue(yesterdayFile.exists());
    assertTrue(twoDaysOldFile.exists());
  }

  @Test
  public void cleanup() throws Exception {
    when(taskLogCleanup.getTaskLogHome()).thenReturn(tempTaskFolder.getAbsolutePath());

    taskLogCleanup.cleanup();

    // only two day old file is deleted
    assertTrue(todayFile.exists());
    assertTrue(yesterdayFile.exists());
    assertFalse(twoDaysOldFile.exists());
  }

  private File createFile(final String name, final int ageInDays) throws IOException {
    File file = new File(tempTaskFolder, name);
    file.createNewFile();
    file.setLastModified(ZonedDateTime.now().minusDays(ageInDays).toInstant().toEpochMilli());
    return file;
  }
}
