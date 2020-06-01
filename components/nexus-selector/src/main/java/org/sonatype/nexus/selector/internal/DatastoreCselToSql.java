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
package org.sonatype.nexus.selector.internal;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.datastore.api.DataStoreManager;
import org.sonatype.nexus.selector.CselToSql;
import org.sonatype.nexus.selector.SelectorSqlBuilder;

import org.apache.commons.jexl3.parser.ASTJexlScript;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.datastore.api.DataStoreManager.CONTENT_DATASTORE_NAME;

/**
 * Walks the script, transforming CSEL expressions into Datastore appropriate SQL clauses.
 *
 * @since 3.24
 */
@Named("mybatis")
@Singleton
@ManagedLifecycle(phase = SERVICES)
public class DatastoreCselToSql
    extends StateGuardLifecycleSupport
    implements CselToSql
{
  private final DataStoreManager dataStoreManager;

  private final Map<String, DatastoreSqlTransformer> transformerMap;

  private Optional<String> dataStoreId;

  @Inject
  public DatastoreCselToSql(
      final DataStoreManager dataStoreManager,
      final Map<String, DatastoreSqlTransformer> transformerMap)
  {
    this.dataStoreManager = checkNotNull(dataStoreManager);
    this.transformerMap = checkNotNull(transformerMap);
  }

  @Override
  protected void doStart() throws Exception {
    dataStoreId = dataStoreManager
        .get(CONTENT_DATASTORE_NAME)
        .map(DataStore::getDatabaseId);
  }

  @Override
  @Guarded(by = STARTED)
  public void transformCselToSql(final ASTJexlScript script, final SelectorSqlBuilder builder) {
    final DatastoreSqlTransformer transformer = dataStoreId
        .map(transformerMap::get)
        .orElseThrow(() -> new IllegalStateException("Cannot find sql transformer for " + dataStoreId));

    script.childrenAccept(transformer, builder);
  }
}
