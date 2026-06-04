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

import java.util.Optional;
import javax.annotation.Nullable;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.upgrade.UpgradeService;
import org.sonatype.nexus.upgrade.datastore.DeploymentValidator;
import org.sonatype.nexus.upgrade.datastore.UpgradeManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.CLUSTERED_ZERO_DOWNTIME_ENABLED;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.UPGRADE;

/**
 * Default datastore {@link UpgradeService}.
 *
 * @since 3.29
 */
@Component
@ConditionalOnProperty(name = CLUSTERED_ZERO_DOWNTIME_ENABLED, havingValue = "false", matchIfMissing = true)
@Order(Ordered.HIGHEST_PRECEDENCE)
@ManagedLifecycle(phase = UPGRADE)
public class UpgradeServiceImpl
    extends StateGuardLifecycleSupport
    implements UpgradeService
{
  private final Optional<DeploymentValidator> deploymentValidator;

  private final UpgradeManager upgradeManager;

  @Autowired
  public UpgradeServiceImpl(
      @Nullable final DeploymentValidator deploymentValidator,
      final UpgradeManager upgradeManager)
  {
    this.upgradeManager = checkNotNull(upgradeManager);
    this.deploymentValidator = Optional.ofNullable(deploymentValidator);
  }

  @Override
  protected void doStart() throws Exception {
    deploymentValidator.ifPresent(DeploymentValidator::validate);

    upgradeManager.migrate();
  }
}
