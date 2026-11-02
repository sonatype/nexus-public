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

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.HttpMethod;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.HttpHeaders;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

  /** Prefix under which repository content is served. */
  private static final String REPOSITORY_PATH_PREFIX = "/repository";

  private final boolean enabled;

  private final boolean secFetchHeaderEnabled;

  @Autowired
  public AntiCsrfHelper(
      @Value("${" + ENABLED + ":true}") final boolean enabled,
      @Value("${" + SEC_FETCH_SITE_HEADER_ENABLED + ":true}") final boolean secFetchHeaderEnabled)
  {
    this.enabled = enabled;
    this.secFetchHeaderEnabled = secFetchHeaderEnabled;
  }

  /**
   * Checks the request for CSRF if the token is invalid.
   *
   * @return true if the token is valid or if the token does not require validation.
   */
  public boolean isAccessAllowed(final HttpServletRequest httpRequest) {
    if (!enabled) {
      return true;
    }
    boolean safeHttpMethod = isSafeHttpMethod(httpRequest);
    if (!safeHttpMethod && !isRepositoryPath(httpRequest) && isCrossSiteRequest(httpRequest)) {
      log.debug("Blocking cross-site request header Sec-Fetch-Site:{}",
          httpRequest.getHeader(HttpHeaders.SEC_FETCH_SITE));
      return false;
    }

    return safeHttpMethod
        || !isSessionAuthentication() // non-session auth
        || isAntiCsrfTokenValid(httpRequest, Optional.ofNullable(httpRequest.getHeader(ANTI_CSRF_TOKEN_NAME)));
  }

  /**
   * Repository content endpoints, which are a machine-facing API rather than the session-backed UI.
   *
   * <p>
   * These are exempt from the {@code Sec-Fetch-Site} check because that check cannot distinguish a
   * legitimate non-browser client from a forged browser request, and it blocks the legitimate case: VS
   * Code and its derivatives render the extension gallery in a Chromium view whose origin is
   * {@code vscode-file://vscode-app}, so every gallery call carries
   * {@code Sec-Fetch-Site: cross-site} and was rejected before the credential logic was reached.
   *
   * <p>
   * <strong>The anti-CSRF token check is what protects these endpoints, and this exemption does not
   * weaken it.</strong> {@link #isAccessAllowed} falls through to
   * {@code !isSessionAuthentication() || isAntiCsrfTokenValid(...)}, so a session-authenticated
   * cross-site POST is still rejected on the token whether or not the header check ran. Removing that
   * fall-through would leave repository endpoints with no CSRF protection at all.
   *
   * <p>
   * An earlier version of this comment claimed the session cookie is issued {@code SameSite=Lax} and
   * therefore would not be attached to a cross-site POST. That is not true of this codebase:
   * {@code JwtHelper.createCookie} sets only {@code Max-Age}, {@code Path}, {@code HttpOnly} and
   * {@code Secure}, and no {@code SameSite} attribute is configured anywhere. Browsers default an
   * unspecified cookie to {@code Lax} in current versions, but that is a browser default rather than
   * something asserted here, and it must not be relied on as the reason this exemption is safe.
   *
   * <p>
   * Token- and basic-authenticated repository calls are unaffected either way; they do not rely on a
   * session, so CSRF does not apply to them.
   *
   * <p>
   * Scoped to the path prefix rather than to a repository format on purpose: this filter runs in the
   * servlet chain, before the repository is looked up, so no format is known here. A URL segment names
   * a repository, not a format, and repository names are user-chosen — keying a security decision on
   * one would be keying it on a naming convention.
   */
  private static boolean isRepositoryPath(final HttpServletRequest request) {
    String uri = request.getRequestURI();
    if (uri == null) {
      return false;
    }
    String contextPath = request.getContextPath();
    if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
      uri = uri.substring(contextPath.length());
    }
    return uri.equals(REPOSITORY_PATH_PREFIX) || uri.startsWith(REPOSITORY_PATH_PREFIX + "/");
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

  /**
   * Methods that cannot mutate state, and so cannot be a cross-site request forgery vector.
   *
   * <p>
   * {@code OPTIONS} is included because a CORS preflight carries no credentials and no body, and the
   * browser discards its response body — there is nothing for an attacker to forge or to read back.
   * Treating it as unsafe breaks every browser-based client on a cross-site origin: the preflight is
   * rejected with a 401 before any downstream handler runs, and because a failed preflight stops the
   * browser from sending the real request at all, the failure reaches the user only as an unexplained
   * CORS error with no server-side trace of the request that was never sent.
   *
   * <p>
   * Observed with VS Code and its derivatives, whose renderer origin is {@code vscode-file://vscode-app}
   * and which therefore send {@code Sec-Fetch-Site: cross-site} on every extension gallery call.
   */
  private static boolean isSafeHttpMethod(final HttpServletRequest request) {
    String method = request.getMethod();
    return HttpMethod.GET.equals(method)
        || HttpMethod.HEAD.equals(method)
        || HttpMethod.OPTIONS.equals(method);
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

  /**
   * @return whether CSRF protection is enabled
   */
  public boolean isEnabled() {
    return enabled;
  }
}
