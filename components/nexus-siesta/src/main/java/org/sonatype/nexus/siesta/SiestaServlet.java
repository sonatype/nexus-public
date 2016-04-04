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
package org.sonatype.nexus.siesta;

import java.io.IOException;
import java.lang.annotation.Annotation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.ext.RuntimeDelegate;

import org.sonatype.nexus.rest.Component;

import com.google.inject.Key;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.Mediator;
import org.eclipse.sisu.inject.BeanLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Siesta servlet.
 *
 * This is a thin wrapper around {@link ComponentContainer} which also handles {@link Component} registration.
 *
 * @since 3.0
 */
@Named
@Singleton
public class SiestaServlet
    extends HttpServlet
{
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final BeanLocator beanLocator;

  private final ComponentContainer componentContainer;

  @Inject
  public SiestaServlet(final BeanLocator beanLocator, final ComponentContainer componentContainer) {
    this.beanLocator = checkNotNull(beanLocator);
    this.componentContainer = checkNotNull(componentContainer);

    log.debug("Component container: {}", componentContainer);
  }

  @Override
  public void init(final ServletConfig config) throws ServletException {
    super.init(config);

    // TODO: Figure out what version of RESTEasy is used and log it

    // Initialize container
    componentContainer.init(config);
    log.info("JAX-RS RuntimeDelegate: {}", RuntimeDelegate.getInstance());

    // Watch for components
    beanLocator.watch(Key.get(Component.class), new ComponentMediator(), componentContainer);

    log.info("Initialized");
  }

  /**
   * Handles component [de]registration events.
   */
  private class ComponentMediator
      implements Mediator<Annotation, Component, ComponentContainer>
  {
    @Override
    public void add(final BeanEntry<Annotation, Component> entry, final ComponentContainer container) throws Exception {
      log.trace("Adding component: {}", entry.getKey());
      try {
        container.addComponent(entry);
      }
      catch (Exception e) {
        log.error("Failed to add component", e);
      }
    }

    @Override
    public void remove(final BeanEntry<Annotation, Component> entry, final ComponentContainer container)
        throws Exception
    {
      log.trace("Removing component: {}", entry.getKey());
      try {
        container.removeComponent(entry);
      }
      catch (Exception e) {
        log.error("Failed to remove component", e);
      }
    }
  }

  @Override
  public void service(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException
  {
    checkNotNull(request);
    checkNotNull(response);

    // Log the request URI+URL muck
    String uri = request.getRequestURI();
    if (request.getQueryString() != null) {
      uri = String.format("%s?%s", uri, request.getQueryString());
    }

    if (log.isDebugEnabled()) {
      log.debug("Processing: {} {} ({})", request.getMethod(), uri, request.getRequestURL());
    }

    if (log.isTraceEnabled()) {
      log.trace("Context path: {}", request.getContextPath());
      log.trace("Servlet path: {}", request.getServletPath());
    }

    MDC.put(getClass().getName(), uri);
    try {
      componentContainer.service(request, response);
    }
    finally {
      MDC.remove(getClass().getName());
    }
  }

  @Override
  public void destroy() {
    componentContainer.destroy();
    super.destroy();

    log.info("Destroyed");
  }
}
