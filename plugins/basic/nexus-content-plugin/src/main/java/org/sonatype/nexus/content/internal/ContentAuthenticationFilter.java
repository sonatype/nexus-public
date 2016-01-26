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
package org.sonatype.nexus.content.internal;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.sonatype.nexus.content.ContentRestrictionConstituent;
import org.sonatype.nexus.security.filter.authc.AuthenticationTokenFactory;
import org.sonatype.nexus.security.filter.authc.NexusHttpAuthenticationFilter;

import com.google.common.base.Throwables;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.web.filter.authc.AuthenticationFilter;

/**
 * Nexus {code}/content{code} {@link AuthenticationFilter}.
 *
 * @see ContentRestrictionConstituent
 * @see ContentRestrictedToken
 * @since 2.1
 */
public class ContentAuthenticationFilter
    extends NexusHttpAuthenticationFilter
{
  private final List<ContentRestrictionConstituent> constituents;

  private List<AuthenticationTokenFactory> factories;

  @Inject
  public ContentAuthenticationFilter(final @Nullable List<ContentRestrictionConstituent> constituents,
                                     final @Nullable List<AuthenticationTokenFactory> factories)
  {
    this.constituents = constituents;
    this.factories = factories;
    // Do not change this string, third-party clients depend on it
    setApplicationName("Sonatype Nexus Repository Manager");
  }

  /**
   * Determine if content restriction is enabled, by asking each constituent.
   * If any constituent reports a restriction then returns true.
   */
  private boolean isRestricted(final ServletRequest request) {
    if (constituents != null) {
      for (ContentRestrictionConstituent constituent : constituents) {
        if (constituent.isContentRestricted(request)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Servlet request attribute to mark request as protected.
   */
  private static final String RESTRICTED_ATTR = ContentRestrictedToken.class.getSimpleName();

  /**
   * Return custom error message when content access is protected.
   */
  @Override
  protected String getUnauthorizedMessage(final ServletRequest request) {
    Object attr = request.getAttribute(RESTRICTED_ATTR);
    if (attr != null) {
      return "Content access is protected by token";
    }
    else {
      return super.getUnauthorizedMessage(request);
    }
  }

  @Override
  protected AuthenticationToken createToken(final ServletRequest request, final ServletResponse response) {
    if (isRestricted(request)) {
      getLogger().debug("Content authentication for request is restricted");

      // mark request as protected for better error messaging
      request.setAttribute(RESTRICTED_ATTR, true);

      // We know our super-class makes UsernamePasswordTokens, ask super to pull out the relevant details
      UsernamePasswordToken basis = (UsernamePasswordToken) super.createToken(request, response);

      // And include more information than is normally provided to a token (ie. the request)
      return new ContentRestrictedToken(basis, request);
    }
    else {
      AuthenticationToken token = createAuthenticationToken(request, response);
      if (token != null) {
        return token;
      }
      return super.createToken(request, response);
    }
  }

  @Override
  protected boolean isAccessAllowed(final ServletRequest request, final ServletResponse response,
                                    final Object mappedValue)
  {
    if (!isRestricted(request) && isLoginAttempt(request, response)) {
      try {
        return executeLogin(request, response) && super.isAccessAllowed(request, response, mappedValue);
      }
      catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }
    return super.isAccessAllowed(request, response, mappedValue);
  }

  @Override
  protected boolean isLoginAttempt(ServletRequest request, ServletResponse response) {
    if (isRestricted(request)) {
      return super.isLoginAttempt(request, response);
    }
    AuthenticationToken token = createAuthenticationToken(request, response);
    return token != null || super.isLoginAttempt(request, response);
  }

  private AuthenticationToken createAuthenticationToken(ServletRequest request, ServletResponse response) {
    if (factories != null) {
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
    }
    return null;
  }
}
