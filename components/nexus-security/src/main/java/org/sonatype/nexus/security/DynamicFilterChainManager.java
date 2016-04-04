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
package org.sonatype.nexus.security;

import java.util.Enumeration;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

import org.sonatype.goodies.common.Loggers;

import com.google.inject.Key;
import org.apache.shiro.web.filter.mgt.DefaultFilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainManager;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.Mediator;
import org.eclipse.sisu.inject.BeanLocator;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Dynamic {@link FilterChainManager} that reacts to {@link Filter}s and {@link FilterChain}s as they come and go.
 *
 * @since 3.0
 */
@Singleton
class DynamicFilterChainManager
    extends DefaultFilterChainManager
{
  private static final Logger log = Loggers.getLogger(DynamicFilterChainManager.class);

  private final List<FilterChain> filterChains;

  private volatile boolean refreshChains;

  @Inject
  public DynamicFilterChainManager(@Named("SHIRO") final ServletContext servletContext,
      final List<FilterChain> filterChains, final BeanLocator locator)
  {
    super(new DelegatingFilterConfig("SHIRO", checkNotNull(servletContext)));
    this.filterChains = checkNotNull(filterChains);

    // install the watchers for dynamic components contributed by other bundles
    locator.watch(Key.get(Filter.class, Named.class), new FilterInstaller(), this);
    locator.watch(Key.get(FilterChain.class), new FilterChainRefresher(), this);
  }

  @Override
  public boolean hasChains() {
    refreshChains();

    return super.hasChains();
  }

  /**
   * Regenerates the cached chain data based on the latest list of {@link FilterChain}s.
   */
  private void refreshChains() {
    if (refreshChains) { // only refresh once for the first request after any change
      synchronized (this) {
        if (refreshChains) {
          getChainNames().clear(); // completely replace old chains with latest list

          for (FilterChain filterChain : filterChains) {
            try {
              createChain(filterChain.getPathPattern(), filterChain.getFilterExpression());
            }
            catch (IllegalArgumentException e) {
              log.warn("Problem registering: {}", filterChain, e);
            }
          }

          refreshChains = false;
        }
      }
    }
  }

  /**
   * Simple {@link FilterConfig} that delegates to the surrounding {@link ServletContext}.
   */
  private static class DelegatingFilterConfig
      implements FilterConfig
  {
    private final String filterName;

    private final ServletContext servletContext;

    DelegatingFilterConfig(final String filterName, final ServletContext servletContext) {
      this.filterName = filterName;
      this.servletContext = servletContext;
    }

    @Override
    public String getFilterName() {
      return filterName;
    }

    @Override
    public ServletContext getServletContext() {
      return servletContext;
    }

    @Override
    public String getInitParameter(String name) {
      return servletContext.getInitParameter(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
      return servletContext.getInitParameterNames();
    }
  }

  /**
   * Watches for {@link Filter}s and registers them with the manager to be initialized.
   */
  private static class FilterInstaller
      implements Mediator<Named, Filter, DynamicFilterChainManager>
  {
    @Override
    public void add(BeanEntry<Named, Filter> entry, DynamicFilterChainManager manager) {
      manager.addFilter(entry.getKey().value(), entry.getValue(), true);
    }

    @Override
    public void remove(BeanEntry<Named, Filter> entry, DynamicFilterChainManager manager) {
      manager.getFilters().remove(entry.getKey().value());
    }
  }

  /**
   * Watches for {@link FilterChain}s and flags when the cached data needs refreshing.
   */
  private static class FilterChainRefresher
      implements Mediator<Named, FilterChain, DynamicFilterChainManager>
  {
    @Override
    public void add(BeanEntry<Named, FilterChain> entry, DynamicFilterChainManager manager) {
      manager.refreshChains = true;
    }

    @Override
    public void remove(BeanEntry<Named, FilterChain> entry, DynamicFilterChainManager manager) {
      manager.refreshChains = true;
    }
  }
}
