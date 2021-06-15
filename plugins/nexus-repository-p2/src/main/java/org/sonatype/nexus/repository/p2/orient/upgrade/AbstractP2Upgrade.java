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
package org.sonatype.nexus.repository.p2.orient.upgrade;

import java.util.List;
import java.util.Optional;

import javax.inject.Provider;

import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseUpgradeSupport;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OSchemaProxy;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

abstract class AbstractP2Upgrade
    extends DatabaseUpgradeSupport
{
  private static final String SELECT_P2_REPOSITORIES = "select from repository where recipe_name = 'p2-proxy'";

  private static final String P_REPOSITORY_NAME = "repository_name";

  private static final String C_BROWSE_NODE = "browse_node";

  private static final String BROWSE_NODE_CLASS = new OClassNameBuilder().type(C_BROWSE_NODE).build();

  private static final String DELETE_BROWSE_NODE_FROM_REPOSITORIES =
      String.format("delete from %s where repository_name in ?", BROWSE_NODE_CLASS);

  private static final String I_REPOSITORY_NAME =
      new OIndexNameBuilder().type("bucket").property(P_REPOSITORY_NAME).build();

  protected static final String ASSET_CLASS_NAME = "asset";

  protected final Provider<DatabaseInstance> configDatabaseInstance;

  protected final Provider<DatabaseInstance> componentDatabaseInstance;

  protected AbstractP2Upgrade(
      final Provider<DatabaseInstance> configDatabaseInstance,
      final Provider<DatabaseInstance> componentDatabaseInstance)
  {
    this.configDatabaseInstance = checkNotNull(configDatabaseInstance);
    this.componentDatabaseInstance = checkNotNull(componentDatabaseInstance);
  }

  protected List<String> getP2RepositoryNames() {
    try (ODatabaseDocumentTx db = configDatabaseInstance.get().connect()) {
      return db.<List<ODocument>> query(new OSQLSynchQuery<ODocument>(SELECT_P2_REPOSITORIES)).stream()
          .map(d -> (String) d.field(P_REPOSITORY_NAME)).collect(toList());
    }
  }

  protected Optional<OIdentifiable> bucketFor(final ODatabaseDocumentTx db, final String repositoryName) {
    OIndex<?> bucketIdx = db.getMetadata().getIndexManager().getIndex(I_REPOSITORY_NAME);
    OIdentifiable bucket = (OIdentifiable) bucketIdx.get(repositoryName);
    if (bucket == null) {
      log.warn("Unable to find bucket for {}", repositoryName);
    }
    return Optional.ofNullable(bucket);
  }

  protected void deleteBrowseNodes(final List<String> p2RepositoryNames) {
    log.info("Deleting browse_node data from p2 repositories to be rebuilt ({}).", p2RepositoryNames);

    DatabaseUpgradeSupport.withDatabaseAndClass(componentDatabaseInstance, C_BROWSE_NODE, (db, type) -> {
      OSchemaProxy schema = db.getMetadata().getSchema();
      if (schema.existsClass(C_BROWSE_NODE)) {
        db.command(new OCommandSQL(DELETE_BROWSE_NODE_FROM_REPOSITORIES)).execute(p2RepositoryNames);
      }
    });
  }
}
