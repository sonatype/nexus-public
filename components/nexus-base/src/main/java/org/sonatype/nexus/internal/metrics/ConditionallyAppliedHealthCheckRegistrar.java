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
package org.sonatype.nexus.internal.metrics;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.systemchecks.ConditionallyAppliedHealthCheck;

import com.codahale.metrics.health.HealthCheckRegistry;
import org.eclipse.sisu.BeanEntry;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;

@Named
@Singleton
@ManagedLifecycle(phase = SERVICES)
public class ConditionallyAppliedHealthCheckRegistrar
    extends StateGuardLifecycleSupport
{
  private final HealthCheckRegistry healthCheckRegistry;

  private final Iterable<BeanEntry<Named, ConditionallyAppliedHealthCheck>> conditionallyAppliedHealthChecks;

  @Inject
  public ConditionallyAppliedHealthCheckRegistrar(
      final HealthCheckRegistry healthCheckRegistry,
      final Iterable<BeanEntry<Named, ConditionallyAppliedHealthCheck>> conditionallyAppliedHealthChecks)
  {
    this.healthCheckRegistry = checkNotNull(healthCheckRegistry);
    this.conditionallyAppliedHealthChecks = checkNotNull(conditionallyAppliedHealthChecks);
  }

  @Override
  protected void doStart() throws Exception {
    conditionallyAppliedHealthChecks.forEach(healthCheckBeanEntry -> {
      String name = healthCheckBeanEntry.getKey().value();
      ConditionallyAppliedHealthCheck check = healthCheckBeanEntry.getValue();
      if (check.shouldApply()) {
        healthCheckRegistry.register(name, check);
      }
    });
  }

  @Override
  protected void doStop() throws Exception {
    conditionallyAppliedHealthChecks.forEach(healthCheckBeanEntry -> {
      String name = healthCheckBeanEntry.getKey().value();
      healthCheckRegistry.unregister(name);
    });
  }
}
