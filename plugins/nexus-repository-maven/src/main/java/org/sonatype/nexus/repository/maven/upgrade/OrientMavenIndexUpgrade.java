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
package org.sonatype.nexus.repository.maven.upgrade;

import java.util.List;

import javax.inject.Provider;

import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseUpgradeSupport;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

/**
 * Upgrade step that marks existing maven repositories (at the time of the upgrade) as needing to be re-indexed.
 *
 * @since 3.38
 */
public class OrientMavenIndexUpgrade
    extends DatabaseUpgradeSupport
{
  private static final String SELECT_MAVEN_REPOSITORIES =
      "select from repository where recipe_name in ['maven2-hosted', 'maven2-proxy']";

  private static final String UPDATE_BUCKET_ATTRIBUTES =
      "update bucket set attributes.maven_search_index_outdated = true where repository_name in ?";

  private static final String P_REPOSITORY_NAME = "repository_name";

  private final Provider<DatabaseInstance> configDatabaseInstance;

  private final Provider<DatabaseInstance> componentDatabaseInstance;

  public OrientMavenIndexUpgrade(
      final Provider<DatabaseInstance> configDatabaseInstance,
      final Provider<DatabaseInstance> componentDatabaseInstance)
  {
    this.configDatabaseInstance = checkNotNull(configDatabaseInstance);
    this.componentDatabaseInstance = checkNotNull(componentDatabaseInstance);
  }

  @Override
  public void apply() throws Exception {
    if (hasSchemaClass(configDatabaseInstance, "repository") && hasSchemaClass(componentDatabaseInstance, "bucket")) {
      markMavenRepositoriesThatNeedUpdate();
    }
  }

  private void markMavenRepositoriesThatNeedUpdate() {
    List<String> repositoryNames;
    try (ODatabaseDocumentTx db = configDatabaseInstance.get().connect()) {
      repositoryNames = db.<List<ODocument>>query(new OSQLSynchQuery<ODocument>(SELECT_MAVEN_REPOSITORIES)).stream()
          .map(d -> (String) d.field(P_REPOSITORY_NAME))
          .collect(toList());
    }
    if (!repositoryNames.isEmpty()) {
      log.info("Marking existing maven repositories as needing search index update ({}).", repositoryNames);
      OCommandSQL updateBucketsCommand = new OCommandSQL(UPDATE_BUCKET_ATTRIBUTES);
      int updates;
      try (ODatabaseDocumentTx db = componentDatabaseInstance.get().connect()) {
        updates = db.command(updateBucketsCommand).execute(repositoryNames);
      }
      log.info("Marked {} existing maven repositories as needing search index update.", updates);
    }
  }
}
