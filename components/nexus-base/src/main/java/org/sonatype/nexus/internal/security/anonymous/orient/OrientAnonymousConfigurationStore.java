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
package org.sonatype.nexus.internal.security.anonymous.orient;

import javax.annotation.Nullable;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.internal.security.anonymous.AnonymousConfigurationStore;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.security.anonymous.AnonymousConfiguration;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SCHEMAS;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTx;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTxRetry;

/**
 * Orient {@link AnonymousConfigurationStore}.
 *
 * @since 3.0
 */
@Named("orient")
@Priority(Integer.MAX_VALUE)
@ManagedLifecycle(phase = SCHEMAS)
@Singleton
public class OrientAnonymousConfigurationStore
  extends StateGuardLifecycleSupport
  implements AnonymousConfigurationStore
{
  private final Provider<DatabaseInstance> databaseInstance;

  private final OrientAnonymousConfigurationEntityAdapter entityAdapter;

  @Inject
  public OrientAnonymousConfigurationStore(@Named(DatabaseInstanceNames.SECURITY) final Provider<DatabaseInstance> databaseInstance,
                                           final OrientAnonymousConfigurationEntityAdapter entityAdapter)
  {
    this.databaseInstance = checkNotNull(databaseInstance);
    this.entityAdapter = checkNotNull(entityAdapter);
  }

  @Override
  protected void doStart() {
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      entityAdapter.register(db);
    }
  }

  @Override
  @Nullable
  @Guarded(by = STARTED)
  public AnonymousConfiguration load() {
    return inTx(databaseInstance).call(entityAdapter::get);
  }

  @Override
  @Guarded(by = STARTED)
  public void save(final AnonymousConfiguration configuration) {
    checkArgument(configuration instanceof OrientAnonymousConfiguration,
        "Anonymous configuration does not match backing store");
    inTxRetry(databaseInstance).run(db -> entityAdapter.set(db, (OrientAnonymousConfiguration) configuration));
  }

  @Override
  public AnonymousConfiguration newConfiguration() {
    return new OrientAnonymousConfiguration();
  }
}
