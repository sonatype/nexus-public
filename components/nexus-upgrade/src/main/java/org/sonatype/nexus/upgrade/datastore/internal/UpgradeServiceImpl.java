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
package org.sonatype.nexus.upgrade.datastore.internal;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.app.FeatureFlags;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.upgrade.UpgradeService;
import org.sonatype.nexus.upgrade.datastore.UpgradeManager;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.UPGRADE;

/**
 * Default datastore {@link UpgradeService}.
 *
 * @since 3.29
 */
@Named
@FeatureFlag(name = FeatureFlags.DATASTORE_CLUSTERED_ENABLED, inverse = true, enabledByDefault = true)
@Priority(Integer.MAX_VALUE)
@ManagedLifecycle(phase = UPGRADE)
@Singleton
public class UpgradeServiceImpl
    extends StateGuardLifecycleSupport
    implements UpgradeService
{
  private final UpgradeManager upgradeManager;

  @Inject
  public UpgradeServiceImpl(final UpgradeManager upgradeManager) {
    this.upgradeManager = checkNotNull(upgradeManager);
  }

  @Override
  protected void doStart() throws Exception {
    upgradeManager.migrate();
  }
}
