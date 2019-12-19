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
package org.sonatype.nexus.repository.pypi.upgrade;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.upgrade.DependsOn;
import org.sonatype.nexus.common.upgrade.Upgrades;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.orient.DatabaseUpgradeSupport;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Upgrade step to invalidate all index files because of the change that adds the requires metadata field, previously
 * built indexes will be missing this new property so we need to delete the indexes, so they get rebuilt on the next
 * request.
 *
 * @since 3.20
 */
@Named
@Singleton
@Upgrades(model = PyPiModel.NAME, from = "1.0", to = "1.1")
@DependsOn(model = DatabaseInstanceNames.COMPONENT, version = "1.14", checkpoint = true)
@DependsOn(model = DatabaseInstanceNames.CONFIG, version = "1.8")
public class PypiUpgrade_1_1
    extends DatabaseUpgradeSupport
{
  private static final String SELECT_PYPI_REPOSITORIES =
      "select from repository where recipe_name in ['pypi-hosted', 'pypi-proxy', 'pypi-group']";

  private static final String DELETE_INDEXES =
      "delete from asset where bucket=? and (attributes.pypi.asset_kind='ROOT_INDEX' or attributes.pypi.asset_kind='INDEX')";

  private static final String DELETE_BROWSE_NODES =
      "delete from browse_node where repository_name = ?";

  private static final String P_REPOSITORY_NAME = "repository_name";

  private static final String BROWSE_NODE_CLASS = new OClassNameBuilder().type("browse_node").build();

  private static final String I_REPOSITORY_NAME = new OIndexNameBuilder()
      .type("bucket")
      .property(P_REPOSITORY_NAME)
      .build();

  private final Provider<DatabaseInstance> configDatabaseInstance;

  private final Provider<DatabaseInstance> componentDatabaseInstance;

  @Inject
  public PypiUpgrade_1_1(
      @Named(DatabaseInstanceNames.CONFIG) final Provider<DatabaseInstance> configDatabaseInstance,
      @Named(DatabaseInstanceNames.COMPONENT) final Provider<DatabaseInstance> componentDatabaseInstance)
  {
    this.configDatabaseInstance = checkNotNull(configDatabaseInstance);
    this.componentDatabaseInstance = checkNotNull(componentDatabaseInstance);
  }

  @Override
  public void apply() throws Exception {
    if (hasSchemaClass(configDatabaseInstance, "repository") &&
        hasSchemaClass(componentDatabaseInstance, "asset")) {
      deleteIndexes();
    }
  }

  private void deleteIndexes() {
    List<String> pypiRepositoryNames;
    try (ODatabaseDocumentTx db = configDatabaseInstance.get().connect()) {
      pypiRepositoryNames = db.<List<ODocument>>query(new OSQLSynchQuery<ODocument>(SELECT_PYPI_REPOSITORIES)).stream()
          .map(d -> (String) d.field(P_REPOSITORY_NAME))
          .collect(Collectors.toList());
    }

    if (!pypiRepositoryNames.isEmpty()) {
      OCommandSQL deleteIndexCommand = new OCommandSQL(DELETE_INDEXES);
      OCommandSQL deleteBrowseNodesCommand = new OCommandSQL(DELETE_BROWSE_NODES);
      try (ODatabaseDocumentTx db = componentDatabaseInstance.get().connect()) {
        OIndex<?> bucketIdx = db.getMetadata().getIndexManager().getIndex(I_REPOSITORY_NAME);
        pypiRepositoryNames.forEach(repositoryName -> {
          log.info("Scanning pypi repository {} for index file assets", repositoryName);
          OIdentifiable bucket = (OIdentifiable) bucketIdx.get(repositoryName);
          if (bucket == null) {
            log.warn("Unable to find bucket for {}", repositoryName);
          }
          else {
            // Deleting index files
            int deletes = db.command(deleteIndexCommand).execute(bucket.getIdentity());
            if (deletes > 0) {
              log.info(
                  "Deleted {} pypi index asset(s) in repository {}: ", deletes, repositoryName);
            }

            if (db.getMetadata().getSchema().existsClass(BROWSE_NODE_CLASS)) {
              // Deleting browse nodes
              deletes = db.command(deleteBrowseNodesCommand).execute(repositoryName);
              if (deletes > 0) {
                log.info(
                    "Deleted {} browse node(s) in repository {}", deletes, repositoryName);
              }
            }
          }
        });
      }
    }
  }
}
