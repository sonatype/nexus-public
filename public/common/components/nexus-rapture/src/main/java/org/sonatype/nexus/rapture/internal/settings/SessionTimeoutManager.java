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
package org.sonatype.nexus.rapture.internal.settings;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.rapture.settings.RaptureSettings;
import org.sonatype.nexus.security.JwtHelper;

import com.google.common.eventbus.Subscribe;
import org.apache.shiro.nexus.NexusWebSessionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;

/**
 * Manages session timeout configuration and applies it to JWT and session enforcement mechanisms.
 * Supports cluster-wide timeout updates via events and respects property file overrides.
 */
@Component
@ManagedLifecycle(phase = SERVICES)
public class SessionTimeoutManager
    extends StateGuardLifecycleSupport
    implements EventAware
{
  private final AtomicInteger timeoutMinutes = new AtomicInteger(RaptureSettings.DEFAULT_SESSION_TIMEOUT);

  private final AtomicBoolean propertyOverrideActive = new AtomicBoolean(false);

  private final Optional<JwtHelper> jwtHelper;

  private final Optional<NexusWebSessionManager> sessionManager;

  private final EventManager eventManager;

  @Autowired
  public SessionTimeoutManager(
      final Optional<JwtHelper> jwtHelper,
      final Optional<NexusWebSessionManager> sessionManager,
      final EventManager eventManager,
      @Value("${nexus.jwt.expiry:-1}") final int jwtExpiryProperty,
      @Value("${shiro.globalSessionTimeout:-1}") final long sessionTimeoutProperty)
  {
    this.jwtHelper = jwtHelper;
    this.sessionManager = sessionManager;
    this.eventManager = eventManager;

    // Detect property overrides - property ALWAYS wins for security
    if (jwtExpiryProperty != -1) {
      propertyOverrideActive.set(true);
      timeoutMinutes.set(jwtExpiryProperty / 60);
      log.info("Session timeout configured via nexus.jwt.expiry property: {} minutes", timeoutMinutes.get());
    }
    else if (sessionTimeoutProperty != -1) {
      propertyOverrideActive.set(true);
      timeoutMinutes.set((int) (sessionTimeoutProperty / 60000));
      log.info("Session timeout configured via shiro.globalSessionTimeout property: {} minutes", timeoutMinutes.get());
    }
  }

  /**
   * Updates session timeout and publishes cluster event.
   * Called when admin changes settings via UI capability.
   * Property files always take precedence for security reasons.
   *
   * @param minutes the new timeout in minutes
   */
  public synchronized void updateTimeout(final int minutes) {
    if (propertyOverrideActive.get()) {
      log.warn("Session timeout is configured via property file ({} minutes) and cannot be changed via UI. " +
          "To use UI configuration, remove the nexus.jwt.expiry or shiro.globalSessionTimeout property.",
          this.timeoutMinutes.get());
      return; // Early exit - property wins
    }

    if (minutes == 0) {
      log.warn("Session timeout set to 0 (sessions never expire). This may be a security risk.");
    }

    if (this.timeoutMinutes.get() != minutes) {
      log.info("Updating session timeout from {} to {} minutes", this.timeoutMinutes.get(), minutes);
      this.timeoutMinutes.set(minutes);
      applyToEnforcementMechanisms();

      // Publish cluster event so other nodes receive the change
      eventManager.post(new SessionTimeoutChangedEvent(minutes));
    }
  }

  /**
   * Handles remote session timeout changes from other cluster nodes.
   * Uses @Subscribe from Guava EventBus.
   *
   * @param event the session timeout changed event
   */
  @Subscribe
  public void on(final SessionTimeoutChangedEvent event) {
    if (event.isLocal()) {
      // Skip local events - we already applied the change above
      return;
    }

    // Remote event from another cluster node
    log.info("Received remote session timeout change from node {}: {} minutes",
        event.getRemoteNodeId(), event.getTimeoutMinutes());

    if (propertyOverrideActive.get()) {
      log.warn("Session timeout is configured via property file ({} minutes) and cannot be changed via cluster events. "
          +
          "To use UI configuration, remove the nexus.jwt.expiry or shiro.globalSessionTimeout property on all cluster nodes.",
          this.timeoutMinutes.get());
      return; // Early exit - property wins
    }

    synchronized (this) {
      this.timeoutMinutes.set(event.getTimeoutMinutes());
      applyToEnforcementMechanisms();
    }
  }

  private void applyToEnforcementMechanisms() {
    // JWT mode: convert minutes to seconds
    jwtHelper.ifPresent(helper -> {
      int minutes = timeoutMinutes.get();
      if (minutes == 0) {
        // Zero means no expiration - use very large value (1 year)
        int seconds = 31536000; // 365 days in seconds
        helper.setExpirySeconds(seconds);
        log.info("Updated JWT expiry to {} seconds (effectively no expiration)", seconds);
      }
      else {
        int seconds = minutes * 60;
        helper.setExpirySeconds(seconds);
        log.info("Updated JWT expiry to {} seconds", seconds);
      }
    });

    // Session mode: convert minutes to milliseconds
    sessionManager.ifPresent(manager -> {
      int minutes = timeoutMinutes.get();
      if (minutes == 0) {
        // Shiro interprets -1 as "never expire"
        manager.setGlobalSessionTimeout(-1L);
        log.info("Updated session timeout to -1 (never expires)");
      }
      else {
        long millis = minutes * 60L * 1000L;
        manager.setGlobalSessionTimeout(millis);
        log.info("Updated session timeout to {} ms", millis);
      }
    });
  }
}
