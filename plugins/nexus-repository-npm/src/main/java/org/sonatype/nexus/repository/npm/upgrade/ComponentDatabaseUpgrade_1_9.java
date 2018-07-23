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
package org.sonatype.nexus.repository.npm.upgrade;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.upgrade.DependsOn;
import org.sonatype.nexus.common.upgrade.Upgrades;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.orient.DatabaseUpgradeSupport;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

/**
 * Upgrade step that marks existing npm repositories (at the time of the upgrade) as not supporting npm v1 search. These
 * flags can be used to identify legacy repositories that must be reindexed using {@code ReindexNpmRepositoryTask}.
 *
 * @since 3.7
 */
@Named
@Singleton
@Upgrades(model = DatabaseInstanceNames.COMPONENT, from = "1.8", to = "1.9")
@DependsOn(model = DatabaseInstanceNames.CONFIG, version = "1.5")
public class ComponentDatabaseUpgrade_1_9 // NOSONAR
    extends DatabaseUpgradeSupport
{
  private static final String SELECT_NPM_REPOSITORIES =
      "select from repository where recipe_name in ['npm-hosted', 'npm-proxy']";

  private static final String UPDATE_BUCKET_ATTRIBUTES =
      "update bucket set attributes.npm_v1_search_unsupported = true where repository_name in ?";

  private static final String P_REPOSITORY_NAME = "repository_name";

  private final Provider<DatabaseInstance> configDatabaseInstance;

  private final Provider<DatabaseInstance> componentDatabaseInstance;

  @Inject
  public ComponentDatabaseUpgrade_1_9(
      @Named(DatabaseInstanceNames.CONFIG) final Provider<DatabaseInstance> configDatabaseInstance,
      @Named(DatabaseInstanceNames.COMPONENT) final Provider<DatabaseInstance> componentDatabaseInstance)
  {
    this.configDatabaseInstance = checkNotNull(configDatabaseInstance);
    this.componentDatabaseInstance = checkNotNull(componentDatabaseInstance);
  }

  @Override
  public void apply() throws Exception {
    if (hasSchemaClass(configDatabaseInstance, "repository") && hasSchemaClass(componentDatabaseInstance, "bucket")) {
      markNpmRepositoriesWithoutV1SearchIndexing();
    }
  }

  private void markNpmRepositoriesWithoutV1SearchIndexing() {
    List<String> npmRepositoryNames;
    try (ODatabaseDocumentTx db = configDatabaseInstance.get().connect()) {
      npmRepositoryNames = db.<List<ODocument>>query(new OSQLSynchQuery<ODocument>(SELECT_NPM_REPOSITORIES)).stream()
          .map(d -> (String) d.field(P_REPOSITORY_NAME))
          .collect(toList());
    }
    if (!npmRepositoryNames.isEmpty()) {
      log.info("Marking existing npm repositories as not supporting v1 search ({}).", npmRepositoryNames);
      OCommandSQL updateBucketsCommand = new OCommandSQL(UPDATE_BUCKET_ATTRIBUTES);
      int updates;
      try (ODatabaseDocumentTx db = componentDatabaseInstance.get().connect()) {
        updates = db.command(updateBucketsCommand).execute(npmRepositoryNames);
      }
      log.info("Marked {} existing npm repositories as not supporting v1 search.", updates);
    }
  }
}
