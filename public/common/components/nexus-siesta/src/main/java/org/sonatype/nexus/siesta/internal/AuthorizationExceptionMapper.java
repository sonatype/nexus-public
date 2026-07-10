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
package org.sonatype.nexus.siesta.internal;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.inject.Provider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.sonatype.nexus.rest.ExceptionMapperSupport;

import org.sonatype.nexus.security.anonymous.AnonymousHelper;
import org.sonatype.nexus.security.authc.NexusBasicHttpAuthenticationFilter;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.subject.Subject;

import static com.google.common.base.Preconditions.checkNotNull;
import org.springframework.stereotype.Component;

/**
 * Maps {@link AuthorizationException} to appropriate HTTP status code per RFC 7235:
 * <ul>
 * <li>{@link Status#UNAUTHORIZED} (401) when user is not authenticated - indicates client should provide
 * credentials</li>
 * <li>{@link Status#FORBIDDEN} (403) when user is authenticated but lacks required permissions - authentication won't
 * help</li>
 * </ul>
 */
@Component
public class AuthorizationExceptionMapper
    extends ExceptionMapperSupport<AuthorizationException>
{
  private static final String AUTH_SCHEME_KEY = "auth.scheme";

  private static final String AUTH_REALM_KEY = "auth.realm";

  private static final String AUTHENTICATE_HEADER = "WWW-Authenticate";

  private final Provider<HttpServletRequest> httpRequestProvider;

  @Autowired
  public AuthorizationExceptionMapper(final Provider<HttpServletRequest> httpRequestProvider) {
    this.httpRequestProvider = checkNotNull(httpRequestProvider);
  }

  @Override
  protected Response convert(final AuthorizationException exception, final String id) {
    Subject subject = SecurityUtils.getSubject();

    if (isAuthenticatedUser(subject)) {
      return Response.status(Status.FORBIDDEN).build();
    }

    // UI requests get 403 to avoid browser's native auth dialog
    // (WWW-Authenticate on 401 triggers it)
    if (isUiRequest()) {
      return Response.status(Status.FORBIDDEN).build();
    }

    return buildUnauthorizedResponse();
  }

  private boolean isUiRequest() {
    HttpServletRequest request = httpRequestProvider.get();
    return "true".equals(request.getHeader("X-Nexus-UI"));
  }

  /**
   * Determines if subject is authenticated, including anonymous per Nexus documentation:
   * "the anonymous user is considered logged in"
   */
  private boolean isAuthenticatedUser(final Subject subject) {
    return subject.getPrincipal() != null &&
        (subject.isAuthenticated() || AnonymousHelper.isAnonymous(subject));
  }

  private Response buildUnauthorizedResponse() {
    HttpServletRequest request = httpRequestProvider.get();
    String scheme = getAuthScheme(request);
    String realm = getAuthRealm(request);

    return Response.status(Status.UNAUTHORIZED)
        .header(AUTHENTICATE_HEADER, String.format("%s realm=\"%s\"", scheme, realm))
        .build();
  }

  private String getAuthScheme(final HttpServletRequest request) {
    String scheme = (String) request.getAttribute(AUTH_SCHEME_KEY);
    return scheme != null ? scheme : "Basic";
  }

  private String getAuthRealm(final HttpServletRequest request) {
    String realm = (String) request.getAttribute(AUTH_REALM_KEY);
    return realm != null ? realm : NexusBasicHttpAuthenticationFilter.BASIC_AUTH_REALM;
  }
}
