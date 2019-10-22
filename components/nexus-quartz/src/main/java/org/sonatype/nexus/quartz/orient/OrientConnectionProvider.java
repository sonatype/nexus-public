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
package org.sonatype.nexus.quartz.orient;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import com.orientechnologies.orient.jdbc.OrientJdbcConnection;
import org.quartz.utils.ConnectionProvider;

/**
 * An OrientDB specific {@link ConnectionProvider} provider implemenation.
 *
 * This class will typically be used via the Quartz {@code org.quartz.jobStore.driverDelegateClass}
 * configuration property.
 *
 * @since 3.19
 */
public class OrientConnectionProvider
    implements ConnectionProvider
{
  private String connectionString = "plocal:./quartz";

  private String user = "admin";

  private String password = "admin";

  private boolean usePool = true;

  private int poolMin = 3;

  private int poolMax = 30;

  private Properties info;

  @Override
  public Connection getConnection() throws SQLException {
    return new OrientJdbcConnection("jdbc:orient:" + connectionString, info);
  }

  @Override
  public void shutdown() throws SQLException {
    info = null;
  }

  @Override
  public void initialize() throws SQLException {
    info = new Properties();
    info.put("user", user);
    info.put("password", password);
    info.put("db.usePool", Boolean.toString(usePool));
    info.put("db.pool.min", Integer.toString(poolMin));
    info.put("db.pool.max", Integer.toString(poolMax));
  }

  public String getConnectionString() {
    return connectionString;
  }

  public void setConnectionString(final String path) {
    this.connectionString = path;
  }

  public String getUser() {
    return user;
  }

  public void setUser(final String user) {
    this.user = user;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(final String password) {
    this.password = password;
  }

  public boolean isUsePool() {
    return usePool;
  }

  public void setUsePool(final boolean usePool) {
    this.usePool = usePool;
  }

  public int getPoolMin() {
    return poolMin;
  }

  public void setPoolMin(final int poolMin) {
    this.poolMin = poolMin;
  }

  public int getPoolMax() {
    return poolMax;
  }

  public void setPoolMax(final int poolMax) {
    this.poolMax = poolMax;
  }
}
