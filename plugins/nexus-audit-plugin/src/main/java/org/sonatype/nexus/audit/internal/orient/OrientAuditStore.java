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
package org.sonatype.nexus.audit.internal.orient;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.internal.AuditStore;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.orient.DatabaseInstance;

import com.google.common.collect.ImmutableList;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SCHEMAS;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTx;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTxRetry;

/**
 * OrientDB implementation of {@link AuditStore}.
 *
 * @since 3.1
 */
@Named("orient")
@Singleton
@ManagedLifecycle(phase = SCHEMAS)
public class OrientAuditStore
    extends LifecycleSupport
    implements AuditStore
{
  private final Provider<DatabaseInstance> databaseInstance;

  private final AuditDataEntityAdapter entityAdapter;

  @Inject
  public OrientAuditStore(@Named("audit") final Provider<DatabaseInstance> databaseInstance,
                          final AuditDataEntityAdapter entityAdapter)
  {
    this.databaseInstance = checkNotNull(databaseInstance);
    this.entityAdapter = checkNotNull(entityAdapter);
  }

  @Override
  protected void doStart() throws Exception {
    // create database
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      // register schema
      entityAdapter.register(db);
    }
  }

  @Override
  public void add(final AuditData data) throws Exception {
    checkNotNull(data);
    ensureStarted();

    checkState(!EventHelper.isReplicating(), "Replication in progress");

    inTxRetry(databaseInstance).run(db -> entityAdapter.addEntity(db, data));
  }

  @Override
  public void clear() throws Exception {
    ensureStarted();

    inTxRetry(databaseInstance).run(entityAdapter::clear);
  }

  @Override
  public long approximateSize() throws Exception {
    ensureStarted();

    return inTx(databaseInstance).call(entityAdapter::count);
  }

  @Override
  public List<AuditData> browse(final long offset, final long limit) throws Exception {
    ensureStarted();

    return inTx(databaseInstance).call(db -> ImmutableList.copyOf(entityAdapter.browse(db, offset, limit)));
  }
}
