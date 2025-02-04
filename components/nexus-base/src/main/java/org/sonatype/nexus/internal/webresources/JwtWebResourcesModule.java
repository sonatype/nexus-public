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
package org.sonatype.nexus.internal.webresources;

import javax.inject.Named;

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.security.FilterChainModule;
import org.sonatype.nexus.security.JwtFilter;
import org.sonatype.nexus.security.JwtSecurityFilter;
import org.sonatype.nexus.security.anonymous.AnonymousFilter;
import org.sonatype.nexus.security.authc.AntiCsrfFilter;

import com.google.inject.Binder;
import com.google.inject.servlet.ServletModule;
import org.eclipse.sisu.inject.Sources;

import static org.sonatype.nexus.common.app.FeatureFlags.JWT_ENABLED;

/**
 * Web resources module using {@link JwtSecurityFilter}. Both servlet and filter-chain are installed with the lowest
 * priority.
 *
 * @since 3.38
 */
@Named
@FeatureFlag(name = JWT_ENABLED)
public class JwtWebResourcesModule
    extends WebResourcesModule
{
  @Override
  protected void configure() {
    final Binder lowPriorityBinder = binder().withSource(Sources.prioritize(Integer.MIN_VALUE));

    lowPriorityBinder.install(new ServletModule()
    {
      @Override
      protected void configureServlets() {
        serve("/*").with(WebResourceServlet.class);
        filter("/*").through(JwtSecurityFilter.class);
      }
    });

    lowPriorityBinder.install(new FilterChainModule()
    {
      @Override
      protected void configure() {
        addFilterChain("/**", JwtFilter.NAME, AnonymousFilter.NAME, AntiCsrfFilter.NAME);
      }
    });
  }
}
