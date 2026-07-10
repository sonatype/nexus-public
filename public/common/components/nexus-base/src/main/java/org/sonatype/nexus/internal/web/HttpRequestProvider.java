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

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;

import org.sonatype.nexus.common.app.WebFilterPriority;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * This class provides the HttpServletRequest for injection on the current thread as we are not using a Spring managed
 * Jetty
 */
@WebFilter("/*")
@Order(WebFilterPriority.SERVLET_FILTER)
@Component
public class HttpRequestProvider
    implements FactoryBean<HttpServletRequest>, Filter
{
  private final ThreadLocal<HttpServletRequest> currentRequest = new ThreadLocal<>();

  @Override
  public void doFilter(
      final ServletRequest request,
      final ServletResponse response,
      final FilterChain chain) throws IOException, ServletException
  {
    currentRequest.set((HttpServletRequest) request);
    try {
      chain.doFilter(request, response);
    }
    finally {
      currentRequest.set(null);
    }
  }

  @Override
  public HttpServletRequest getObject() throws Exception {
    return currentRequest.get();
  }

  @Override
  public Class<?> getObjectType() {
    return HttpServletRequest.class;
  }

  @Override
  public boolean isSingleton() {
    return false;
  }
}
