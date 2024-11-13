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
package org.sonatype.nexus.internal.security.apikey.upgrade;

import java.sql.Connection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;
import org.sonatype.nexus.upgrade.datastore.DefinedUpgradeRound;
import org.sonatype.nexus.upgrade.datastore.RepeatableDatabaseMigrationStep;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An upgrade step which triggers the task to move from {@code api_key} to {@code api_key_v2}
 */
@Named
@Singleton
public class ApiKeySecretsDatabaseMigrationStep
    implements RepeatableDatabaseMigrationStep, DefinedUpgradeRound
{
  private final UpgradeTaskScheduler scheduler;

  @Inject
  public ApiKeySecretsDatabaseMigrationStep(final UpgradeTaskScheduler scheduler) {
    this.scheduler = checkNotNull(scheduler);
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    scheduler.schedule(scheduler.createTaskConfigurationInstance(ApiKeyToSecretsTask.TYPE_ID));
  }

  @Override
  public Integer getChecksum() {
    return 1;
  }

  @Override
  public int getUpgradeRound() {
    // We need to be run after UserTokenLdapSamlDualRealmUserTokensMigrationStep
    return 2;
  }
}
