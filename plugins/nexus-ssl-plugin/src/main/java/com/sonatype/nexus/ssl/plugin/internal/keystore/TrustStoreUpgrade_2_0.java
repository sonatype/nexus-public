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
import org.sonatype.nexus.common.upgrade.DependsOn;
import org.sonatype.nexus.common.upgrade.Upgrade;
import org.sonatype.nexus.common.upgrade.Upgrades;
import org.sonatype.nexus.orient.DatabaseInstanceNames;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Upgrades the trust store from its file-based storage to the database.
 * 
 * @since 3.1
 */
@Named
@Singleton
@Upgrades(model = TrustStoreCheckpoint.MODEL, from = "1.0", to = "2.0")
@DependsOn(model = DatabaseInstanceNames.CONFIG, version = "1.1")
public class TrustStoreUpgrade_2_0 // NOSONAR
    extends ComponentSupport
    implements Upgrade
{
  private final LegacyKeyStoreUpgradeService upgradeService;

  @Inject
  public TrustStoreUpgrade_2_0(final LegacyKeyStoreUpgradeService upgradeService) {
    this.upgradeService = checkNotNull(upgradeService);
  }

  @Override
  public void apply() throws Exception {
    upgradeService.importKeyStoreFiles();
  }
}
