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
package org.sonatype.nexus.guice;

import java.util.Map;

import javax.servlet.ServletContext;

import org.sonatype.nexus.web.TemplateRenderer;
import org.sonatype.nexus.web.WebResourceBundle;
import org.sonatype.nexus.web.internal.BaseUrlHolderFilter;
import org.sonatype.nexus.web.internal.CommonHeadersFilter;
import org.sonatype.nexus.web.internal.ErrorPageFilter;
import org.sonatype.nexus.web.internal.ErrorPageServlet;
import org.sonatype.security.SecuritySystem;
import org.sonatype.security.web.guice.SecurityWebModule;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.google.inject.servlet.ServletModule;
import com.palominolabs.metrics.guice.InstrumentationModule;
import org.apache.shiro.guice.aop.ShiroAopModule;
import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.eclipse.sisu.inject.DefaultRankingFunction;
import org.eclipse.sisu.inject.RankingFunction;
import org.eclipse.sisu.wire.ParameterKeys;
import org.osgi.framework.Bundle;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.inject.name.Names.named;

/**
 * Nexus guice modules.
 *
 * @since 2.4
 */
public class NexusModules
{
  /**
   * Nexus common guice module.
   */
  public static class CommonModule
      extends AbstractModule
  {
    @Override
    protected void configure() {
      install(new ShiroAopModule());
      install(new InstrumentationModule());
    }
  }

  /**
   * Nexus core guice module.
   */
  public static class CoreModule
      extends AbstractModule
  {
    private final ServletContext servletContext;

    private final Map<String, String> properties;

    private final Bundle systemBundle;

    public CoreModule(final ServletContext servletContext, final Map<String, String> properties, final Bundle systemBundle) {
      this.servletContext = checkNotNull(servletContext);
      this.properties = checkNotNull(properties);
      this.systemBundle = checkNotNull(systemBundle);
    }

    @Override
    protected void configure() {
      // HACK: Re-bind servlet-context instance with a name to avoid backwards-compat warnings from guice-servlet
      bind(ServletContext.class).annotatedWith(named("nexus")).toInstance(servletContext);
      bind(ParameterKeys.PROPERTIES).toInstance(properties);
      bind(Bundle.class).toInstance(systemBundle);

      install(new CommonModule());

      install(new ServletModule()
      {
        @Override
        protected void configureServlets() {
          filter("/*").through(BaseUrlHolderFilter.class);
          filter("/*").through(ErrorPageFilter.class);
          filter("/*").through(CommonHeadersFilter.class);

          serve("/error.html").with(ErrorPageServlet.class);

          // our configuration needs to be first-most when calculating order (some fudge room for edge-cases)
          bind(RankingFunction.class).toInstance(new DefaultRankingFunction(0x70000000));
        }
      });

      install(new SecurityWebModule(servletContext, true));

      // HACK: Disable CSRFGuard support for now, its too problematic
      //install(new CsrfGuardModule());
    }
  }

  /**
   * Nexus plugin guice module.
   */
  public static class PluginModule
      extends AbstractModule
  {
    @Override
    protected void configure() {
      install(new CommonModule());

      // handle some edge-cases for commonly used servlet-based components which need a bit more configuration
      // so that sisu/guice can find the correct bindings inside of plugins
      requireBinding(SecuritySystem.class);
      requireBinding(FilterChainResolver.class);
      requireBinding(TemplateRenderer.class);

      // eagerly initialize list of static web resources as soon as plugin starts (rather than on first request)
      bind(WebResourceBundle.class).annotatedWith(Names.named("static")).to(StaticWebResourceBundle.class).asEagerSingleton();
    }
  }
}
