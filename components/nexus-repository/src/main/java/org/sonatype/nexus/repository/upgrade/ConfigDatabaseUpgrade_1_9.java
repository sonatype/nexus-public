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
package org.sonatype.nexus.repository.upgrade;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.upgrade.Upgrades;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.orient.DatabaseUpgradeSupport;

import com.orientechnologies.orient.core.sql.OCommandSQL;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.orient.DatabaseInstanceNames.CONFIG;

/**
 * Update compact blobstore task to set multinode to false.
 *
 * @since 3.30
 */
@Named
@Singleton
@Upgrades(model = DatabaseInstanceNames.CONFIG, from = "1.8", to = "1.9")
public class ConfigDatabaseUpgrade_1_9 // NOSONAR
    extends DatabaseUpgradeSupport
{
  private static final String UPDATE_JOBS = "UPDATE quartz_job_detail "
      + "  SET value_data.jobDataMap.multinode = 'false' "
      + "WHERE value_data['jobDataMap']['.typeId'] = 'blobstore.compact'";

  private final Provider<DatabaseInstance> databaseInstance;

  @Inject
  public ConfigDatabaseUpgrade_1_9(@Named(CONFIG) final Provider<DatabaseInstance> databaseInstance)
  {
    this.databaseInstance = checkNotNull(databaseInstance);
  }

  @Override
  public void apply() {
    withDatabaseAndClass(databaseInstance, "quartz_job_detail", (db, type) ->
      db.command(new OCommandSQL(UPDATE_JOBS)).execute()
    );
  }
}
