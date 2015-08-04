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
import java.security.Principal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.sonatype.security.SecuritySystem;
import org.sonatype.security.internal.UserIdMdcHelper;

import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.apache.shiro.web.servlet.AbstractShiroFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Web security filter.
 *
 * @since 2.8
 */
@Named
@Singleton
public class SecurityFilter
    extends AbstractShiroFilter
{
  private static final Logger log = LoggerFactory.getLogger(SecurityFilter.class);

  public static final String ATTR_USER_PRINCIPAL = "nexus.user.principal";

  public static final String ATTR_USER_ID = "nexus.user.id";

  @Inject
  public SecurityFilter(final SecuritySystem securitySystem,
                        final FilterChainResolver filterChainResolver)
  {
    checkNotNull(securitySystem);
    WebSecurityManager sm = (WebSecurityManager)securitySystem.getSecurityManager();
    log.trace("Security manager: {}", sm);
    setSecurityManager(sm);

    checkNotNull(filterChainResolver);
    log.trace("Filter chain resolver: {}", filterChainResolver);
    setFilterChainResolver(filterChainResolver);
  }

  /**
   * Sets MDC user-id attribute for request.
   */
  @Override
  protected void executeChain(final ServletRequest request,
                              final ServletResponse response,
                              final FilterChain origChain)
      throws IOException, ServletException
  {
    UserIdMdcHelper.set();

    // HACK: Attach principal to underlying request so we can use that in the request-log
    if (request instanceof HttpServletRequest) {
      HttpServletRequest httpRequest = (HttpServletRequest)request;
      Principal p = httpRequest.getUserPrincipal();
      if (p != null) {
        httpRequest.setAttribute(ATTR_USER_PRINCIPAL, p);
        httpRequest.setAttribute(ATTR_USER_ID, p.getName());
      }
    }

    try {
      super.executeChain(request, response, origChain);
    }
    finally {
      UserIdMdcHelper.unset();
    }
  }
}
