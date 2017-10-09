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
package org.sonatype.nexus.repository.storage;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.Lifecycle;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.orient.DatabaseInstance;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SCHEMAS;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * @since 3.6
 */
@Singleton
@ManagedLifecycle(phase = SCHEMAS)
@Named
public class BucketStoreImpl
    extends StateGuardLifecycleSupport
    implements BucketStore, Lifecycle
{
  private final Provider<DatabaseInstance> databaseInstance;

  private final BucketEntityAdapter entityAdapter;

  @Inject
  public BucketStoreImpl(@Named("component") final Provider<DatabaseInstance> databaseInstance,
                         final BucketEntityAdapter entityAdapter)
  {
    this.databaseInstance = checkNotNull(databaseInstance);
    this.entityAdapter = checkNotNull(entityAdapter);
  }

  @Override
  @Guarded(by = STARTED)
  public Bucket read(final String repositoryName)
  {
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      return entityAdapter.read(db, repositoryName);
    }
  }

  @Override
  public Bucket getById(final EntityId bucketId) {
    try (ODatabaseDocumentTx db = databaseInstance.get().acquire()) {
      return entityAdapter.read(db, bucketId);
    }
  }
}
