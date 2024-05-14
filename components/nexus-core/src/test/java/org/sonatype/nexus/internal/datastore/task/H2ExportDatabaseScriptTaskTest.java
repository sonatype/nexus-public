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

import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.app.FreezeService;
import org.sonatype.nexus.content.testsuite.groups.H2TestGroup;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.datastore.api.DataStoreManager;
import org.sonatype.nexus.datastore.api.DataStoreNotFoundException;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@Category(H2TestGroup.class)
public class H2ExportDatabaseScriptTaskTest
    extends TestSupport
{
  @Mock
  private DataStoreManager dataStoreManager;

  @Mock
  private DataStore<?> dataStore;

  @Mock
  private ApplicationDirectories applicationDirectories;

  @Mock
  private FreezeService freezeService;

  @InjectMocks
  private H2ExportDatabaseScriptTask task;

  private static final String DEFAULT_LOCATION = "db";

  @Before
  public void setup() {
    when(dataStoreManager.get(DEFAULT_DATASTORE_NAME)).thenReturn(Optional.of(dataStore));
  }

  @Test
  public void testDefaultLocation() {
    H2ExportDatabaseScriptTask h2ExportDatabaseScriptTaskSpy = spy(createTaskWithDefaultLocation());

    when(h2ExportDatabaseScriptTaskSpy.getConfigurationLocationPath()).thenReturn(DEFAULT_LOCATION);

    String scriptPath = h2ExportDatabaseScriptTaskSpy.getLocationPath();

    assertEquals(DEFAULT_LOCATION, scriptPath);
  }

  @Test
  public void testUserLocation() {
    String userLocationPath = "foo/bar";
    H2ExportDatabaseScriptTask h2ExportDatabaseScriptTaskSpy = spy(createTaskWithUserLocation(userLocationPath));

    when(h2ExportDatabaseScriptTaskSpy.getConfigurationLocationPath()).thenReturn(userLocationPath);

    String scriptPath = h2ExportDatabaseScriptTaskSpy.getLocationPath();

    assertEquals(userLocationPath, scriptPath);
  }
  
  @Test
  public void testMissingDataStore() {
    when(dataStoreManager.get(DEFAULT_DATASTORE_NAME)).thenReturn(Optional.empty());

    task = createTaskWithDefaultLocation();

    assertThrows(DataStoreNotFoundException.class, task::execute);
  }

  private H2ExportDatabaseScriptTask createTaskWithDefaultLocation() {
    task = new H2ExportDatabaseScriptTask(dataStoreManager,
        applicationDirectories,
        freezeService);
    TaskConfiguration configuration = new TaskConfiguration();
    configuration.setString(H2ExportDatabaseScriptTaskDescriptor.LOCATION, DEFAULT_LOCATION);
    configuration.setTypeId(H2ExportDatabaseScriptTaskDescriptor.TYPE_ID);
    configuration.setId("testId1");
    task.configure(configuration);
    return task;
  }

  private H2ExportDatabaseScriptTask createTaskWithUserLocation(final String location) {
    task = new H2ExportDatabaseScriptTask(dataStoreManager,
        applicationDirectories,
        freezeService);
    TaskConfiguration configuration = new TaskConfiguration();
    configuration.setString(H2ExportDatabaseScriptTaskDescriptor.LOCATION, location);
    configuration.setTypeId(H2ExportDatabaseScriptTaskDescriptor.TYPE_ID);
    configuration.setId("testId2");
    task.configure(configuration);
    return task;
  }
}
