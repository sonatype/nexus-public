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
package org.sonatype.nexus.security.filter.authc;

import java.util.List;

import javax.inject.Inject;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;

import static com.google.common.base.Preconditions.checkNotNull;

// HACK: Disable CSRFGuard support for now, its too problematic
//import org.sonatype.nexus.csrfguard.CsrfGuardFilter;

/**
 * {@link AuthenticatingFilter} that delegates token creation to {@link AuthenticationTokenFactory}s before falling
 * back to {@link NexusHttpAuthenticationFilter}.
 *
 * e.g. {@link AuthenticationTokenFactory} that will lookup REMOTE_USER HTTP header
 *
 * @since 2.7
 */
public class NexusAuthenticationFilter
    extends NexusHttpAuthenticationFilter
{

  private List<AuthenticationTokenFactory> factories = Lists.newArrayList();

  @Inject
  public void install(List<AuthenticationTokenFactory> factories) {
    this.factories = checkNotNull(factories);
  }

  @Override
  protected boolean isAccessAllowed(final ServletRequest request, final ServletResponse response,
                                    final Object mappedValue)
  {
    if (isLoginAttempt(request, response)) {
      // HACK: Disable CSRFGuard support for now, its too problematic
      //request.setAttribute(CsrfGuardFilter.SKIP_VALIDATION, Boolean.TRUE);
      try {
        return executeLogin(request, response) && super.isAccessAllowed(request, response, mappedValue);
      }
      catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }
    return super.isAccessAllowed(request, response, mappedValue);
  }

  /**
   * Will consider an login attempt if any of the factories is able to create an authentication token.
   *
   * Otherwise will fallback to {@link NexusHttpAuthenticationFilter#isLoginAttempt(ServletRequest, ServletResponse)}
   */
  @Override
  protected boolean isLoginAttempt(ServletRequest request, ServletResponse response) {
    AuthenticationToken token = createAuthenticationToken(request, response);
    return token != null || super.isLoginAttempt(request, response);
  }

  /**
   * Will cycle configured factories for an authentication token. First one that will return a non null one will win.
   * If none of them will return an authentication token will fallback to
   * {@link NexusHttpAuthenticationFilter#createToken(ServletRequest, ServletResponse)}
   */
  @Override
  protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) {
    AuthenticationToken token = createAuthenticationToken(request, response);
    if (token != null) {
      return token;
    }
    return super.createToken(request, response);
  }

  private AuthenticationToken createAuthenticationToken(ServletRequest request, ServletResponse response) {
    for (AuthenticationTokenFactory factory : factories) {
      try {
        AuthenticationToken token = factory.createToken(request, response);
        if (token != null) {
          getLogger().debug("Token '{}' created by {}", token, factory);
          return token;
        }
      }
      catch (Exception e) {
        getLogger().warn(
            "Factory {} failed to create an authentication token {}/{}",
            factory, e.getClass().getName(), e.getMessage(),
            getLogger().isDebugEnabled() ? e : null
        );
      }
    }
    return null;
  }

}
