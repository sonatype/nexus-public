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
package org.sonatype.nexus.repository.internal.blobstore.upgrade;

import java.util.List;

import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.internal.blobstore.BlobStoreConfigurationDAO;
import org.sonatype.nexus.upgrade.datastore.UpgradeConfigStoreSupport;

import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.UPGRADE;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

/**
 * UPGRADE-phase-safe equivalent of {@code BlobStoreConfigurationStore} for the
 * {@code blob_store_configuration} table.
 *
 * <p>
 * {@code BlobStoreConfigurationStoreImpl} extends {@code ConfigStoreSupport} (which injects the
 * EVENTS-phase {@code EventManager}) and so cannot be injected into UPGRADE-phase migration steps. This
 * store reaches the same {@link BlobStoreConfigurationDAO} through a {@link DataSession} (reusing its SQL
 * and type handlers, so attributes are fully reconstructed) without that dependency.
 * </p>
 */
@Component
@ManagedLifecycle(phase = UPGRADE)
public final class UpgradeBlobStoreConfigurationStore
    extends UpgradeConfigStoreSupport
{
  @Autowired
  public UpgradeBlobStoreConfigurationStore(final DataSessionSupplier sessionSupplier) {
    super(sessionSupplier);
  }

  /**
   * Returns all stored blob store configurations.
   */
  public List<BlobStoreConfiguration> list() {
    try (DataSession<?> session = sessionSupplier.openSession(DEFAULT_DATASTORE_NAME)) {
      return ImmutableList.copyOf(session.access(BlobStoreConfigurationDAO.class).browse());
    }
  }
}
