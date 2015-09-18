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

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.io.DirSupport;

import com.google.common.base.Strings;
import com.google.common.io.Files;
import org.h2.Driver;
import org.h2.api.ErrorCode;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.util.JdbcUtils;
import org.quartz.utils.ConnectionProvider;

import static com.google.common.base.Preconditions.checkState;

/**
 * H2-based {@link ConnectionProvider}.
 */
@Singleton
@Named
public class H2ConnectionProvider
    extends ComponentSupport
    implements ConnectionProvider
{
  private static final String H2_SETTINGS_KEY = QuartzConstants.CONFIG_PREFIX + ".h2-settings";

  private static final String H2_DEFAULTS =
      ";FILE_LOCK=FS;MVCC=TRUE;QUERY_CACHE_SIZE=0;CACHE_SIZE=0;CACHE_TYPE=SOFT_LRU;LOCK_TIMEOUT=60000;MAX_MEMORY_ROWS=1000000";

  private static final String H2_SETTINGS =
      "${" + H2_SETTINGS_KEY + ":-" + H2_DEFAULTS + "}";

  private static final String DATABASE_NAME = "quartz";

  private final ApplicationDirectories cfg;

  private final String settings;

  private final int poolSize;

  private JdbcConnectionPool pool;

  @Inject
  public H2ConnectionProvider(final ApplicationDirectories cfg,
                              final @Named(H2_SETTINGS) String settings,
                              final @Named(QuartzSupportImpl.QUARTZ_POOL_SIZE) int threadPoolSize)
  {
    this.cfg = cfg;
    this.settings = settings;
    // QZ recommendation: threadPoolSize + 2 is the possible max connection count needed
    this.poolSize = threadPoolSize + 2;
  }

  @Override
  public Connection getConnection() throws SQLException {
    checkState(pool != null, "Pool not initialized!");
    return pool.getConnection();
  }

  @Override
  public void initialize() throws SQLException {
    if (null != pool) {
      return; // already started
    }
    final File workDir = new File(cfg.getWorkDirectory("db"), DATABASE_NAME).getAbsoluteFile();
    final String database = workDir + File.separator + DATABASE_NAME;
    final File databaseFile = new File(database + ".h2.db");
    try {
      try {
        DirSupport.mkdir(workDir.toPath());
        pool = open(database, settings, poolSize);
      }
      catch (final SQLException e) {
        if (e.getErrorCode() == ErrorCode.DATABASE_ALREADY_OPEN_1 ||
            Strings.nullToEmpty(e.getMessage()).contains("Database may be already in use")) {
          log.warn("Database locked, is another instance of Nexus running? ({})", e.getMessage());
          final File backup = new File(workDir, DATABASE_NAME + ".backup." + System.currentTimeMillis());
          if (databaseFile.exists()) {
            log.info("Backing up locked database to {} to limit potential corruption", backup);
            Files.move(databaseFile, backup);
            Files.copy(backup, databaseFile);
          }
          final File lockFile = new File(database + ".lock.db");
          if (lockFile.exists()) {
            Files.move(lockFile, new File(backup.getPath() + ".lock"));
          }
          pool = open(database, settings, poolSize);
        }
        else {
          throw e; // not locked, re-throw as DB is probably corrupt...
        }
      }
    }
    catch (final SQLException e) {
      log.warn("Database corrupt, starting with empty Quartz DB ({})", e.getMessage());
      try {
        final File backup = new File(workDir, DATABASE_NAME + ".corrupt." + System.currentTimeMillis());
        log.info("Moving corrupt database to {}", backup);
        Files.move(databaseFile, backup);
        pool = open(database, settings, poolSize);
      }
      catch (final Exception ignore) {
        throw new SQLException("Problem rebooting Quartz DB", e);
      }
    }
    catch (final Exception e) {
      throw new SQLException("Problem starting Quartz DB", e);
    }
  }

  @Override
  public void shutdown() throws SQLException {
    if (null != pool) {
      try {
        pool.dispose();
      }
      finally {
        pool = null;
        Driver.unload();
      }
    }
  }

  private static JdbcConnectionPool open(final String database, final String settings, final int poolSize)
      throws Exception
  {
    JdbcConnectionPool pool = null;
    try {
      Driver.load();
      final String url = "jdbc:h2:" + database + (settings.startsWith(";") ? settings : ';' + settings);
      Connection conn = null;
      try {
        // eagerly test the connection
        conn = DriverManager.getConnection(url, "sa", "");
      }
      finally {
        JdbcUtils.closeSilently(conn);
      }
      // connection checks out, so now create lazy pool
      pool = JdbcConnectionPool.create(url, "sa", "");
      pool.setMaxConnections(poolSize);
      return pool;
    }
    catch (final Exception e) {
      if (null != pool) {
        pool.dispose();
      }
      throw e;
    }
  }
}