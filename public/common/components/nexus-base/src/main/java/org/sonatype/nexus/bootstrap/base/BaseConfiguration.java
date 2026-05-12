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
package org.sonatype.nexus.bootstrap.base;

import org.sonatype.nexus.security.FilterChain;
import org.sonatype.nexus.security.JwtFilter;
import org.sonatype.nexus.security.anonymous.AnonymousFilter;
import org.sonatype.nexus.security.authc.AntiCsrfFilter;

import com.codahale.metrics.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import static org.sonatype.nexus.common.app.FeatureFlags.JWT_ENABLED;
import static org.sonatype.nexus.common.app.FeatureFlags.SESSION_ENABLED;

/**
 * Web resources module. Both servlet and filter-chain are installed with the lowest priority.
 */
@Configuration
public class BaseConfiguration
{

  @Bean("nexus-uber")
  public ClassLoader uberClassLoader() {
    return BaseConfiguration.class.getClassLoader();
  }

  @Bean
  public com.codahale.metrics.Clock clock() {
    return Clock.defaultClock();
  }

  @Order(Ordered.LOWEST_PRECEDENCE)
  @ConditionalOnProperty(value = SESSION_ENABLED, havingValue = "true", matchIfMissing = true)
  @Bean
  public FilterChain primaryFilterChain() {
    return new FilterChain("/**", AnonymousFilter.NAME, AntiCsrfFilter.NAME);
  }

  @Order(Ordered.LOWEST_PRECEDENCE)
  @ConditionalOnProperty(value = JWT_ENABLED, havingValue = "true")
  @Bean
  public FilterChain primaryFilterChain_jwt() {
    return new FilterChain("/**", JwtFilter.NAME, AnonymousFilter.NAME, AntiCsrfFilter.NAME);
  }
}
