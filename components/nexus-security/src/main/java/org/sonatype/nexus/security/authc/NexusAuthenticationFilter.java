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
package org.sonatype.nexus.security.authc;

import java.util.List;

import javax.inject.Inject;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.google.common.collect.Lists;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;

import static com.google.common.base.Preconditions.checkNotNull;

// FIXME: Rename this class, unsure what but its confusing asis
// FIXME: Also like api-key filter, this isn't basic-auth, but is extending from that base

/**
 * {@link AuthenticatingFilter} that delegates token creation to {@link AuthenticationTokenFactory}s before falling
 * back to {@link BasicHttpAuthenticationFilter}.
 *
 * e.g. {@link AuthenticationTokenFactory} that will lookup REMOTE_USER HTTP header
 *
 * @since 2.7
 */
public class NexusAuthenticationFilter
    extends NexusBasicHttpAuthenticationFilter
{
  private List<AuthenticationTokenFactory> factories = Lists.newArrayList();

  @Inject
  public void install(List<AuthenticationTokenFactory> factories) {
    this.factories = checkNotNull(factories);
  }

  /**
   * Will consider an login attempt if any of the factories is able to create an authentication token.
   *
   * Otherwise will fallback to {@link BasicHttpAuthenticationFilter#isLoginAttempt(ServletRequest, ServletResponse)}
   */
  @Override
  protected boolean isLoginAttempt(ServletRequest request, ServletResponse response) {
    AuthenticationToken token = createAuthenticationToken(request, response);
    return token != null || super.isLoginAttempt(request, response);
  }

  /**
   * Will cycle configured factories for an authentication token. First one that will return a non null one will win.
   * If none of them will return an authentication token will fallback to
   * {@link BasicHttpAuthenticationFilter#createToken(ServletRequest, ServletResponse)}
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
          log.debug("Token '{}' created by {}", token, factory);
          return token;
        }
      }
      catch (Exception e) {
        log.warn(
            "Factory {} failed to create an authentication token {}/{}",
            factory, e.getClass().getName(), e.getMessage(),
            log.isDebugEnabled() ? e : null
        );
      }
    }
    return null;
  }
}
