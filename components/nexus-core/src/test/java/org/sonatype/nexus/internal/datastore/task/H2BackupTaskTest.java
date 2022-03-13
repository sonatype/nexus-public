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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

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

  @Before
  public void setup() {
    when(dataStoreManager.get(DEFAULT_DATASTORE_NAME)).thenReturn(Optional.of(dataStore));
  }

  @Test
  public void testExecute_relativePath() throws Exception {
    String folder = "foo/bar";
    H2BackupTask task = createTask(folder);

    when(applicationDirectories.getWorkDirectory(folder)).thenReturn(new File(temporaryFolder.getRoot(), folder));

    task.execute();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(dataStore).backup(captor.capture());

    String backupPath = captor.getValue();

    assertThat(backupPath, startsWith(new File(temporaryFolder.getRoot(), folder).getPath()));
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

    thrown.expect(RuntimeException.class);
    thrown.expectMessage("Unable to locate datastore with name nexus");

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

  private Date date() throws ParseException {
    // We need to lose the milliseconds for comparison as they aren't used for the filename
    return format.parse(format.format(new Date()));
  }
}
