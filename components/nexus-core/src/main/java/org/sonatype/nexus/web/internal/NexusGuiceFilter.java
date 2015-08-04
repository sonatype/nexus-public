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
package org.sonatype.nexus.web.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.google.inject.servlet.FilterPipeline;
import com.google.inject.servlet.GuiceFilter;

public final class NexusGuiceFilter
    extends GuiceFilter
{
  /*
   * Guice @Inject instead of JSR330 so Resin/CDI won't try to inject this and fail!
   */
  @com.google.inject.Inject
  static List<FilterPipeline> pipelines = Collections.emptyList();

  public NexusGuiceFilter() {
    super(new MultiFilterPipeline());
  }

  static final class MultiFilterPipeline
      implements FilterPipeline
  {
    public void initPipeline(ServletContext context)
        throws ServletException
    {
      for (final FilterPipeline p : pipelines) {
        p.initPipeline(context);
      }
    }

    public void dispatch(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException
    {
      new MultiFilterChain(chain).doFilter(request, response);
    }

    public void destroyPipeline() {
      for (final FilterPipeline p : pipelines) {
        p.destroyPipeline();
      }
    }
  }

  static final class MultiFilterChain
      implements FilterChain
  {
    private final Iterator<FilterPipeline> itr;

    private final FilterChain defaultChain;

    MultiFilterChain(final FilterChain chain) {
      itr = pipelines.iterator();
      defaultChain = chain;
    }

    public void doFilter(final ServletRequest request, final ServletResponse response)
        throws IOException, ServletException
    {
      if (itr.hasNext()) {
        itr.next().dispatch(request, response, this);
      }
      else {
        defaultChain.doFilter(request, response);
      }
    }
  }
}
