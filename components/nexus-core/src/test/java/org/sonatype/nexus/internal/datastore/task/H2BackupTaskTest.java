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
package org.sonatype.nexus.internal.datastore.task;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.datastore.api.DataStoreManager;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class H2BackupTaskTest
    extends TestSupport
{
  private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HHmmss");

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

  private String testName = "config";

  @Before
  public void setup() {
    when(dataStoreManager.get(testName)).thenReturn(Optional.of(dataStore));
  }

  @Test
  public void testExecute_dateTime() throws Exception {
    String folder = "/foo/bar/{datetime}.zip";
    Date before = date();
    H2BackupTask task = createTask(testName, folder);

    task.execute();
    Date after = date();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(dataStore).backup(captor.capture());

    String backupPath = captor.getValue();

    Date serialized = new SimpleDateFormat("yyyy-MM-dd_HHmmss").parse(backupPath.substring(9, backupPath.length() - 4));

    assertThat(before.compareTo(serialized), lessThanOrEqualTo(0));
    assertThat(serialized.compareTo(after), lessThanOrEqualTo(0));
  }

  @Test
  public void testExecute_relativePath() throws Exception {
    String folder = "foo/bar/backup.zip";
    H2BackupTask task = createTask(testName, folder);

    when(applicationDirectories.getWorkDirectory()).thenReturn(temporaryFolder.getRoot());

    task.execute();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(dataStore).backup(captor.capture());

    String backupPath = captor.getValue();

    assertThat(backupPath, equalTo(new File(temporaryFolder.getRoot(), "foo/bar/backup.zip").getPath()));
  }

  @Test
  public void testExecute_missingLocation() throws Exception {
    H2BackupTask task = createTask(testName, null);

    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Backup location not configured");
    task.execute();
  }

  @Test
  public void testExecute_missingDataStoreName() throws Exception {
    H2BackupTask task = createTask(null, "/foo/bar.zip");

    thrown.expect(NullPointerException.class);
    thrown.expectMessage("DataStore name not configured");

    task.execute();
  }

  @Test
  public void testExecute_missingDataStore() throws Exception {
    when(dataStoreManager.get("config2")).thenReturn(Optional.empty());
    H2BackupTask task = createTask("config2", "/foo/bar.zip");

    thrown.expect(RuntimeException.class);
    thrown.expectMessage("Unable to locate datastore with name config2");

    task.execute();
  }

  private H2BackupTask createTask(final String dataStoreName, final String location) {
    H2BackupTask task = new H2BackupTask(dataStoreManager, applicationDirectories);
    TaskConfiguration configuration = new TaskConfiguration();
    configuration.setString(H2BackupTaskDescriptor.LOCATION, location);
    configuration.setString(H2BackupTaskDescriptor.DATASTORE, dataStoreName);
    configuration.setTypeId(H2BackupTaskDescriptor.TYPE_ID);
    configuration.setId("my.id");
    task.configure(configuration);
    return task;
  }

  private Date date() throws ParseException {
    // We need to lose the milliseconds for comparison as they aren't used for the filename
    return format.parse(format.format(new Date()));
  }
}
