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
package com.google.inject.servlet;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import org.eclipse.sisu.inject.BeanLocator;
import org.eclipse.sisu.wire.EntryListAdapter;

/**
 * Dynamic {@code ServletPipeline} that can update its sequence of servlet definitions on-demand.
 * Includes patched methods from {@link ManagedServletPipeline} where delegating isn't possible.
 */
@Singleton
// don't use @Named, keep as implicit JIT-binding
final class DynamicServletPipeline
    extends ManagedServletPipeline
{
  static final Injector DUMMY_INJECTOR = Guice.createInjector();

  // dynamic list of definitions
  private final List<ServletDefinition> servletDefinitions;

  // stable cache of definitions
  private volatile ServletDefinition[] servletDefinitionCache = {};

  @Inject
  DynamicServletPipeline(final BeanLocator locator) {
    super(DUMMY_INJECTOR);

    servletDefinitions = new EntryListAdapter<>(locator.locate(Key.get(ServletDefinition.class)));
  }

  public synchronized void refreshCache() {
    final Object[] snapshot = servletDefinitions.toArray();
    servletDefinitionCache = Arrays.copyOf(snapshot, snapshot.length, ServletDefinition[].class);
    PipelineLogger.dump(servletDefinitionCache);
  }

  @Override
  public boolean service(ServletRequest request, ServletResponse response) throws IOException, ServletException {
    for (ServletDefinition servletDefinition : servletDefinitions()) {
      if (servletDefinition.service(request, response)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void destroy() {
    Set<HttpServlet> destroyedSoFar = Sets.newIdentityHashSet();
    for (ServletDefinition servletDefinition : servletDefinitions()) {
      servletDefinition.destroy(destroyedSoFar);
    }
  }

  @Override
  RequestDispatcher getRequestDispatcher(final String path) {
    for (final ServletDefinition servletDefinition : servletDefinitions()) {
      if (servletDefinition.shouldServe(path)) {
        return new RequestDispatcher()
        {
          public void forward(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException,
              IOException
          {
            Preconditions.checkState(!servletResponse.isCommitted(),
                "Response has been committed--you can only call forward before"
                    + " committing the response (hint: don't flush buffers)");

            servletResponse.resetBuffer();

            ServletRequest requestToProcess = servletRequest instanceof HttpServletRequest ? wrapRequest(
                (HttpServletRequest) servletRequest, path) : servletRequest;

            doServiceImpl(requestToProcess, servletResponse);
          }

          public void include(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException,
              IOException
          {
            doServiceImpl(servletRequest, servletResponse);
          }

          private void doServiceImpl(ServletRequest servletRequest, ServletResponse servletResponse)
              throws ServletException, IOException
          {
            servletRequest.setAttribute(REQUEST_DISPATCHER_REQUEST, Boolean.TRUE);
            try {
              servletDefinition.doService(servletRequest, servletResponse);
            }
            finally {
              servletRequest.removeAttribute(REQUEST_DISPATCHER_REQUEST);
            }
          }
        };
      }
    }

    return null;
  }

  @Override
  protected boolean hasServletsMapped() {
    return servletDefinitionCache.length > 0;
  }

  private ServletDefinition[] servletDefinitions() {
    return servletDefinitionCache;
  }
}
