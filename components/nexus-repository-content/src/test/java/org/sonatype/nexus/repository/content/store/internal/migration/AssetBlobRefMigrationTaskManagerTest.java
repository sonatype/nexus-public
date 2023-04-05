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
package org.sonatype.nexus.repository.content.store.internal.migration;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.store.AssetBlobStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.schedule.ScheduleFactoryImpl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.DATA_STORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE;
import static org.sonatype.nexus.repository.content.store.internal.AssetBlobCleanupTaskDescriptor.CONTENT_STORE_FIELD_ID;
import static org.sonatype.nexus.repository.content.store.internal.AssetBlobCleanupTaskDescriptor.FORMAT_FIELD_ID;
import static org.sonatype.nexus.repository.content.store.internal.migration.AssetBlobRefMigrationTaskDescriptor.TYPE_ID;

/**
 * Test for {@link AssetBlobRefMigrationTaskManager}
 */
public class AssetBlobRefMigrationTaskManagerTest
    extends TestSupport
{
  private static final String STORE_NAME = "default-store";

  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private Format rawFormat;

  @Mock
  private Format mavenFormat;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private Repository rawRepository;

  @Mock
  private Repository mavenRepository;

  @Mock
  private Configuration rawConfig;

  @Mock
  private Configuration mavenConfig;

  @Mock
  private AssetBlobStore<?> assetBlobStore;

  private Map<String, FormatStoreManager> formatStoreManagers = new HashMap<>();

  @Before
  public void setup() {
    when(rawFormat.getValue()).thenReturn("raw");
    when(mavenFormat.getValue()).thenReturn("maven");

    when(rawRepository.getFormat()).thenReturn(rawFormat);
    when(mavenRepository.getFormat()).thenReturn(mavenFormat);

    when(rawConfig.attributes(STORAGE))
        .thenReturn(new NestedAttributesMap(STORAGE, ImmutableMap.of(DATA_STORE_NAME, STORE_NAME)));
    when(mavenConfig.attributes(STORAGE))
        .thenReturn(new NestedAttributesMap(STORAGE, ImmutableMap.of(DATA_STORE_NAME, STORE_NAME)));

    when(rawRepository.getConfiguration()).thenReturn(rawConfig);
    when(mavenRepository.getConfiguration()).thenReturn(mavenConfig);

    when(repositoryManager.browse()).thenReturn(ImmutableList.of(rawRepository, mavenRepository));

    when(taskScheduler.createTaskConfigurationInstance(TYPE_ID)).thenAnswer(call -> new TaskConfiguration());
    when(taskScheduler.getScheduleFactory()).thenReturn(new ScheduleFactoryImpl());

    prepareFormatStoreManagers();
  }

  @Test
  public void testTasksScheduled() throws Exception {
    when(assetBlobStore.notMigratedAssetBlobRefsExists()).thenReturn(true);

    AssetBlobRefMigrationTaskManager underTest = new AssetBlobRefMigrationTaskManager(repositoryManager,
        formatStoreManagers, taskScheduler);
    InOrder inOrder = inOrder(taskScheduler);

    inOrder.verify(taskScheduler, never()).scheduleTask(any(), any());

    underTest.start();

    ArgumentCaptor<TaskConfiguration> configCaptor = forClass(TaskConfiguration.class);
    inOrder.verify(taskScheduler).scheduleTask(configCaptor.capture(), any());
    inOrder.verify(taskScheduler).scheduleTask(configCaptor.capture(), any());
    assertThat(configCaptor.getAllValues().stream().map(TaskConfiguration::asMap).collect(toList()),
        containsInAnyOrder(
            allOf(hasEntry(FORMAT_FIELD_ID, "raw"), hasEntry(CONTENT_STORE_FIELD_ID, STORE_NAME)),
            allOf(hasEntry(FORMAT_FIELD_ID, "maven"), hasEntry(CONTENT_STORE_FIELD_ID, STORE_NAME))));

    underTest.stop();
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void testTasksNotScheduled() throws Exception {
    when(assetBlobStore.notMigratedAssetBlobRefsExists()).thenReturn(false);

    AssetBlobRefMigrationTaskManager underTest = new AssetBlobRefMigrationTaskManager(repositoryManager,
        formatStoreManagers, taskScheduler);

    InOrder inOrder = inOrder(taskScheduler);

    underTest.start();
    inOrder.verify(taskScheduler, never()).scheduleTask(any(), any());
    underTest.stop();
  }

  private void prepareFormatStoreManagers() {
    FormatStoreManager rawStoreManager = mock(FormatStoreManager.class);
    when(rawStoreManager.assetBlobStore(STORE_NAME)).thenReturn(assetBlobStore);
    formatStoreManagers.put("raw", rawStoreManager);

    FormatStoreManager mavenStoreManager = mock(FormatStoreManager.class);
    when(mavenStoreManager.assetBlobStore(STORE_NAME)).thenReturn(assetBlobStore);
    formatStoreManagers.put("maven", mavenStoreManager);
  }
}
