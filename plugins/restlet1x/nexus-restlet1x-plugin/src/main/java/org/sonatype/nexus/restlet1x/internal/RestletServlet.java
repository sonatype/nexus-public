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
package org.sonatype.nexus.restlet1x.internal;

import java.io.EOFException;
import java.io.IOException;
import java.util.Enumeration;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.content.csp.ContentSecurityPolicy;
import org.sonatype.plexus.rest.PlexusServerServlet;
import org.sonatype.sisu.goodies.common.Throwables2;

import com.noelios.restlet.http.HttpServerConverter;
import com.noelios.restlet.http.HttpServerHelper;
import org.restlet.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An {@link PlexusServerServlet} that has an hardcoded name of "nexus" as required by plexus init param lookup.
 *
 * Guice servlet extension does not allow servlet name setup while binding.
 *
 * @author adreghiciu
 */
@Singleton
@Named
class RestletServlet
    extends PlexusServerServlet
{
  private static final long serialVersionUID = -840934203229475592L;

  private static final Logger log = LoggerFactory.getLogger(RestletServlet.class);

  /**
   * Original servlet context delegate.
   */
  private DelegatingServletConfig servletConfig;

  private final ContentSecurityPolicy contentSecurityPolicy;

  @Inject
  public RestletServlet(final ContentSecurityPolicy contentSecurityPolicy) {
    this.contentSecurityPolicy = contentSecurityPolicy;
    servletConfig = new DelegatingServletConfig();
  }

  @Override
  public void init() throws ServletException {
    final ClassLoader original = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
    try {
      super.init();
    }
    finally {
      Thread.currentThread().setContextClassLoader(original);
    }
  }

  @Override
  public void service(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException
  {
    checkNotNull(request);
    checkNotNull(response);
    contentSecurityPolicy.apply(request, response);

    // Log the request URI+URL muck
    String uri = request.getRequestURI();
    if (request.getQueryString() != null) {
      uri = String.format("%s?%s", uri, request.getQueryString());
    }

    if (log.isDebugEnabled()) {
      log.debug("Processing: {} {} ({})", request.getMethod(), uri, request.getRequestURL());
    }

    MDC.put(getClass().getName(), uri);
    try {
      super.service(request, response);
    }
    catch (EOFException e) {
      // special handling for EOF exceptions; tersely log unless debug is enabled
      if (log.isDebugEnabled()) {
        log.warn(e.toString(), e);
      }
      else {
        log.warn(Throwables2.explain(e));
      }
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
    finally {
      MDC.remove(getClass().getName());
    }
  }

  @Override
  public ServletConfig getServletConfig() {
    return servletConfig;
  }

  /**
   * An {@link ServletConfig} delegate that has an hardcoded servlet name.
   */
  private class DelegatingServletConfig
      implements ServletConfig
  {
    public String getServletName() {
      return "nexus";
    }

    public ServletContext getServletContext() {
      return RestletServlet.super.getServletConfig().getServletContext();
    }

    public String getInitParameter(String name) {
      return RestletServlet.super.getServletConfig().getInitParameter(name);
    }

    @SuppressWarnings("rawtypes")
    public Enumeration getInitParameterNames() {
      return RestletServlet.super.getServletConfig().getInitParameterNames();
    }
  }

  /**
   * Get a {@link Server} with a custom {@link HttpServerConverter}.
   *
   * @see NexusHttpServerConverter
   * @param request the request to get the server for
   * @return the {@link Server} from {@link super#getServer(HttpServletRequest)} with a custom
   * {@link HttpServerConverter}.
   */
  @Override
  public HttpServerHelper getServer(final HttpServletRequest request) {
    final HttpServerHelper server = super.getServer(request);
    server.setConverter(new NexusHttpServerConverter(server.getContext()));
    return server;
  }
}
