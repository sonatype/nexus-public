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
package org.sonatype.nexus.internal.selector;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.selector.SelectorConfiguration;

import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SCHEMAS;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.orient.OrientTransaction.inTx;
import static org.sonatype.nexus.orient.OrientTransaction.inTxNoReturn;

/**
 * Default {@link SelectorConfigurationStore} implementation.
 *
 * @since 3.0
 */
@Named
@Singleton
@ManagedLifecycle(phase = SCHEMAS)
public class SelectorConfigurationStoreImpl
    extends StateGuardLifecycleSupport
    implements SelectorConfigurationStore
{
  private final Provider<DatabaseInstance> databaseInstance;

  private final SelectorConfigurationEntityAdapter entityAdapter;

  @Inject
  public SelectorConfigurationStoreImpl(@Named(DatabaseInstanceNames.CONFIG) final Provider<DatabaseInstance> databaseInstance,
                                        final SelectorConfigurationEntityAdapter entityAdapter)
  {
    this.databaseInstance = databaseInstance;
    this.entityAdapter = entityAdapter;
  }

  @Override
  protected void doStart() throws Exception {
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      entityAdapter.register(db);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public List<SelectorConfiguration> browse() {
    return inTx(databaseInstance, db -> Lists.newArrayList(entityAdapter.browse.execute(db)));
  }

  @Override
  @Guarded(by = STARTED)
  public SelectorConfiguration read(final EntityId entityId) {
    checkNotNull(entityId);

    return inTx(databaseInstance, db -> entityAdapter.read.execute(db, entityId));
  }

  @Override
  @Guarded(by = STARTED)
  public void create(final SelectorConfiguration configuration) {
    checkNotNull(configuration);

    inTx(databaseInstance, db -> entityAdapter.addEntity(db, configuration));
  }

  @Override
  @Guarded(by = STARTED)
  public void update(final SelectorConfiguration configuration) {
    checkNotNull(configuration);

    inTx(databaseInstance, db -> entityAdapter.editEntity(db, configuration));
  }

  @Override
  @Guarded(by = STARTED)
  public void delete(final SelectorConfiguration configuration) {
    checkNotNull(configuration);

    inTxNoReturn(databaseInstance, db -> entityAdapter.deleteEntity(db, configuration));
  }
}
