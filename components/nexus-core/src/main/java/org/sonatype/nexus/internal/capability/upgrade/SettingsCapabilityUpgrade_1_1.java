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
package org.sonatype.nexus.internal.capability.upgrade;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.upgrade.DependsOn;
import org.sonatype.nexus.common.upgrade.Upgrades;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.orient.DatabaseUpgradeSupport;

import com.orientechnologies.orient.core.sql.OCommandSQL;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Upgrade UI:Settings capability to set missing properties requestTimeout and longRequestTimeout.
 */
@Named
@Singleton
@Upgrades(model = "SettingsCapability", from = "1.0", to = "1.1")
@DependsOn(model = DatabaseInstanceNames.CONFIG, version = "1.9", checkpoint = true)
public class SettingsCapabilityUpgrade_1_1
    extends DatabaseUpgradeSupport
{
  public static final String DEFAULT_REQUEST_TIMEOUT = "60";

  public static final String DEFAULT_LONG_REQUEST_TIMEOUT = "180";

  private static final String sql =
      "UPDATE capability SET properties.requestTimeout = '%s', properties.longRequestTimeout = '%s' " +
          "WHERE type = 'rapture.settings' AND properties['requestTimeout'] IS NULL " +
          "AND properties['longRequestTimeout'] IS NULL";

  private final OCommandSQL updatePropertiesQuery =
      new OCommandSQL(String.format(sql, DEFAULT_REQUEST_TIMEOUT, DEFAULT_LONG_REQUEST_TIMEOUT));

  private final Provider<DatabaseInstance> configDatabaseInstance;

  @Inject
  public SettingsCapabilityUpgrade_1_1(
      @Named(DatabaseInstanceNames.CONFIG) final Provider<DatabaseInstance> configDatabaseInstance)
  {
    this.configDatabaseInstance = checkNotNull(configDatabaseInstance);
  }

  @Override
  public void apply() throws Exception {
    withDatabaseAndClass(configDatabaseInstance, "capability", (db, type) ->
        db.command(updatePropertiesQuery).execute()
    );
  }
}
