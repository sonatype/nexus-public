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
package org.sonatype.nexus.bootstrap.entrypoint;

import java.lang.management.ManagementFactory;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import org.sonatype.nexus.bootstrap.entrypoint.edition.NexusEditionSelector;
import org.sonatype.nexus.bootstrap.entrypoint.jvm.ShutdownDelegate;
import org.sonatype.nexus.bootstrap.spring.NexusComponentScanCompleteEvent;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.common.app.ManagedLifecycle.Phase;
import org.sonatype.nexus.extender.NexusLifecycleManager;

import com.codahale.metrics.SharedMetricRegistries;
import com.google.common.base.Strings;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.KERNEL;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.OFF;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;
import static org.sonatype.nexus.common.time.DateHelper.toDurationString;

/**
 * Waits for Jetty to start and component scan to complete before launching the nexus lifecycle.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class NexusLifecycleLauncher
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final NexusEditionSelector nexusEditionSelector;

  private final ShutdownDelegate shutdownDelegate;

  private final ApplicationVersion applicationVersion;

  private Phase startupPhase;

  private NexusLifecycleManager nexusLifecycleManager;

  public NexusLifecycleLauncher(
      final NexusEditionSelector nexusEditionSelector,
      final ShutdownDelegate shutdownDelegate,
      @Autowired(required = false) final ApplicationVersion applicationVersion,
      @Value("${nexus.lifecycle.startupPhase:#{null}}") final String startupPhaseName)
  {
    this.nexusEditionSelector = checkNotNull(nexusEditionSelector);
    this.shutdownDelegate = checkNotNull(shutdownDelegate);
    this.applicationVersion = applicationVersion; // may be null
    // set startup phase from properties, if available
    Optional.ofNullable(startupPhaseName)
        .filter(value -> !Strings.isNullOrEmpty(value))
        .map(Phase::valueOf)
        .ifPresent((phase -> startupPhase = phase));
  }

  @PreDestroy
  public void preDestroy() {
    SharedMetricRegistries.remove("nexus");
  }

  public void stop() throws Exception {
    long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
    log.info("Stopping Nexus, Uptime: {} ({})", toDurationString(uptime),
        EditionVersionFormatter.formatEditionAndVersion(nexusEditionSelector, applicationVersion));
    if (nexusLifecycleManager != null) {
      nexusLifecycleManager.to(Phase.OFF);
    }
  }

  @EventListener
  public void onNexusScanComplete(final NexusComponentScanCompleteEvent event) throws Exception {
    log.debug("Nexus component scan completed - edition ({})", nexusEditionSelector.getCurrent().getShortName());
    maybeLaunch((ApplicationContext) event.getSource());
  }

  private void maybeLaunch(final ApplicationContext context) throws Exception {
    logStartupPhase();
    // We use the event context to retrieve NexusLifecycleManager as it is from the second round of scanning
    nexusLifecycleManager = context.getBean(NexusLifecycleManager.class);

    if (nexusLifecycleManager == null) {
      log.debug("received NexusComponentScanCompleteEvent but no NexusLifecycleManager available");
      return;
    }
    try {
      // on startup we want to move to the TASKS phase
      moveToPhase(TASKS);
    }
    catch (Exception e) {
      log.error("Failed to move to phase TASKS: {}", e.getMessage(), e);
      shutdownDelegate.exit(1);
    }
  }

  /**
   * Log the startup phases that will be executed.
   */
  private void logStartupPhase() {
    Set<Phase> phases = EnumSet.complementOf(EnumSet.of(OFF));

    if (startupPhase != null) {
      phases = EnumSet.range(KERNEL, startupPhase);
    }

    log.info("Running lifecycle phases {}", phases);
  }

  /**
   * Move to the given phase, but only if it is not past the startup phase.
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
