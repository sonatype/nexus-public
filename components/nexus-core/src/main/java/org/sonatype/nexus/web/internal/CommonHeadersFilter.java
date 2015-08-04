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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.web.WebUtils;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Filter to set some "common" headers on response, instead littering this across
 * code in various servlets.
 * <p/>Note: this will unify response Server header, as some plugins, like Restlet1x does set it
 * already and some does not at all, like siesta. This filter will simply override it.
 *
 * @since 2.8
 */
@Named
@Singleton
public class CommonHeadersFilter
    implements Filter
{

  private final WebUtils webUtils;

  @Inject
  public CommonHeadersFilter(final WebUtils webUtils) {
    this.webUtils = checkNotNull(webUtils);
  }

  @Override
  public void init(final FilterConfig config) throws ServletException {
    // ignore
  }

  @Override
  public void destroy() {
    // ignore
  }

  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
      throws IOException, ServletException
  {
    // do it Before, as doFilter might commit response (and this even allows some code to customise std headers if needed)
    webUtils.equipResponseWithStandardHeaders((HttpServletResponse) response);
    chain.doFilter(request, response);
  }
}
