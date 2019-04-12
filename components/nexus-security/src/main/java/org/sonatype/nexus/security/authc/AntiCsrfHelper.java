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

import java.util.Optional;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.text.Strings2;

import org.apache.shiro.authz.UnauthorizedException;

/**
 * @since 3.16
 */
@Named
@Singleton
public class AntiCsrfHelper extends ComponentSupport
{
  public static final String ENABLED = "nexus.security.anticsrftoken.enabled";

  public static final String ERROR_MESSAGE_TOKEN_MISMATCH = "Anti cross-site request forgery token mismatch";

  public static final String ANTI_CSRF_TOKEN_NAME = "NX-ANTI-CSRF-TOKEN";

  private final boolean enabled;

  @Inject
  public AntiCsrfHelper(@Named("${nexus.security.anticsrftoken.enabled:-true}") final boolean enabled)
  {
    this.enabled = enabled;
  }

  /**
   * Checks the request for CSRF if the token is invalid.
   *
   * @return true if the token is valid or if the token does not require validation. Requests with a multipart form
   *         content type should call {@link requireValidToken} once the field is extracted.
   */
  public boolean isAccessAllowed(final HttpServletRequest httpRequest) {
    if (!enabled) {
      return true;
    }

    return isSafeHttpMethod(httpRequest)
        || isMultiPartFormDataPost(httpRequest) // token is passed as a form field instead of a custom header
                                                // and is validated in the directnjine code so we just needed
                                                // to create the cookie above
        || isNotBrowserRequest(httpRequest)
        || isAntiCsrfTokenValid(httpRequest, Optional.ofNullable(httpRequest.getHeader(ANTI_CSRF_TOKEN_NAME)));
  }

  /**
   * Validate that the token passed as an argument matches the cookie in the request (if the request requires
   * validation)
   *
   * @throws UnauthorizedException when the provided token is missing or does not match the request
   */
  public void requireValidToken(final HttpServletRequest httpRequest, @Nullable final String token) {
    Optional<String> optToken = token == null ? Optional.ofNullable(httpRequest.getHeader(ANTI_CSRF_TOKEN_NAME))
        : Optional.of(token);
    if (!enabled || isNotBrowserRequest(httpRequest) || isAntiCsrfTokenValid(httpRequest, optToken)) {
      return;
    }
    throw new UnauthorizedException(ERROR_MESSAGE_TOKEN_MISMATCH);
  }

  private boolean isSafeHttpMethod(final HttpServletRequest request) {
    String method = request.getMethod();
    return HttpMethod.GET.equals(method) || HttpMethod.HEAD.equals(method);
  }

  private boolean isMultiPartFormDataPost(final HttpServletRequest request) {
    return HttpMethod.POST.equals(request.getMethod()) && !Strings2.isBlank(request.getContentType())
        && MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(MediaType.valueOf(request.getContentType()));
  }

  private boolean isNotBrowserRequest(final HttpServletRequest request) {
    String userAgent = request.getHeader("User-Agent");

    return userAgent == null || !userAgent.startsWith("Mozilla/");
  }

  private Optional<String> getCookie(final HttpServletRequest request, final String cookieName) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (cookieName.equals(cookie.getName())) {
          return Optional.ofNullable(cookie.getValue());
        }
      }
    }
    return Optional.empty();
  }

  private Optional<String> getAntiCsrfTokenCookie(final HttpServletRequest request) {
    return getCookie(request, ANTI_CSRF_TOKEN_NAME);
  }

  private boolean isAntiCsrfTokenValid(final HttpServletRequest request, final Optional<String> token) {
    Optional<String> cookie = getAntiCsrfTokenCookie(request);

    return token.isPresent() && token.equals(cookie);
  }

  /**
   * @return whether CSRF protection is enabled
   */
  public boolean isEnabled() {
    return enabled;
  }
}
