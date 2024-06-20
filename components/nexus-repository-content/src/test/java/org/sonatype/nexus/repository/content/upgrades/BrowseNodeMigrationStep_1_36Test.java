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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.browse.node.RebuildBrowseNodesTaskDescriptor;
import org.sonatype.nexus.repository.config.ConfigurationDAO;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.repository.content.browse.store.BrowseNodeDAO;
import org.sonatype.nexus.repository.content.browse.store.example.TestBrowseNodeDAO;
import org.sonatype.nexus.repository.content.store.AssetBlobDAO;
import org.sonatype.nexus.repository.content.store.AssetDAO;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.content.store.ComponentDAO;
import org.sonatype.nexus.repository.content.store.ComponentData;
import org.sonatype.nexus.repository.content.store.ContentRepositoryDAO;
import org.sonatype.nexus.repository.content.store.ContentRepositoryData;
import org.sonatype.nexus.repository.content.store.ExampleContentTestSupport;
import org.sonatype.nexus.repository.content.store.InternalIds;
import org.sonatype.nexus.repository.content.store.example.TestAssetDAO;
import org.sonatype.nexus.repository.content.store.example.TestComponentDAO;
import org.sonatype.nexus.repository.content.store.example.TestContentRepositoryDAO;
import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskScheduler;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

public class BrowseNodeMigrationStep_1_36Test
  extends ExampleContentTestSupport
{
  private static final Logger log = LoggerFactory.getLogger(BrowseNodeMigrationStep_1_36Test.class);

  private final String INSERT = "INSERT INTO test_browse_node (repository_id, parent_id, display_name, request_path,"
      + " asset_id, component_id) "
      + " VALUES (?, ?, ?, ?, ?, ?);";

  @Mock
  private Format fakeFormat;

  @Mock
  private Format pypiFormat;

  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private UpgradeTaskScheduler upgradeTaskScheduler;

  @Mock
  private TaskConfiguration configuration;

  private BrowseNodeMigrationStep_1_36 upgradeStep;

  private DataStore<?> store;

  public BrowseNodeMigrationStep_1_36Test() {
    super(TestBrowseNodeDAO.class, PyPiContentRepositoryDAO.class, PyPiComponentDAO.class, PyPiAssetBlobDAO.class,
        PyPiAssetDAO.class, PyPiBrowseNodeDAO.class);
  }

  @Before
  public void setup() {
    when(fakeFormat.getValue()).thenReturn("test");
    when(pypiFormat.getValue()).thenReturn("pypi");
    when(taskScheduler.createTaskConfigurationInstance(RebuildBrowseNodesTaskDescriptor.TYPE_ID))
        .thenReturn(configuration);

    upgradeStep = new BrowseNodeMigrationStep_1_36(Arrays.asList(fakeFormat, pypiFormat), taskScheduler,
        upgradeTaskScheduler);
    store = sessionRule.getDataStore(DEFAULT_DATASTORE_NAME).get();
  }

  @Test(expected = Test.None.class)
  public void testUnknownFormat() throws Exception {
    when(fakeFormat.getValue()).thenReturn("foo");
    BrowseNodeMigrationStep_1_36 upgradeStep = new BrowseNodeMigrationStep_1_36(Collections.singletonList(fakeFormat),
        taskScheduler, upgradeTaskScheduler);

    try (Connection conn = store.openConnection()) {
      upgradeStep.migrate(conn);
    }
    // no assertions, not blowing up is the expectation
  }

  @Test
  public void testMigration() throws Exception {
    generateRandomPaths(10);
    generateRandomNamespaces(10);
    generateRandomNames(10);
    generateRandomVersions(10);

    ContentRepositoryData member = createRepository("member", "test-hosted", randomContentRepository());
    ContentRepositoryData group = createRepository("group", "test-group", randomContentRepository());

    // An asset in member that was added to the browse tree of the group
    insert(group.contentRepositoryId(), 0, "jquery", "/jquery/", createAsset(member), null);
    // A component in member that was added to the browse tree of the group
    insert(group.contentRepositoryId(), 0, "jquery-1.5", "/jquery-1.5/", null, createComponent(member));

    // A group repository with content (the fact this is a group is unimportant)
    ContentRepositoryData unaffectedGroup = createRepository("unaffected", "test-group", randomContentRepository());

    // An asset in the correct repository
    insert(unaffectedGroup.contentRepositoryId(), 0, "jquery", "/jquery/", createAsset(unaffectedGroup), null);
    // A component in the correct repository
    insert(unaffectedGroup.contentRepositoryId(), 0, "jquery-1.5", "/jquery-1.5/", null,
        createComponent(unaffectedGroup));

    // All pypi groups should be scheduled regardless of mixed content
    createRepository("my-pypi-group", "pypi-group", randomContentRepository());

    try (Connection conn = store.openConnection()) {
      upgradeStep.migrate(conn);
    }

    // Order is important here, we specifically want the group rebuild before the member
    verify(configuration).setString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, "group,member,my-pypi-group");
    verify(upgradeTaskScheduler).schedule(configuration);
  }


  private void insert(
      final int repositoryId,
      final int parentId,
      final String displayName,
      final String requestPath,
      final AssetData asset,
      final ComponentData component) throws SQLException
  {
    // we don't use the dao because the index shouldn't exist for PostgreSQL at this phase
    try (Connection conn = store.openConnection()) {
      try (PreparedStatement statement = conn.prepareStatement(INSERT)) {
        statement.setInt(1, repositoryId);
        statement.setInt(2, parentId);
        statement.setString(3, displayName);
        statement.setString(4, requestPath);
        if (asset != null) {
          statement.setInt(5, InternalIds.internalAssetId(asset));
        }
        else {
          statement.setNull(5, Types.INTEGER);
        }
        if (component != null) {
          statement.setInt(6, InternalIds.internalComponentId(component));
        }
        else {
          statement.setObject(6, null);
        }
        log.info("{} dipslayName: {} asset: {} component: {}", repositoryId, displayName, asset, component);
        statement.execute();
      }
    }
  }

  private AssetData createAsset(final ContentRepositoryData repository) {
    AssetData asset = randomAsset(InternalIds.contentRepositoryId(repository));
    try (DataSession<?> session = store.openSession()) {
      TestAssetDAO dao = session.access(TestAssetDAO.class);
      dao.createAsset(asset, false);
      session.getTransaction().commit();
    }
    return asset;
  }

  private ComponentData createComponent(final ContentRepositoryData repository) {
    ComponentData component = randomComponent(InternalIds.contentRepositoryId(repository));
    try (DataSession<?> session = store.openSession()) {
      TestComponentDAO dao = session.access(TestComponentDAO.class);
      dao.createComponent(component, false);
      session.getTransaction().commit();
    }
    return component;
  }

  private ContentRepositoryData createRepository(
      final String name,
      final String recipe,
      final ContentRepositoryData contentRepository)
  {
    ConfigurationData entity = new ConfigurationData();
    entity.setRepositoryName(name);
    entity.setRecipeName(recipe);
    entity.setOnline(true);
    entity.setAttributes(new HashMap<>());

    try (DataSession<?> session = store.openSession()) {
      ConfigurationDAO dao = session.access(ConfigurationDAO.class);
      dao.create(entity);
      session.getTransaction().commit();
    }

    contentRepository.setConfigRepositoryId(entity.getId());

    try (DataSession<?> session = store.openSession()) {
      ContentRepositoryDAO dao = session.access(TestContentRepositoryDAO.class);
      dao.createContentRepository(contentRepository);
      session.getTransaction().commit();
    }

    return contentRepository;
  }

  private interface PyPiBrowseNodeDAO
      extends BrowseNodeDAO
  {
  }

  private interface PyPiContentRepositoryDAO
      extends ContentRepositoryDAO
  {
  }

  private interface PyPiComponentDAO
      extends ComponentDAO
  {
  }

  private interface PyPiAssetBlobDAO
      extends AssetBlobDAO
  {
  }

  private interface PyPiAssetDAO
      extends AssetDAO
  {
  }
}
