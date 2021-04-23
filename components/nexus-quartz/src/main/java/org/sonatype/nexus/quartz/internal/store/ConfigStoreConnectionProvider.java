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
package org.sonatype.nexus.quartz.internal.store;

import java.sql.Connection;
import java.sql.SQLException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.datastore.api.DataStore;

import org.quartz.utils.ConnectionProvider;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_ENABLED;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

/**
 * Supplies JDBC connections to Quartz from the shared config {@link DataStore}.
 *
 * @since 3.19
 */
@FeatureFlag(name = DATASTORE_ENABLED)
@Named
@Singleton
public class ConfigStoreConnectionProvider
    implements ConnectionProvider
{
  private final DataSessionSupplier sessionSupplier;

  @Inject
  public ConfigStoreConnectionProvider(final DataSessionSupplier sessionSupplier) {
    this.sessionSupplier = checkNotNull(sessionSupplier);
  }

  @Override
  public void initialize() {
    // no-op
  }

  @Override
  public Connection getConnection() throws SQLException {
    return sessionSupplier.openConnection(DEFAULT_DATASTORE_NAME);
  }

  @Override
  public void shutdown() {
    // no-op
  }
}
