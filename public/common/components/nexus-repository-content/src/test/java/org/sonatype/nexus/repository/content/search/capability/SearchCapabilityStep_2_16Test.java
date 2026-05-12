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
package org.sonatype.nexus.repository.content.search.capability;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.content.testsuite.groups.PostgresTestGroup;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItemDAO;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItemData;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Recipe;
import org.sonatype.nexus.repository.config.ConfigurationDAO;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.repository.content.store.ContentRepositoryData;
import org.sonatype.nexus.repository.content.store.example.TestContentRepositoryDAO;
import org.sonatype.nexus.testdb.DataSessionRule;

import org.assertj.core.api.Condition;
import org.assertj.core.condition.MappedCondition;
import org.assertj.db.type.Table;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.condition.MappedCondition.mappedCondition;
import static org.assertj.db.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@Category(PostgresTestGroup.class)
@RunWith(MockitoJUnitRunner.Silent.class)
public class SearchCapabilityStep_2_16Test
{
  @Rule
  public DataSessionRule sessionRule =
      new DataSessionRule().access(CapabilityStorageItemDAO.class)
          .access(ConfigurationDAO.class)
          .access(TestContentRepositoryDAO.class);

  @Mock
  private Format format;

  @Mock
  private Recipe recipe;

  private MockedStatic<QualifierUtil> mockedStatic;

  private SearchCapabilityStep_2_16 underTest = new SearchCapabilityStep_2_16();

  @Before
  public void setup() {
    mockedStatic = Mockito.mockStatic(QualifierUtil.class);
    when(QualifierUtil.buildQualifierBeanMap(anyList())).thenReturn(Map.of("test-proxy", recipe));
    underTest.inject(List.of(recipe));
    lenient().when(recipe.getFormat()).thenReturn(format);
    lenient().when(format.getValue()).thenReturn("test");
  }

  @After
  public void tearDown() {
    mockedStatic.close();
  }

  @Test
  public void testMigrate_noCapability_noSearch() throws Exception {
    createRepository();
    try (Connection conn = spy(sessionRule.openConnection(DEFAULT_DATASTORE_NAME))) {
      underTest.migrate(conn);

      verify(conn, times(2)).prepareStatement("SELECT to_regclass(?);");
      // Check if table exists twice, plus a delete call to the table
      verify(conn, times(3)).prepareStatement(any());
    }
  }

  @Test
  public void testMigrate() throws Exception {
    stubSearchTable();
    createCapability();
    createRepository();
    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      underTest.migrate(conn);
    }

    assertThat(capabilityTable()).hasNumberOfRows(0);

    MappedCondition<byte[], String> condition =
        mappedCondition(b -> new String((byte[]) b),
            new Condition("{\"search_index_outdated\":true}"::equals, "equals"));

    assertThat(testContentRepositoryTable()).hasNumberOfRows(1)
        .row(0)
        .column("attributes")
        .value()
        .has(condition);
  }

  private void createRepository() {
    ConfigurationData configuration = new ConfigurationData();

    configuration.setAttributes(Map.of());
    configuration.setName("maven-central");
    configuration.setRecipeName("test-proxy");
    configuration.setRepositoryName("maven-central");

    ContentRepositoryData contentRepository = new ContentRepositoryData();
    contentRepository.setAttributes(new NestedAttributesMap());

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      session.access(ConfigurationDAO.class).create(configuration);

      contentRepository.setConfigRepositoryId(configuration.getRepositoryId());
      session.access(TestContentRepositoryDAO.class).createContentRepository(contentRepository);

      session.getTransaction().commit();
    }
  }

  private void createCapability() {
    CapabilityStorageItemData data = new CapabilityStorageItemData();
    data.setEnabled(true);
    data.setType("nexus.search.configuration");
    data.setNotes("");
    data.setProperties(Map.of());
    data.setVersion(1);
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      session.access(CapabilityStorageItemDAO.class).create(data);
      session.getTransaction().commit();
    }
  }

  private void stubSearchTable() {
    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME);
        PreparedStatement statement = conn.prepareStatement("CREATE TABLE search_components ( id int );")) {
      statement.execute();
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private Table capabilityTable() {
    DataStore<?> dataStore = sessionRule.getDataStore(DEFAULT_DATASTORE_NAME).orElseThrow(RuntimeException::new);
    return new Table(dataStore.getDataSource(), "capability_storage_item");
  }

  private Table testContentRepositoryTable() {
    DataStore<?> dataStore = sessionRule.getDataStore(DEFAULT_DATASTORE_NAME).orElseThrow(RuntimeException::new);
    return new Table(dataStore.getDataSource(), "test_content_repository");
  }
}
