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
package org.sonatype.nexus.repository.content.upgrades;

import java.sql.Connection;
import java.util.HashMap;

import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.repository.browse.node.RebuildBrowseNodesTaskDescriptor;
import org.sonatype.nexus.repository.config.ConfigurationDAO;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.repository.content.store.ContentRepositoryDAO;
import org.sonatype.nexus.repository.content.store.ContentRepositoryData;
import org.sonatype.nexus.repository.content.store.ExampleContentTestSupport;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

public class BrowseNodeMigrationStep_1_39Test
    extends ExampleContentTestSupport
{
  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private UpgradeTaskScheduler upgradeTaskScheduler;

  @Mock
  private TaskConfiguration configuration;

  private DataStore<?> store;

  private BrowseNodeMigrationStep_1_39 underTest;

  public BrowseNodeMigrationStep_1_39Test() {
    super(ConanContentRepositoryDAO.class);
  }

  @Before
  public void setup() {
    when(taskScheduler.createTaskConfigurationInstance(RebuildBrowseNodesTaskDescriptor.TYPE_ID))
        .thenReturn(configuration);
    underTest = new BrowseNodeMigrationStep_1_39(taskScheduler, upgradeTaskScheduler);
    store = sessionRule.getDataStore(DEFAULT_DATASTORE_NAME).get();
  }

  @Test
  public void testMigrationEmptyRepository() throws Exception {

    try (Connection conn = store.openConnection()) {
      underTest.migrate(conn);
    }
    verify(upgradeTaskScheduler, never()).schedule(configuration);
  }

  @Test
  public void testMigration() throws Exception {
    ContentRepositoryData contentRepositoryData = randomContentRepository();
    createRepository(contentRepositoryData);

    try (Connection conn = store.openConnection()) {
      underTest.migrate(conn);
    }
    verify(upgradeTaskScheduler, times(1)).schedule(configuration);

    cleanContentRepository(contentRepositoryData);
  }

  private void cleanContentRepository(final ContentRepositoryData contentRepositoryData) {
    try (DataSession<?> session = store.openSession()) {
      ContentRepositoryDAO dao = session.access(ConanContentRepositoryDAO.class);
      dao.deleteContentRepository(contentRepositoryData);
      session.getTransaction().commit();
    }
  }

  private ContentRepositoryData createRepository(
      final ContentRepositoryData contentRepository)
  {
    ConfigurationData entity = new ConfigurationData();
    entity.setRepositoryName("my-conan" + randomUUID());
    entity.setRecipeName("conan-hosted");
    entity.setOnline(true);
    entity.setAttributes(new HashMap<>());

    try (DataSession<?> session = store.openSession()) {
      ConfigurationDAO dao = session.access(ConfigurationDAO.class);
      dao.create(entity);
      session.getTransaction().commit();
    }

    contentRepository.setConfigRepositoryId(entity.getId());

    try (DataSession<?> session = store.openSession()) {
      ContentRepositoryDAO dao = session.access(ConanContentRepositoryDAO.class);
      dao.createContentRepository(contentRepository);
      session.getTransaction().commit();
    }

    return contentRepository;
  }

  private interface ConanContentRepositoryDAO
      extends ContentRepositoryDAO
  {
  }
}
