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

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

import org.sonatype.nexus.common.text.Strings2;

import com.google.common.net.HttpHeaders;
import org.apache.shiro.web.filter.authc.AuthenticationFilter;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This is an anti cross-site request forgery (CSRF / XSRF) protection using a cookie-to-header token approach.
 *
 * @since 3.13
 */
@Named
@Singleton
public class AntiCsrfFilter
    extends AuthenticationFilter
{

  private static final Logger log = LoggerFactory.getLogger(AntiCsrfFilter.class);

  public static final String ENABLED = "nexus.security.anticsrftoken.enabled";

  private static final String ERROR_MESSAGE_TOKEN_MISMATCH = "Anti cross-site request forgery token mismatch";

  public static final String NAME = "nx-anticsrf-authc";

  public static final String ANTI_CSRF_TOKEN_NAME = "NX-ANTI-CSRF-TOKEN";

  private static final String SESSION_COOKIE_NAME = "NXSESSIONID";

  private final boolean enabled;

  @Inject
  public AntiCsrfFilter(@Named("${nexus.security.anticsrftoken.enabled:-true}") final boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isEnabled() {
    return enabled;
  }

  @Override
  protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    if (!isEnabled() || !isCsrfCheckWarranted(httpRequest)) {
      return true;
    }

    provideAntiCsrfTokenCookieIfAbsent(httpRequest, httpResponse);

    return isSafeHttpMethod(httpRequest)
        || isMultiPartFormDataPost(httpRequest) // token is passed as a form field instead of a custom header
                                                // and is validated in the directnjine code so we just needed
                                                // to create the cookie above
        || isSessionAndRefererAbsent(httpRequest)
        || isAntiCsrfTokenValid(httpRequest);
  }

  @Override
  protected boolean onAccessDenied(final ServletRequest request, final ServletResponse response) throws IOException
  {
    log.debug("Rejecting request from {} due to invalid cross-site request forgery token", request.getRemoteAddr());

    HttpServletResponse httpResponse = (HttpServletResponse) response;
    httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    httpResponse.setContentType("text/plain");
    httpResponse.getWriter().print(ERROR_MESSAGE_TOKEN_MISMATCH);

    return false;
  }

  private boolean isCsrfCheckWarranted(HttpServletRequest request) {
    String userAgent = request.getHeader("User-Agent");

    return userAgent != null && userAgent.startsWith("Mozilla/");
  }

  private boolean isSafeHttpMethod(final HttpServletRequest request) {
    String method = request.getMethod();
    return HttpMethod.GET.equals(method) || HttpMethod.HEAD.equals(method);
  }

  private boolean isMultiPartFormDataPost(HttpServletRequest request) {
    return HttpMethod.POST.equals(request.getMethod())
        && !Strings2.isBlank(request.getContentType())
        && MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(MediaType.valueOf(request.getContentType()));
  }

  private boolean isSessionAndRefererAbsent(final HttpServletRequest request) {
    return !getCookie(request, SESSION_COOKIE_NAME).isPresent()
        && isRefererAbsent(request);
  }

  private boolean isRefererAbsent(final HttpServletRequest request) {
    return Strings2.isBlank(request.getHeader(HttpHeaders.REFERER));
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

  private void provideAntiCsrfTokenCookieIfAbsent(final HttpServletRequest request,
                                                  final HttpServletResponse response)
  {
    Optional<String> antiCsrfTokenCookie = getAntiCsrfTokenCookie(request);
    if (!antiCsrfTokenCookie.isPresent()) {
      SimpleCookie csrfCookie = new SimpleCookie(ANTI_CSRF_TOKEN_NAME);
      csrfCookie.setValue(UUID.randomUUID().toString());
      csrfCookie.setPath("/");
      csrfCookie.setHttpOnly(false);
      csrfCookie.saveTo(request, response);
    }
  }

  private boolean isAntiCsrfTokenValid(final HttpServletRequest request) {
    Optional<String> header = Optional.ofNullable(request.getHeader(ANTI_CSRF_TOKEN_NAME));
    Optional<String> cookie = getAntiCsrfTokenCookie(request);

    return header.isPresent() && cookie.isPresent() && header.equals(cookie);
  }

}
