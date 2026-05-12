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
package org.sonatype.nexus.datastore.h2.task;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Optional;

import org.sonatype.nexus.bootstrap.entrypoint.configuration.ApplicationDirectories;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.datastore.api.DataStoreManager;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class H2BackupTaskTest
{
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock
  private DataStoreManager dataStoreManager;

  @Mock
  private DataStore<?> dataStore;

  @Mock
  private ApplicationDirectories applicationDirectories;

  @Before
  public void setup() throws Exception {
    when(dataStoreManager.get(DEFAULT_DATASTORE_NAME)).thenReturn(Optional.of(dataStore));

    // Mock the backup operation to create the file
    doAnswer(invocation -> {
      String path = invocation.getArgument(0);
      File file = new File(path);
      file.getParentFile().mkdirs();
      // Write some data to simulate a real backup
      Files.write(file.toPath(), "fake backup data".getBytes());
      return null;
    }).when(dataStore).backup(anyString());
  }

  @Test
  public void testExecute_relativePath() throws Exception {
    String folder = "foo/bar";
    H2BackupTask task = createTask(folder);

    File backupDir = new File(temporaryFolder.getRoot(), folder);
    when(applicationDirectories.getWorkDirectory(folder)).thenReturn(backupDir);

    task.execute();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(dataStore).backup(captor.capture());

    String backupPath = captor.getValue();

    assertThat(backupPath, startsWith(backupDir.getPath()));
  }

  @Test
  public void testExecute_missingLocation() throws Exception {
    H2BackupTask task = createTask(null);

    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Backup location not configured");
    task.execute();
  }

  @Test
  public void testExecute_missingDataStore() throws Exception {
    when(dataStoreManager.get(DEFAULT_DATASTORE_NAME)).thenReturn(Optional.empty());
    H2BackupTask task = createTask("/foo");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Unable to locate datastore with name nexus");

    task.execute();
  }

  @Test
  public void testExecute_leadingWhitespace() throws Exception {
    // Leading whitespace should be trimmed
    String folder = " /foo/bar";
    H2BackupTask task = createTask(folder);

    // Should trim to "/foo/bar" and treat as absolute path
    File backupDir = new File(temporaryFolder.getRoot(), "foo/bar");
    when(applicationDirectories.getWorkDirectory(folder.trim())).thenReturn(backupDir);

    task.execute();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(dataStore).backup(captor.capture());

    String backupPath = captor.getValue();
    assertThat(backupPath, startsWith(backupDir.getPath()));
  }

  @Test
  public void testExecute_trailingWhitespace() throws Exception {
    // Trailing whitespace should be trimmed
    String folder = "/foo/bar ";
    H2BackupTask task = createTask(folder);

    File backupDir = new File(temporaryFolder.getRoot(), "foo/bar");
    when(applicationDirectories.getWorkDirectory(folder.trim())).thenReturn(backupDir);

    task.execute();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(dataStore).backup(captor.capture());

    String backupPath = captor.getValue();
    assertThat(backupPath, startsWith(backupDir.getPath()));
  }

  @Test
  public void testExecute_onlyWhitespace() throws Exception {
    // Location with only whitespace should fail with clear error
    H2BackupTask task = createTask("   ");

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Backup location cannot be empty or whitespace: '   '");

    task.execute();
  }

  @Test
  public void testExecute_mixedWhitespace() throws Exception {
    // Mixed leading and trailing whitespace should be trimmed
    String folder = "  /foo/bar  ";
    H2BackupTask task = createTask(folder);

    File backupDir = new File(temporaryFolder.getRoot(), "foo/bar");
    when(applicationDirectories.getWorkDirectory(folder.trim())).thenReturn(backupDir);

    task.execute();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(dataStore).backup(captor.capture());

    String backupPath = captor.getValue();
    assertThat(backupPath, startsWith(backupDir.getPath()));
  }

  @Test
  public void testExecute_backupFileAlreadyExists() throws Exception {
    // Verify behavior when backup file already exists
    String folder = "foo/bar";
    File backupDir = new File(temporaryFolder.getRoot(), folder);
    backupDir.mkdirs();

    when(applicationDirectories.getWorkDirectory(folder)).thenReturn(backupDir);

    // Pre-create files covering the current second and next few seconds to guarantee collision
    // This avoids timing-dependent test failures
    LocalDateTime now = LocalDateTime.now();
    for (int i = 0; i <= 5; i++) {
      LocalDateTime time = now.plusSeconds(i);
      String timestamp = String.format("%1$tY-%1$tm-%1$td-%1$tH-%1$tM-%1$tS", time);
      File existingFile = new File(backupDir, DEFAULT_DATASTORE_NAME + "-" + timestamp + ".zip");
      Files.write(existingFile.toPath(), "existing backup".getBytes());
    }

    H2BackupTask task = createTask(folder);

    thrown.expect(IOException.class);
    thrown.expectMessage("File already exists");

    task.execute(); // Should collide with one of the pre-created files
  }

  @Test
  public void testExecute_backupCreatesEmptyFile() throws Exception {
    // Verify behavior when backup creates empty file
    String folder = "foo/bar";
    H2BackupTask task = createTask(folder);

    File backupDir = new File(temporaryFolder.getRoot(), folder);
    when(applicationDirectories.getWorkDirectory(folder)).thenReturn(backupDir);

    // Mock backup to create empty file
    doAnswer(invocation -> {
      String path = invocation.getArgument(0);
      File file = new File(path);
      file.getParentFile().mkdirs();
      file.createNewFile(); // Creates empty file
      return null;
    }).when(dataStore).backup(anyString());

    thrown.expect(IOException.class);
    thrown.expectMessage("Backup file is empty (0 bytes)");

    task.execute();
  }

  @Test
  public void testExecute_backupFailsToCreateFile() throws Exception {
    // Verify behavior when backup doesn't create file at all
    String folder = "foo/bar";
    H2BackupTask task = createTask(folder);

    File backupDir = new File(temporaryFolder.getRoot(), folder);
    when(applicationDirectories.getWorkDirectory(folder)).thenReturn(backupDir);

    // Mock backup to not create any file
    doAnswer(invocation -> {
      // Do nothing - simulate backup failure
      return null;
    }).when(dataStore).backup(anyString());

    thrown.expect(IOException.class);
    thrown.expectMessage("Backup file was not created");

    task.execute();
  }

  @Test
  public void testExecute_backupThrowsException() throws Exception {
    // Verify behavior when backup operation throws exception
    String folder = "foo/bar";
    H2BackupTask task = createTask(folder);

    File backupDir = new File(temporaryFolder.getRoot(), folder);
    when(applicationDirectories.getWorkDirectory(folder)).thenReturn(backupDir);

    // Mock backup to throw exception
    doAnswer(invocation -> {
      throw new IOException("Database backup failed");
    }).when(dataStore).backup(anyString());

    thrown.expect(Exception.class);
    thrown.expectMessage("Database backup failed");

    task.execute();
  }

  @Test
  public void testExecute_directoryCreationFails() throws Exception {
    // Verify behavior when backup directory cannot be created
    String folder = "foo/bar";
    H2BackupTask task = createTask(folder);

    // Mock a directory that doesn't exist and cannot be created
    File backupDir = new File(temporaryFolder.getRoot(), folder)
    {
      @Override
      public boolean mkdirs() {
        return false; // Simulate creation failure
      }

      @Override
      public boolean exists() {
        return false; // Directory doesn't exist
      }
    };

    when(applicationDirectories.getWorkDirectory(folder)).thenReturn(backupDir);

    thrown.expect(IOException.class);
    thrown.expectMessage("Failed to create backup directory");

    task.execute();
  }

  @Test
  public void testExecute_directoryExistsAtBackupFilePath() throws Exception {
    // Verify behavior when a directory exists at the backup file path
    String folder = "foo/bar";
    File backupDir = new File(temporaryFolder.getRoot(), folder);
    backupDir.mkdirs();

    when(applicationDirectories.getWorkDirectory(folder)).thenReturn(backupDir);

    // Pre-create directories covering the current second and next few seconds to guarantee collision
    // This avoids timing-dependent test failures
    LocalDateTime now = LocalDateTime.now();
    for (int i = 0; i <= 5; i++) {
      LocalDateTime time = now.plusSeconds(i);
      String timestamp = String.format("%1$tY-%1$tm-%1$td-%1$tH-%1$tM-%1$tS", time);
      File directoryAtFilePath = new File(backupDir, DEFAULT_DATASTORE_NAME + "-" + timestamp + ".zip");
      directoryAtFilePath.mkdirs(); // Create as directory, not file
    }

    H2BackupTask task = createTask(folder);

    thrown.expect(IOException.class);
    thrown.expectMessage("File already exists");

    task.execute(); // Should collide with one of the pre-created directories
  }

  @Test
  public void testExecute_backupPathIsFile() throws Exception {
    // Verify behavior when backup path exists but is a file instead of directory
    String folder = "foo/bar";
    H2BackupTask task = createTask(folder);

    // Create a file at the backup directory path
    File backupPathAsFile = new File(temporaryFolder.getRoot(), folder);
    backupPathAsFile.getParentFile().mkdirs();
    backupPathAsFile.createNewFile();

    when(applicationDirectories.getWorkDirectory(folder)).thenReturn(backupPathAsFile);

    thrown.expect(IOException.class);
    thrown.expectMessage("Backup path exists but is not a directory");

    task.execute();
  }

  private H2BackupTask createTask(final String location) {
    H2BackupTask task = new H2BackupTask(dataStoreManager, applicationDirectories);
    TaskConfiguration configuration = new TaskConfiguration();
    configuration.setString(H2BackupTaskDescriptor.LOCATION, location);
    configuration.setTypeId(H2BackupTaskDescriptor.TYPE_ID);
    configuration.setId("my.id");
    task.configure(configuration);
    return task;
  }
}
