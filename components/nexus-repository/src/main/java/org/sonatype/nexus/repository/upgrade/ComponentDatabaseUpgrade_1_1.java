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
package org.sonatype.nexus.repository.upgrade;

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
import org.sonatype.nexus.orient.OIndexNameBuilder;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Upgrade step to set {@code asset_kind} for Docker layer blobs.
 *
 * @since 3.2.1
 */
@Named
@Singleton
@Upgrades(model = DatabaseInstanceNames.COMPONENT, from = "1.0", to = "1.1")
@DependsOn(model = DatabaseInstanceNames.CONFIG, version = "1.2")
public class ComponentDatabaseUpgrade_1_1 // NOSONAR
    extends DatabaseUpgradeSupport
{
  private static final String SELECT_DOCKER_REPOSITORIES =
      "select from repository where recipe_name in ['docker-hosted', 'docker-proxy']";

  private static final String UPDATE_ASSET_KIND =
      "update asset set attributes.docker.asset_kind='BLOB' " +
          "where bucket=? and name like '%/blobs/%' and attributes.docker.asset_kind='MANIFEST'";

  private static final String P_REPOSITORY_NAME = "repository_name";

  private static final String I_REPOSITORY_NAME = new OIndexNameBuilder()
      .type("bucket")
      .property(P_REPOSITORY_NAME)
      .build();

  private final Provider<DatabaseInstance> configDatabaseInstance;

  private final Provider<DatabaseInstance> componentDatabaseInstance;

  @Inject
  public ComponentDatabaseUpgrade_1_1(
      @Named(DatabaseInstanceNames.CONFIG) final Provider<DatabaseInstance> configDatabaseInstance,
      @Named(DatabaseInstanceNames.COMPONENT) final Provider<DatabaseInstance> componentDatabaseInstance)
  {
    this.configDatabaseInstance = checkNotNull(configDatabaseInstance);
    this.componentDatabaseInstance = checkNotNull(componentDatabaseInstance);
  }

  @Override
  public void apply() throws Exception {
    if (hasSchemaClass(configDatabaseInstance, "repository") && hasSchemaClass(componentDatabaseInstance, "asset")) {
      updateDockerBlobAssetKind();
    }
  }

  private void updateDockerBlobAssetKind() {
    List<String> dockerRepositoryNames;
    try (ODatabaseDocumentTx db = configDatabaseInstance.get().connect()) {
      dockerRepositoryNames = db.<List<ODocument>>query(new OSQLSynchQuery<ODocument>(SELECT_DOCKER_REPOSITORIES)).stream()
          .map(d -> (String)d.field(P_REPOSITORY_NAME))
          .collect(Collectors.toList());
    }
    if (!dockerRepositoryNames.isEmpty()) {
      OCommandSQL updateAssetCommand = new OCommandSQL(UPDATE_ASSET_KIND);
      try (ODatabaseDocumentTx db = componentDatabaseInstance.get().connect()) {
        OIndex<?> bucketIdx = db.getMetadata().getIndexManager().getIndex(I_REPOSITORY_NAME);
        dockerRepositoryNames.forEach(repositoryName -> {
          log.info("Scanning docker repository, {}, for misconfigured assets", repositoryName);
          OIdentifiable bucket = (OIdentifiable) bucketIdx.get(repositoryName);
          if (bucket == null) {
            log.warn("Unable to find bucket for {}", repositoryName);
          }
          else {
            int updates = db.command(updateAssetCommand).execute(bucket.getIdentity());
            if (updates > 0) {
              log.info(
                  "Updated {} misconfigured docker asset(s) in repository {}: set attributes.docker.asset_kind='BLOB'",
                  updates, repositoryName);
            }
          }
        });
      }
    }
  }
}
