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
package org.sonatype.nexus.quartz.internal;

import java.sql.Connection;
import java.sql.SQLException;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.common.app.BindAsLifecycleSupport;
import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.app.FeatureFlags;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.node.NodeAccess;

import org.quartz.impl.jdbcjobstore.HSQLDBDelegate;
import org.quartz.impl.jdbcjobstore.JobStoreTX;
import org.quartz.impl.jdbcjobstore.PostgreSQLDelegate;
import org.quartz.impl.jdbcjobstore.StdJDBCDelegate;
import org.quartz.spi.JobStore;
import org.quartz.utils.ConnectionProvider;
import org.quartz.utils.DBConnectionManager;

import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SCHEMAS;

/**
 * {@link JobStore} implementation that uses JDBC.
 *
 * @since 3.19
 */
@FeatureFlag(name = "nexus.quartz.jobstore.jdbc")
@ManagedLifecycle(phase = SCHEMAS)
@Named
@Singleton
@Priority(Integer.MAX_VALUE)
public class JobStoreJdbcProvider
    extends LifecycleSupport
    implements Provider<JobStore>
{
  private static final String QUARTZ_DS = "quartzDS";

  private final ConnectionProvider connectionProvider;

  private final boolean datastoreClustered;

  private volatile JobStore jobStore;

  private final NodeAccess nodeAccess;

  @Inject
  public JobStoreJdbcProvider(
      final ConnectionProvider connectionProvider,
      final NodeAccess nodeAccess,
      @Named(FeatureFlags.DATASTORE_CLUSTERED_ENABLED_NAMED) final boolean datastoreClustered) {
    this.connectionProvider = connectionProvider;
    this.nodeAccess = nodeAccess;
    this.datastoreClustered = datastoreClustered;
  }

  @Override
  protected void doStop() {
    if (jobStore != null) {
      jobStore.shutdown();
      jobStore = null;
      try {
        connectionProvider.shutdown();
      }
      catch (SQLException e) {
        log.error("Error while stopping job store", e);
      }
    }
  }

  @Override
  public JobStore get() {
    JobStore localRef = jobStore;
    if (localRef == null) {
      synchronized (this) {
        localRef = jobStore;
        if (localRef == null) {
          jobStore = localRef = createJobStore();
        }
      }
    }
    return localRef;
  }

  private JobStore createJobStore() {
    try {
      connectionProvider.initialize();
      DBConnectionManager.getInstance().addConnectionProvider(QUARTZ_DS, connectionProvider);
      JobStoreTX delegate = new JobStoreTX();
      delegate.setDataSource(QUARTZ_DS);

      if (datastoreClustered) {
        log.info("Running Quartz in clustered mode");
        delegate.setIsClustered(true);
        delegate.setInstanceName(nodeAccess.getId());
        delegate.setInstanceId("AUTO");
      }

      delegate.setDriverDelegateClass(getDriverDelegateClass());
      return delegate;
    }
    catch (Exception e) {
      log.error("Unable create job store", e);
      return null;
    }
  }

  private String getDatabaseId() throws SQLException {
    try (Connection con = connectionProvider.getConnection()) {
      return con.getMetaData().getDatabaseProductName();
    }
  }

  private String getDriverDelegateClass() throws SQLException {
    switch(getDatabaseId()) {
      case "H2":
        return HSQLDBDelegate.class.getName();
      case "PostgreSQL":
        return PostgreSQLDelegate.class.getName();
      default:
        return StdJDBCDelegate.class.getName();
    }
  }

  /**
   * Provider implementations are not automatically exposed under additional interfaces.
   * This small module is a workaround to expose this provider as a (managed) lifecycle.
   */
  @FeatureFlag(name = "nexus.quartz.jobstore.jdbc")
  @Named
  private static class BindAsLifecycle // NOSONAR
      extends BindAsLifecycleSupport<JobStoreJdbcProvider>
  {
    // empty
  }
}
