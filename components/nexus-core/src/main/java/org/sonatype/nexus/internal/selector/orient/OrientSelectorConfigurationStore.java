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
package org.sonatype.nexus.internal.selector.orient;

import java.util.List;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.internal.selector.SelectorConfigurationStore;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.selector.OrientSelectorConfiguration;
import org.sonatype.nexus.selector.SelectorConfiguration;

import com.google.common.collect.ImmutableList;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SCHEMAS;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTx;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTxRetry;

/**
 * Default {@link SelectorConfigurationStore} implementation.
 *
 * @since 3.0
 */
@Named("orient")
@Priority(Integer.MAX_VALUE)
@ManagedLifecycle(phase = SCHEMAS)
@Singleton
public class OrientSelectorConfigurationStore
    extends StateGuardLifecycleSupport
    implements SelectorConfigurationStore
{
  private final Provider<DatabaseInstance> databaseInstance;

  private final OrientSelectorConfigurationEntityAdapter entityAdapter;

  @Inject
  public OrientSelectorConfigurationStore(@Named(DatabaseInstanceNames.CONFIG) final Provider<DatabaseInstance> databaseInstance,
                                          final OrientSelectorConfigurationEntityAdapter entityAdapter)
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
    return inTx(databaseInstance).call(db -> ImmutableList.copyOf(entityAdapter.browse(db)));
  }

  @Override
  @Guarded(by = STARTED)
  public SelectorConfiguration read(final EntityId entityId) {
    checkNotNull(entityId);

    return inTx(databaseInstance).call(db -> entityAdapter.read(db, entityId));
  }

  @Override
  @Guarded(by = STARTED)
  public SelectorConfiguration getByName(final String name) {
    checkNotNull(name);

    return inTx(databaseInstance).call(db -> entityAdapter.getByName(db, name));
  }

  @Override
  @Guarded(by = STARTED)
  public void create(final SelectorConfiguration configuration) {
    checkNotNull(configuration);
    checkArgument(configuration instanceof OrientSelectorConfiguration,
        "Configuration is not an OrientSelectorConfiguration");

    inTxRetry(databaseInstance).run(db -> entityAdapter.addEntity(db, (OrientSelectorConfiguration) configuration));
  }

  @Override
  @Guarded(by = STARTED)
  public void update(final SelectorConfiguration configuration) {
    checkNotNull(configuration);
    checkArgument(configuration instanceof OrientSelectorConfiguration,
        "Configuration is not an OrientSelectorConfiguration");

    inTxRetry(databaseInstance).run(db -> entityAdapter.editEntity(db, (OrientSelectorConfiguration) configuration));
  }

  @Override
  @Guarded(by = STARTED)
  public void delete(final SelectorConfiguration configuration) {
    checkNotNull(configuration);
    checkArgument(configuration instanceof OrientSelectorConfiguration,
        "Configuration is not an OrientSelectorConfiguration");

    inTxRetry(databaseInstance).run(db -> entityAdapter.deleteEntity(db, (OrientSelectorConfiguration) configuration));
  }

  @Override
  public SelectorConfiguration newSelectorConfiguration() {
    return entityAdapter.newEntity();
  }
}
