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
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.store.AssetBlobStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.schedule.Schedule;

import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.DATA_STORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE;
import static org.sonatype.nexus.repository.content.store.internal.migration.AssetBlobRefMigrationTaskDescriptor.CONTENT_STORE_FIELD_ID;
import static org.sonatype.nexus.repository.content.store.internal.migration.AssetBlobRefMigrationTaskDescriptor.FORMAT_FIELD_ID;
import static org.sonatype.nexus.repository.content.store.internal.migration.AssetBlobRefMigrationTaskDescriptor.TYPE_ID;

/**
 * Manage scheduling {@link AssetBlobRefMigrationTask} for each format separately (if necessary).
 */
@Named
@Singleton
@ManagedLifecycle(phase = TASKS)
public class AssetBlobRefMigrationTaskManager
    extends LifecycleSupport
    implements EventAware, EventAware.Asynchronous
{
  private final RepositoryManager repositoryManager;

  private final Map<String, FormatStoreManager> formatStoreManagers;

  private final TaskScheduler taskScheduler;

  @Inject
  public AssetBlobRefMigrationTaskManager(
      final RepositoryManager repositoryManager,
      final Map<String, FormatStoreManager> formatStoreManagers,
      final TaskScheduler taskScheduler) {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.formatStoreManagers = checkNotNull(formatStoreManagers);
    this.taskScheduler = checkNotNull(taskScheduler);
  }

  @Override
  protected void doStart() throws Exception {
    browseActiveFormatStores().forEach((format, contentStore) -> {
      FormatStoreManager formatStoreManager = formatStoreManagers.get(format);
      if (formatStoreManager != null) {
        AssetBlobStore<?> assetBlobStore = formatStoreManager.assetBlobStore(contentStore);
        boolean notMigratedAssetBlobRefsExists = assetBlobStore.notMigratedAssetBlobRefsExists();
        if (notMigratedAssetBlobRefsExists) {
          log.info("Found asset blobs with legacy blobRef for {} in {}. Scheduling a migration task for it.",
              format, contentStore);
          scheduleMigrationTask(format, contentStore);
        }
      }
    });
  }

  private Map<String, String> browseActiveFormatStores() {
    Map<String, String> activeFormatStores = new HashMap<>();

    repositoryManager.browse().forEach(repository -> {
      Configuration configuration = repository.getConfiguration();
      NestedAttributesMap storageAttributes = configuration.attributes(STORAGE);

      String format = repository.getFormat().getValue();
      String contentStore = (String) storageAttributes.get(DATA_STORE_NAME);

      activeFormatStores.put(format, contentStore);
    });
    return activeFormatStores;
  }

  private void scheduleMigrationTask(final String format, final String contentStore) {
    Map<String, String> settings = ImmutableMap.of(
        FORMAT_FIELD_ID, format,
        CONTENT_STORE_FIELD_ID, contentStore);

    if (taskScheduler.getTaskByTypeId(TYPE_ID, settings) == null) {
      TaskConfiguration taskConfiguration = taskScheduler.createTaskConfigurationInstance(TYPE_ID);
      taskConfiguration.setName("Migrate blobRef assets field for " + format + " format");
      taskConfiguration.setString(FORMAT_FIELD_ID, format);
      taskConfiguration.setString(CONTENT_STORE_FIELD_ID, contentStore);
      Schedule schedule = taskScheduler.getScheduleFactory().now();
      log.info("Scheduling blobRef migration task for {} format on {}", format, contentStore);
      taskScheduler.scheduleTask(taskConfiguration, schedule);
    }
  }
}
