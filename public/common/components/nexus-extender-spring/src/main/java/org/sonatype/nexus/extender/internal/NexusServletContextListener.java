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
package org.sonatype.nexus.extender.internal;

import java.lang.management.ManagementFactory;
import java.util.EnumSet;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import org.sonatype.nexus.bootstrap.entrypoint.EditionVersionFormatter;
import org.sonatype.nexus.bootstrap.entrypoint.edition.NexusEditionSelector;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.common.app.ManagedLifecycle.Phase;
import org.sonatype.nexus.extender.NexusLifecycleManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.KERNEL;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.OFF;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;
import static org.sonatype.nexus.common.time.DateHelper.toDurationString;

@Lazy
@Component
public class NexusServletContextListener
    implements ServletContextListener
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private static final String NEXUS_LIFECYCLE_STARTUP_PHASE = "nexus.lifecycle.startupPhase";

  private final NexusEditionSelector nexusEditionSelector;

  private final NexusLifecycleManager nexusLifecycleManager;

  private final ApplicationVersion applicationVersion;

  private Phase startupPhase;

  @Autowired
  public NexusServletContextListener(
      final NexusEditionSelector nexusEditionSelector,
      final NexusLifecycleManager nexusLifecycleManager,
      @Autowired(required = false) final ApplicationVersion applicationVersion,
      @Value("${" + NEXUS_LIFECYCLE_STARTUP_PHASE + ":#{null}}") final String startupPhaseValue)
  {
    this.nexusLifecycleManager = checkNotNull(nexusLifecycleManager);
    this.nexusEditionSelector = checkNotNull(nexusEditionSelector);
    this.applicationVersion = applicationVersion; // may be null
    if (startupPhaseValue != null) {
      startupPhase = Phase.valueOf(startupPhaseValue);
    }
  }

  @Override
  public void contextInitialized(final ServletContextEvent event) {
    try {
      checkStartupPhase();
      // Push to the last phase in lifecycle, this will of course process each phase in between en route to TASKS phase
      moveToPhase(TASKS);
    }
    catch (final Exception e) {
      log.error("Failed to initialize context", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void contextDestroyed(final ServletContextEvent event) {
    // log uptime before triggering activity which may run into problems
    long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
    log.info("Uptime: {} ({})", toDurationString(uptime),
        EditionVersionFormatter.formatEditionAndVersion(nexusEditionSelector, applicationVersion));

    try {
      moveToPhase(OFF);
    }
    catch (final Exception e) {
      log.error("Failed to stop nexus", e);
    }
  }

  /**
   * Checks whether we should limit application startup to a particular lifecycle phase.
   */
  private void checkStartupPhase() {
    if (startupPhase != null) {
      log.info("Running lifecycle phases {}", EnumSet.range(KERNEL, startupPhase));
    }
    else {
      log.info("Running lifecycle phases {}", EnumSet.complementOf(EnumSet.of(OFF)));
    }
  }

  /**
   * Moves the application lifecycle on to a new phase.
   * <p>
   * When {@link #startupPhase} is set startup will never go past that phase.
   */
  private void moveToPhase(final Phase phase) throws Exception {
    if (startupPhase != null && phase.ordinal() > startupPhase.ordinal()) {
      nexusLifecycleManager.to(startupPhase); // this far, no further
    }
    else {
      nexusLifecycleManager.to(phase);
    }
  }
}
