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
package com.sonatype.nexus.ssl.plugin.internal.keystore;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.upgrade.Upgrade;
import org.sonatype.nexus.common.upgrade.Upgrades;
import org.sonatype.nexus.orient.DatabaseInstanceNames;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Upgrades the config database to account for the new {@link KeyStoreData} entity. Note that this is separate from
 * {@link TrustStoreUpgrade_2_0} to ensure the checkpoint for the config database is run (e.g. performs backup/restore).
 * 
 * @since 3.1
 */
@Named
@Singleton
@Upgrades(model = DatabaseInstanceNames.CONFIG, from = "1.0", to = "1.1")
public class ConfigDatabaseUpgrade_1_1 // NOSONAR
    extends ComponentSupport
    implements Upgrade
{
  private final LegacyKeyStoreUpgradeService upgradeService;

  @Inject
  public ConfigDatabaseUpgrade_1_1(LegacyKeyStoreUpgradeService upgradeService) {
    this.upgradeService = checkNotNull(upgradeService);
  }

  @Override
  public void apply() throws Exception {
    upgradeService.upgradeSchema();
  }
}
