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
package org.sonatype.nexus.internal.status;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle.Phase;
import org.sonatype.nexus.common.app.ManagedLifecycleManager;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Health check that indicates if Nexus is in the correct (i.e. final) phase
 *
 * @since 3.16
 */
@Named("Lifecycle Phase")
@Singleton
public class LifecyclePhaseHealthCheck
    extends HealthCheckComponentSupport
{
  private final Phase finalPhase;

  private final ManagedLifecycleManager lifecycleManager;

  @Inject
  public LifecyclePhaseHealthCheck(final ManagedLifecycleManager lifecycleManager) {
    this.lifecycleManager = checkNotNull(lifecycleManager);
    finalPhase = Phase.values()[Phase.values().length - 1];
  }

  @Override
  protected Result check() {
    return finalPhase == lifecycleManager.getCurrentPhase() ? Result.healthy() : Result.unhealthy(unhealthyMsg());
  }

  private String unhealthyMsg() {
    return format("Nexus's current lifecycle phase is %s, but it should be %s",
        lifecycleManager.getCurrentPhase().name(), finalPhase.name());
  }

}
