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
import java.util.Collections;

import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.repository.config.ConfigurationDAO;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.DatabaseTest;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;

import com.google.common.collect.ImmutableMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

/**
 * Real-database tests for {@link MavenDefaultReposUpgrade_1_17} against the {@code repository} table. These
 * run against H2 only; PostgreSQL parity (the {@code JSON} vs {@code JSONB} attributes binding) is not
 * exercised here and will be covered by the forthcoming {@code UpgradeMatrixIT} in
 * {@code nexus-integration-tests}.
 */
class MavenDefaultReposUpgrade_1_17Test
{
  @DataSessionConfiguration(daos = {ConfigurationDAO.class})
  TestDataSessionSupplier dataSessionSupplier;

  private final MavenDefaultReposUpgrade_1_17 underTest = new MavenDefaultReposUpgrade_1_17();

  @DatabaseTest
  void migrate_setsInlineContentDispositionOnDefaultMavenHostedAndProxyReposOnly() throws Exception {
    seedRepositories();

    try (Connection conn = dataSessionSupplier.openConnection()) {
      underTest.migrate(conn);
    }

    try (DataSession<?> session = dataSessionSupplier.openSession(DEFAULT_DATASTORE_NAME)) {
      ConfigurationDAO dao = session.access(ConfigurationDAO.class);

      // every default non-group Maven repository (releases/snapshots hosted, central proxy) gets INLINE
      assertThat(contentDisposition(dao, "maven-releases")).isEqualTo("INLINE");
      assertThat(contentDisposition(dao, "maven-snapshots")).isEqualTo("INLINE");
      assertThat(contentDisposition(dao, "maven-central")).isEqualTo("INLINE");
      // the default group repository is not in the set, so it is unchanged
      assertThat(contentDisposition(dao, "maven-public")).isEqualTo("ATTACHMENT");
      // non-default repositories are unchanged
      assertThat(contentDisposition(dao, "my-custom-repo")).isEqualTo("ATTACHMENT");
    }
  }

  private void seedRepositories() {
    try (DataSession<?> session = dataSessionSupplier.openSession(DEFAULT_DATASTORE_NAME)) {
      ConfigurationDAO dao = session.access(ConfigurationDAO.class);
      create(dao, "maven-releases", "maven2-hosted", null);
      create(dao, "maven-snapshots", "maven2-hosted", null);
      create(dao, "maven-central", "maven2-proxy", null);
      create(dao, "maven-public", "maven2-group", "ATTACHMENT");
      create(dao, "my-custom-repo", "maven2-hosted", "ATTACHMENT");
      session.getTransaction().commit();
    }
  }

  private static String contentDisposition(final ConfigurationDAO dao, final String name) {
    ConfigurationData config = dao.readByName(name).orElseThrow();
    return config.attributes("maven").get("contentDisposition", String.class);
  }

  private static void create(
      final ConfigurationDAO dao,
      final String name,
      final String recipeName,
      final String contentDisposition)
  {
    ConfigurationData config = new ConfigurationData();
    config.setName(name);
    config.setRecipeName(recipeName);
    config.setAttributes(ImmutableMap.of("maven", contentDisposition != null
        ? ImmutableMap.of("contentDisposition", contentDisposition)
        : Collections.emptyMap()));
    dao.create(config);
  }
}
