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

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OSchemaProxy;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

/**
 * Upgrade step to delete browse_node entries for docker repositories forcing them to be rebuilt by {@link
 * org.sonatype.nexus.repository.browse.internal.RebuildBrowseNodesManager}.
 *
 * @since 3.10
 */
@Named
@Singleton
@Upgrades(model = DatabaseInstanceNames.COMPONENT, from = "1.10", to = "1.11")
@DependsOn(model = DatabaseInstanceNames.CONFIG, version = "1.5")
public class ComponentDatabaseUpgrade_1_11 // NOSONAR
    extends DatabaseUpgradeSupport
{
  private final Provider<DatabaseInstance> configDatabaseInstance;

  private final Provider<DatabaseInstance> componentDatabaseInstance;

  private static final String SELECT_DOCKER_REPOSITORIES = "select from repository where recipe_name in ['docker-hosted', 'docker-proxy']";

  private static final String P_REPOSITORY_NAME = "repository_name";

  private static final String C_BROWSE_NODE = "browse_node";

  private static final String BROWSE_NODE_CLASS = new OClassNameBuilder().type(C_BROWSE_NODE).build();

  private static final String DELETE_BROWSE_NODE_FROM_REPOSITORIES = String
      .format("delete from %s where repository_name in ?", BROWSE_NODE_CLASS);

  @Inject
  public ComponentDatabaseUpgrade_1_11(
      @Named(DatabaseInstanceNames.CONFIG) final Provider<DatabaseInstance> configDatabaseInstance,
      @Named(DatabaseInstanceNames.COMPONENT) final Provider<DatabaseInstance> componentDatabaseInstance)
  {
    this.configDatabaseInstance = checkNotNull(configDatabaseInstance);
    this.componentDatabaseInstance = checkNotNull(componentDatabaseInstance);
  }

  @Override
  public void apply() throws Exception {
    List<String> repositoryNames;

    try (ODatabaseDocumentTx configDb = configDatabaseInstance.get().connect()) {
      repositoryNames = configDb.<List<ODocument>>query(
          new OSQLSynchQuery<ODocument>(SELECT_DOCKER_REPOSITORIES)).stream()
          .map(d -> (String) d.field(P_REPOSITORY_NAME))
          .collect(toList());
    }

    if (repositoryNames != null && !repositoryNames.isEmpty()) {
      log.info("Deleting browse_node data from docker repositories to be rebuilt ({}).", repositoryNames);

      try (ODatabaseDocumentTx componentDb = componentDatabaseInstance.get().connect()) {
        OSchemaProxy schema = componentDb.getMetadata().getSchema();
        if (schema.existsClass(C_BROWSE_NODE)) {
          componentDb.command(new OCommandSQL(DELETE_BROWSE_NODE_FROM_REPOSITORIES)).execute(repositoryNames);
        }
      }
    }
  }
}

