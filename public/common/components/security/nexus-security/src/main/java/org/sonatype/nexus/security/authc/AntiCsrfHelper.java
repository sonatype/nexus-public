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
import java.util.Optional;

import javax.annotation.Nullable;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

import org.sonatype.nexus.common.text.Strings2;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 3.16
 */
@Component
public class AntiCsrfHelper
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  public static final String ENABLED = "nexus.security.anticsrftoken.enabled";

  public static final String SEC_FETCH_SITE_HEADER_ENABLED = "nexus.security.anticsrftoken.secFetchSite.enabled";

  public static final String ERROR_MESSAGE_TOKEN_MISMATCH = "Anti cross-site request forgery token mismatch";

  public static final String ANTI_CSRF_TOKEN_NAME = "NX-ANTI-CSRF-TOKEN";

  private final boolean enabled;

  private final List<CsrfExemption> csrfExemptPaths;

  private final boolean secFetchHeaderEnabled;

  @Autowired
  public AntiCsrfHelper(
      @Value("${" + ENABLED + ":true}") final boolean enabled,
      @Value("${" + SEC_FETCH_SITE_HEADER_ENABLED + ":true}") final boolean secFetchHeaderEnabled,
      final List<CsrfExemption> csrfExemptPaths)
  {
    this.enabled = enabled;
    this.secFetchHeaderEnabled = secFetchHeaderEnabled;
    this.csrfExemptPaths = csrfExemptPaths;
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
    boolean safeHttpMethod = isSafeHttpMethod(httpRequest);
    if (!safeHttpMethod && isCrossSiteRequest(httpRequest)) {
      log.debug("Blocking cross-site request header Sec-Fetch-Site:{}",
          httpRequest.getHeader(HttpHeaders.SEC_FETCH_SITE));
      return false;
    }

    return safeHttpMethod
        || isMultiPartFormDataPost(httpRequest) // token is passed as a form field instead of a custom header
                                                // and is validated in the directnjine code so we just needed
                                                // to create the cookie above
        || !isSessionAuthentication() // non-session auth
        || isExemptRequest(httpRequest)
        || isAntiCsrfTokenValid(httpRequest, Optional.ofNullable(httpRequest.getHeader(ANTI_CSRF_TOKEN_NAME)));
  }

  /**
   * Validate that the token passed as an argument matches the cookie in the request (if the request requires
   * validation)
   *
   * @throws UnauthorizedException when the provided token is missing or does not match the request
   */
  public void requireValidToken(final HttpServletRequest httpRequest, @Nullable final String token) {
    Optional<String> optToken = token == null
        ? Optional.ofNullable(httpRequest.getHeader(ANTI_CSRF_TOKEN_NAME))
        : Optional.of(token);
    if (!enabled || !isSessionAuthentication() || isAntiCsrfTokenValid(httpRequest, optToken)) {
      return;
    }
    throw new UnauthorizedException(ERROR_MESSAGE_TOKEN_MISMATCH);
  }

  @VisibleForTesting
  boolean isCrossSiteRequest(final HttpServletRequest request) {
    if (!secFetchHeaderEnabled) {
      return false;
    }
    String secFetchSiteHeader = request.getHeader(HttpHeaders.SEC_FETCH_SITE);
    if (secFetchSiteHeader == null) {
      return false;
    }

    return switch (secFetchSiteHeader) {
      case "same-origin" -> false;
      case "none" -> false;
      default -> true;
    };
  }

  private static boolean isSafeHttpMethod(final HttpServletRequest request) {
    String method = request.getMethod();
    return HttpMethod.GET.equals(method) || HttpMethod.HEAD.equals(method);
  }

  private boolean isMultiPartFormDataPost(final HttpServletRequest request) {
    String contentType = request.getContentType();
    try {
      return HttpMethod.POST.equals(request.getMethod()) && !Strings2.isBlank(contentType)
          && MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(MediaType.valueOf(contentType));
    }
    catch (IllegalArgumentException e) {
      log.debug("Failed to parse mediatype {}", contentType, e);
      return false;
    }
  }

  private static boolean isSessionAuthentication() {
    Subject subject = SecurityUtils.getSubject();

    return subject != null && subject.getSession(false) != null;
  }

  private static Optional<String> getCookie(final HttpServletRequest request, final String cookieName) {
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

  private static Optional<String> getAntiCsrfTokenCookie(final HttpServletRequest request) {
    return getCookie(request, ANTI_CSRF_TOKEN_NAME);
  }

  private static boolean isAntiCsrfTokenValid(final HttpServletRequest request, final Optional<String> token) {
    Optional<String> cookie = getAntiCsrfTokenCookie(request);

    return token.isPresent() && token.equals(cookie);
  }

  private boolean isExemptRequest(final HttpServletRequest request) {
    String requestPath = request.getRequestURI();
    return csrfExemptPaths.stream()
        .map(CsrfExemption::getPath)
        .anyMatch(requestPath::contains);
  }

  /**
   * @return whether CSRF protection is enabled
   */
  public boolean isEnabled() {
    return enabled;
  }
}
