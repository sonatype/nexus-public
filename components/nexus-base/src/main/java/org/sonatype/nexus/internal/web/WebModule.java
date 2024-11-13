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
package org.sonatype.nexus.internal.web;

import javax.inject.Named;

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.internal.metrics.MetricsModule;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.servlet.DynamicGuiceFilter;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.ServletModule;
import org.eclipse.sisu.inject.Sources;

import static org.sonatype.nexus.common.app.FeatureFlags.SESSION_ENABLED;

/**
 * Web module.
 * 
 * @since 3.0
 */
@Named
@FeatureFlag(name = SESSION_ENABLED)
public class WebModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    bind(GuiceFilter.class).to(DynamicGuiceFilter.class);

    // our configuration needs to be first-most when calculating order (some fudge room for edge-cases)
    final Binder highPriorityBinder = binder().withSource(Sources.prioritize(0x70000000));

    highPriorityBinder.install(new ServletModule()
    {
      @Override
      protected void configureServlets() {
        bind(HeaderPatternFilter.class);
        bind(EnvironmentFilter.class);
        bind(ErrorPageFilter.class);

        filter("/*").through(HeaderPatternFilter.class);
        filter("/*").through(EnvironmentFilter.class);
        filter("/*").through(ErrorPageFilter.class);

        bind(ErrorPageServlet.class);

        serve("/error.html").with(ErrorPageServlet.class);
        serve("/throw.html").with(ThrowServlet.class);
      }
    });

    installMetricsModule(highPriorityBinder);

  }

  protected void installMetricsModule(final Binder highPriorityBinder) {
    highPriorityBinder.install(new MetricsModule());
  }
}
