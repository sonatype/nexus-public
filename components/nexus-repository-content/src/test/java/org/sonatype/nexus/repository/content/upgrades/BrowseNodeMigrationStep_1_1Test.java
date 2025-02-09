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
import java.util.Collections;

import org.assertj.db.type.AssertDbConnectionFactory;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.content.browse.RebuildBrowseNodesManager;
import org.sonatype.nexus.repository.content.browse.store.example.TestBrowseNodeDAO;
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

import org.assertj.db.type.Table;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.db.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

public class BrowseNodeMigrationStep_1_1Test
  extends ExampleContentTestSupport
{
  private final String INSERT = "INSERT INTO test_browse_node (repository_id, parent_id, display_name, request_path,"
      + " asset_id, component_id) "
      + " VALUES (?, ?, ?, ?, ?, ?);";

  @Mock
  private Format fakeFormat;

  @Mock
  private RebuildBrowseNodesManager rebuildBrowseNodesManager;

  private BrowseNodeMigrationStep_1_1 upgradeStep;

  private DataStore<?> store;

  public BrowseNodeMigrationStep_1_1Test() {
    super(TestBrowseNodeDAO.class);
  }


  @Before
  public void setup() {
    when(fakeFormat.getValue()).thenReturn("test");
    upgradeStep = new BrowseNodeMigrationStep_1_1(Collections.singletonList(fakeFormat), rebuildBrowseNodesManager);
    store = sessionRule.getDataStore(DEFAULT_DATASTORE_NAME).get();
  }

  @Test
  public void testUnknownFormat() throws Exception {
    when(fakeFormat.getValue()).thenReturn("foo");
    BrowseNodeMigrationStep_1_1 upgradeStep =
        new BrowseNodeMigrationStep_1_1(Collections.singletonList(fakeFormat), rebuildBrowseNodesManager);

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
    ContentRepositoryData repo = createContentRepository(randomContentRepository());

    insert(repo.contentRepositoryId(), 0, "jquery", "/jquery/", null, null);

    try (Connection conn = store.openConnection()) {
      upgradeStep.migrate(conn);
    }

    Table testBrowseNode = AssertDbConnectionFactory
            .of(store.getDataSource()).create()
            .table("test_browse_node").build();
    assertThat(testBrowseNode).isEmpty();

    verify(rebuildBrowseNodesManager).setRebuildOnSart(true);
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
        statement.execute();
      }
    }
  }

  private void backup() throws SQLException {
    try (Connection conn = store.openConnection()) {
      try (PreparedStatement statement = conn.prepareStatement("SCRIPT TO 'backup.sql'")) {
        statement.execute();
      }
    }
  }

  private AssetData createAsset(final AssetData asset) {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO assetDao = session.access(TestAssetDAO.class);

      assetDao.createAsset(asset, false);

      session.getTransaction().commit();
    }
    return asset;
  }

  private ComponentData createComponent(final ComponentData component) {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ComponentDAO componentDao = session.access(TestComponentDAO.class);

      componentDao.createComponent(component, false);

      session.getTransaction().commit();
    }
    return component;
  }

  private ContentRepositoryData createContentRepository(final ContentRepositoryData contentRepository) {
    try (DataSession<?> session = store.openSession()) {
      ContentRepositoryDAO dao = session.access(TestContentRepositoryDAO.class);
      dao.createContentRepository(contentRepository);
      session.getTransaction().commit();
    }
    return contentRepository;
  }

}
