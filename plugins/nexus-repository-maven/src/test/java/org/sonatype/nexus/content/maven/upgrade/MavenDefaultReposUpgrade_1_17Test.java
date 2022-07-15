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
package org.sonatype.nexus.content.maven.upgrade;

import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.repository.config.ConfigurationDAO;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.repository.maven.internal.MavenDefaultRepositoriesContributor;
import org.sonatype.nexus.testdb.DataSessionRule;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

public class MavenDefaultReposUpgrade_1_17Test
    extends TestSupport
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule(DEFAULT_DATASTORE_NAME)
      .access(ConfigurationDAO.class);

  private DataStore<?> store;

  private ConfigurationDAO configurationDAO;

  private MavenDefaultReposUpgrade_1_17 migrationStep;

  private ConfigurationData hostedRepo;

  private ConfigurationData proxyRepo;

  private ConfigurationData groupRepo;

  private ConfigurationData nonDefaultRepo;

  @Before
  public void setUp() {
    createMockData();

    MavenDefaultRepositoriesContributor mockContributor = mock(MavenDefaultRepositoriesContributor.class);
    when(mockContributor.getRepositoryConfigurations()).thenReturn(Arrays.asList(hostedRepo, proxyRepo, groupRepo));

    migrationStep = new MavenDefaultReposUpgrade_1_17(mockContributor);
  }

  private void createMockData() {
    store = sessionRule.getDataStore(DEFAULT_DATASTORE_NAME).get();
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      configurationDAO = session.access(ConfigurationDAO.class);

      hostedRepo = createConfig("hosted1", "maven2-hosted", null);
      nonDefaultRepo = createConfig("hosted2", "maven2-hosted", "ATTACHMENT");
      proxyRepo = createConfig("proxy", "maven2-proxy", null);
      groupRepo = createConfig("group", "maven2-group", "ATTACHMENT");

      session.getTransaction().commit();
    }
  }

  @Test
  public void testMigrationWorksAsExpected() throws Exception {
    try (Connection conn = store.openConnection()) {
      migrationStep.migrate(conn);
    }

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      configurationDAO = session.access(ConfigurationDAO.class);

      ConfigurationData modifiedHostedRepo = configurationDAO.readByName(hostedRepo.getName()).get();
      ConfigurationData modifiedProxyRepo = configurationDAO.readByName(proxyRepo.getName()).get();
      ConfigurationData groupRepo = configurationDAO.readByName(this.groupRepo.getName()).get();

      ConfigurationData nonDefault = configurationDAO.readByName(nonDefaultRepo.getName()).get();

      assertEquals("INLINE", modifiedHostedRepo.attributes("maven").get("contentDisposition", String.class));
      assertEquals("INLINE", modifiedProxyRepo.attributes("maven").get("contentDisposition", String.class));

      //if it is a group repo , it shouldn't change
      assertEquals("ATTACHMENT", groupRepo.attributes("maven").get("contentDisposition", String.class));

      //If it is a non-default repo , then the value shouldn't change
      assertEquals("ATTACHMENT", nonDefault.attributes("maven").get("contentDisposition", String.class));
    }
  }

  private ConfigurationData createConfig(final String name, final String recipeName, String contentDisposition) {
    ConfigurationData config = new ConfigurationData();
    config.setName(name);
    config.setRecipeName(recipeName);
    config.setAttributes(ImmutableMap.of("maven", contentDisposition != null ? ImmutableMap.of("contentDisposition",
        contentDisposition) : Collections.emptyMap()));

    configurationDAO.create(config);
    return config;
  }
}
