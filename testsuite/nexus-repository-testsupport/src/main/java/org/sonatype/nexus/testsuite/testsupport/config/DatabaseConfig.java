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
package org.sonatype.nexus.testsuite.testsupport.config;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_ENABLED_NAMED;

@Named
@Singleton
public class DatabaseConfig
{
  private boolean datastoreEnabled;

  private String jdbcUrl;

  @Inject
  public DatabaseConfig(
      @Named(DATASTORE_ENABLED_NAMED) final boolean datastoreEnabled,
      @Nullable @Named("nexus.datastore.nexus.jdbcUrl") final String jdbcUrl)
  {
    this.datastoreEnabled = datastoreEnabled;
    this.jdbcUrl = jdbcUrl;
  }

  public boolean isOrient() {
    return !datastoreEnabled;
  }

  public boolean isH2() {
    return datastoreEnabled && (!isPostgresql());
  }

  public boolean isPostgresql() {
    return datastoreEnabled && jdbcUrl != null && jdbcUrl.startsWith("postgresql:");
  }
}
