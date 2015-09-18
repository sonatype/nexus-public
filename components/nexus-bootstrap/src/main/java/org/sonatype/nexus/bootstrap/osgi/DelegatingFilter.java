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
package org.sonatype.nexus.bootstrap.osgi;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * Filter that delegates to a cached (static) instance populated by {@link FilterTracker}.
 * This helps us link the surrounding JEE/WAR environment with the dynamic OSGi container.
 * 
 * @since 3.0
 */
public final class DelegatingFilter
    implements Filter
{
  private static FilterConfig cachedConfig;

  private static volatile Filter delegate;

  public synchronized static void set(Filter filter) {
    if (cachedConfig != null) {
      try {
        filter.init(cachedConfig);
      }
      catch (ServletException e) {
        throw new IllegalStateException(e);
      }
    }
    delegate = filter;
  }

  public synchronized static void unset(Filter filter) {
    if (delegate == filter) {
      delegate = null;
    }
    if (cachedConfig != null) {
      filter.destroy();
    }
  }

  public void init(FilterConfig filterConfig) throws ServletException {
    synchronized (DelegatingFilter.class) {
      cachedConfig = filterConfig;
      if (delegate != null) {
        delegate.init(filterConfig);
      }
    }
  }

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
      ServletException
  {
    Filter filter = delegate;
    if (filter != null) {
      filter.doFilter(request, response, chain);
    }
    else if (response instanceof HttpServletResponse) {
      ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }
    else {
      chain.doFilter(request, response);
    }
  }

  public void destroy() {
    synchronized (DelegatingFilter.class) {
      cachedConfig = null;
      if (delegate != null) {
        delegate.destroy();
      }
    }
  }
}
