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
package org.sonatype.nexus.bootstrap.siesta;

import org.sonatype.nexus.security.FilterChain;
import org.sonatype.nexus.security.JwtFilter;
import org.sonatype.nexus.security.anonymous.AnonymousFilter;
import org.sonatype.nexus.security.authc.AntiCsrfFilter;
import org.sonatype.nexus.security.authc.NexusAuthenticationFilter;

// NEXUS-46395: ResteasyDeployment and ResteasyProviderFactory both became abstract in
// RESTEasy 7. Use the concrete -Impl classes (ResteasyDeploymentImpl /
// LocalResteasyProviderFactory) or the static factory ResteasyProviderFactory.newInstance()
// for instantiation. We use the impl classes here — they are the documented entry points
// for embedded use.
import org.jboss.resteasy.core.ResteasyDeploymentImpl;
import org.jboss.resteasy.core.providerfactory.ResteasyProviderFactoryImpl;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.sonatype.nexus.common.app.FeatureFlags.JWT_ENABLED;
import static org.sonatype.nexus.common.app.FeatureFlags.SESSION_ENABLED;

@Configuration
public class SiestaConfiguration
{
  public static final String MOUNT_POINT = "/service/rest/**";

  @Bean
  public ResteasyDeployment resteasyDeployment() {
    // NEXUS-46395: switched from `new ResteasyDeployment()` / `new ResteasyProviderFactory()`
    // (both abstract in RESTEasy 7) to their concrete impl classes.
    ResteasyDeployment deployment = new ResteasyDeploymentImpl();
    deployment.setProviderFactory(new ResteasyProviderFactoryImpl());
    return deployment;
  }

  // No-auth health probe: skip bcrypt realm cost on every liveness/readiness check.
  // Intentionally unconditional — probe must bypass auth in all auth modes (session and JWT).
  @Bean
  public FilterChain statusWritableFilterChain() {
    return new FilterChain("/service/rest/v1/status/writable",
        AnonymousFilter.NAME,
        AntiCsrfFilter.NAME);
  }

  @ConditionalOnProperty(value = SESSION_ENABLED, havingValue = "true", matchIfMissing = true)
  @Bean
  public FilterChain siestaFilterChain() {
    return new FilterChain(MOUNT_POINT,
        NexusAuthenticationFilter.NAME,
        AnonymousFilter.NAME,
        AntiCsrfFilter.NAME);
  }

  @ConditionalOnProperty(value = JWT_ENABLED, havingValue = "true")
  @Bean
  public FilterChain siestaFilterChain_jwt() {
    return new FilterChain(MOUNT_POINT,
        NexusAuthenticationFilter.NAME,
        JwtFilter.NAME,
        AnonymousFilter.NAME,
        AntiCsrfFilter.NAME);
  }
}
