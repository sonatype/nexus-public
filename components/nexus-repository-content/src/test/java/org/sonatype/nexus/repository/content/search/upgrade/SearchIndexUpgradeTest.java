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
package org.sonatype.nexus.repository.content.search.upgrade;

import java.sql.Connection;
import java.util.Map;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Recipe;
import org.sonatype.nexus.repository.config.ConfigurationDAO;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.repository.content.ContentRepository;
import org.sonatype.nexus.repository.content.store.ContentRepositoryData;
import org.sonatype.nexus.repository.content.store.example.TestContentRepositoryDAO;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.testdb.DataSessionRule;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static java.lang.Boolean.TRUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.repository.search.index.SearchUpdateService.SEARCH_INDEX_OUTDATED;

public class SearchIndexUpgradeTest
    extends TestSupport
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule(DEFAULT_DATASTORE_NAME)
      .access(ConfigurationDAO.class)
      .access(TestContentRepositoryDAO.class);

  private DataStore<?> store;

  private ConfigurationDAO configurationDAO;

  private TestContentRepositoryDAO contentRepositoryDAO;

  @Mock
  private Recipe testHostedRecipe;

  @Mock
  private Recipe testProxyRecipe;

  @Mock
  private Recipe testGroupRecipe;

  private SearchIndexUpgrade underTest;

  private ConfigurationData hosted1Config;

  private ConfigurationData proxyConfig;

  private ConfigurationData groupConfig;

  @Before
  public void setup() {
    Format testFormat = new Format("test") { };

    when(testHostedRecipe.getFormat()).thenReturn(testFormat);
    when(testHostedRecipe.getType()).thenReturn(new HostedType());

    when(testProxyRecipe.getFormat()).thenReturn(testFormat);
    when(testProxyRecipe.getType()).thenReturn(new ProxyType());

    when(testGroupRecipe.getFormat()).thenReturn(testFormat);
    when(testGroupRecipe.getType()).thenReturn(new GroupType());

    underTest = new TestSearchIndexUpgrade();
    underTest.inject(ImmutableMap.of(
        "test-hosted", testHostedRecipe,
        "test-proxy", testProxyRecipe,
        "test-group", testGroupRecipe));

    store = sessionRule.getDataStore(DEFAULT_DATASTORE_NAME).get();
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      configurationDAO = session.access(ConfigurationDAO.class);
      contentRepositoryDAO = session.access(TestContentRepositoryDAO.class);

      hosted1Config = createConfig("hosted1", "test-hosted");
      createConfig("hosted2", "test-hosted");
      proxyConfig = createConfig("proxy", "test-proxy");
      groupConfig = createConfig("group", "test-group");

      createContentRepository(hosted1Config.getId(), new NestedAttributesMap());
      // intentionally not creating content repository for hosted2 to test mis-configuration
      NestedAttributesMap attrs = new NestedAttributesMap();
      attrs.child("test").set("value", "3");
      createContentRepository(proxyConfig.getId(), attrs);
      createContentRepository(groupConfig.getId(), new NestedAttributesMap());

      session.getTransaction().commit();
    }
  }

  @Test
  public void testMigration() throws Exception {
    try (Connection conn = store.openConnection()) {
      underTest.migrate(conn);
    }

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      TestContentRepositoryDAO dao = session.access(TestContentRepositoryDAO.class);
      Optional<ContentRepository> foundHosted = dao.readContentRepository(hosted1Config.getId());
      Optional<ContentRepository> foundProxy = dao.readContentRepository(proxyConfig.getId());
      Optional<ContentRepository> foundGroup = dao.readContentRepository(groupConfig.getId());
      assertTrue(isRepositoryMarked(foundHosted.get()));
      assertTrue(isRepositoryMarked(foundProxy.get()));
      assertFalse(isRepositoryMarked(foundGroup.get()));

      // test that existing attributes are still there
      Map<String, Object> fooAttrs = (Map<String, Object>) foundProxy.get().attributes().get("test");
      assertEquals("3", fooAttrs.get("value"));
    }
  }

  private ConfigurationData createConfig(final String name, final String recipeName) {
    ConfigurationData config = new ConfigurationData();
    config.setName(name);
    config.setRecipeName(recipeName);
    config.setAttributes(ImmutableMap.of());

    configurationDAO.create(config);
    return config;
  }

  private void createContentRepository(final EntityId configId, final NestedAttributesMap attributes) {
    ContentRepositoryData repo = new ContentRepositoryData();
    repo.setConfigRepositoryId(configId);
    repo.setAttributes(attributes);

    contentRepositoryDAO.createContentRepository(repo);
  }

  private boolean isRepositoryMarked(final ContentRepository repo) {
    return TRUE.equals(repo.attributes().get(SEARCH_INDEX_OUTDATED));
  }

  private static class TestSearchIndexUpgrade
      extends SearchIndexUpgrade
  {
    @Override
    public Optional<String> version() {
      return Optional.of("1.0");
    }
  }
}
