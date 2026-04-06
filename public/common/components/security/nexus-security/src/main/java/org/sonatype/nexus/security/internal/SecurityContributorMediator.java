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
package org.sonatype.nexus.security.internal;

import java.util.HashSet;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.security.config.SecurityConfigurationManager;
import org.sonatype.nexus.security.config.SecurityContributor;

import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SECURITY;

import org.springframework.stereotype.Component;

/**
 * Notifies {@link SecurityConfigurationManager} as {@link SecurityContributor}s come and go.
 *
 * Registers security contributors during the SECURITY lifecycle phase to ensure they are available
 * before the CAPABILITIES phase begins. This prevents validation errors when capabilities depend on
 * security roles or privileges that are provided by contributors.
 *
 * @since 3.1
 */
@Component
@Singleton
@ManagedLifecycle(phase = SECURITY)
public class SecurityContributorMediator
    extends StateGuardLifecycleSupport
{
  private final SecurityConfigurationManagerImpl securityConfigurationManagerImpl;

  private final ApplicationContext applicationContext;

  private final Set<SecurityContributor> registeredContributors = new HashSet<>();

  @Inject
  public SecurityContributorMediator(
      final SecurityConfigurationManagerImpl securityConfigurationManagerImpl,
      final ApplicationContext applicationContext)
  {
    this.securityConfigurationManagerImpl = checkNotNull(securityConfigurationManagerImpl);
    this.applicationContext = checkNotNull(applicationContext);
  }

  @Override
  protected void doStart() throws Exception {
    // Register all security contributors during the SECURITY phase, before CAPABILITIES phase starts
    log.debug("Registering security contributors during SECURITY phase");
    applicationContext.getBeansOfType(SecurityContributor.class)
        .values()
        .forEach(this::registerContributor);
  }

  @EventListener
  public void on(final ContextRefreshedEvent event) {
    // Keep event listener as fallback for any late-loading contributors
    // that may be added after startup (e.g., dynamically loaded plugins)
    event.getApplicationContext()
        .getBeansOfType(SecurityContributor.class)
        .values()
        .forEach(this::registerContributor);
  }

  private synchronized void registerContributor(final SecurityContributor contributor) {
    if (registeredContributors.add(contributor)) {
      log.debug("Registering security contributor: {}", contributor.getClass().getName());
      securityConfigurationManagerImpl.addContributor(contributor);
    }
    else {
      log.trace("Security contributor already registered: {}", contributor.getClass().getName());
    }
  }
}
