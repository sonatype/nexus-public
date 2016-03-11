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

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.auth.ClientInfo;
import org.sonatype.nexus.auth.NexusAuthenticationEvent;
import org.sonatype.nexus.web.BaseUrlHolder;
import org.sonatype.nexus.web.RemoteIPFinder;
import org.sonatype.nexus.web.TemplateRenderer;
import org.sonatype.nexus.web.TemplateRenderer.TemplateLocator;
import org.sonatype.nexus.web.internal.BrowserDetector;
import org.sonatype.security.SecuritySystem;
import org.sonatype.sisu.goodies.common.Loggers;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.ExpiredCredentialsException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;

import static org.sonatype.nexus.web.internal.SecurityFilter.ATTR_USER_ID;
import static org.sonatype.nexus.web.internal.SecurityFilter.ATTR_USER_PRINCIPAL;

public class NexusHttpAuthenticationFilter
    extends BasicHttpAuthenticationFilter
{
  public static final String AUTH_SCHEME_KEY = "auth.scheme";

  public static final String AUTH_REALM_KEY = "auth.realm";

  public static final String FAKE_AUTH_SCHEME = "NxBASIC";

  public static final String ANONYMOUS_LOGIN = "nexus.anonymous";

  private final Logger logger = Loggers.getLogger(getClass());

  private boolean fakeAuthScheme;

  // FIXME: Evil field injection should be replaced!

  @Inject
  private SecuritySystem securitySystem;

  @Inject
  private EventBus eventBus;

  @Inject
  private TemplateRenderer templateRenderer;

  @Inject
  private BrowserDetector browserDetector;
  
  // ==
  
  private String nexusVersion;
  
  @Inject
  public void setApplicationVersion(final ApplicationStatusSource applicationStatusSource) {
    this.nexusVersion = applicationStatusSource.getSystemStatus().getVersion();
  }
  
  // ==

  protected SecuritySystem getSecuritySystem() {
    return securitySystem;
  }

  protected Logger getLogger() {
    return logger;
  }

  // TODO: this should be boolean, but see
  // http://issues.jsecurity.org/browse/JSEC-119
  public String isFakeAuthScheme() {
    return Boolean.toString(fakeAuthScheme);
  }

  // TODO: this should be boolean, but see
  // http://issues.jsecurity.org/browse/JSEC-119
  public void setFakeAuthScheme(String fakeAuthSchemeStr) {
    this.fakeAuthScheme = Boolean.parseBoolean(fakeAuthSchemeStr);

    if (fakeAuthScheme) {
      setAuthcScheme(FAKE_AUTH_SCHEME);
      setAuthzScheme(FAKE_AUTH_SCHEME);
    }
    else {
      setAuthcScheme(HttpServletRequest.BASIC_AUTH);
      setAuthzScheme(HttpServletRequest.BASIC_AUTH);
    }
  }

  @Override
  protected boolean onAccessDenied(ServletRequest request, ServletResponse response)
      throws Exception
  {
    // this will be true if cookie is sent with request and it is valid
    Subject subject = getSubject(request, response);

    // NEXUS-607: fix for cookies, when sent from client. They will expire once
    // and we are not sending them anymore.
    boolean loggedIn = subject.isAuthenticated();

    if (loggedIn) {
      return true;
    }

    if (isLoginAttempt(request, response)) {
      // NEXUS-5049: Check is this an attempt with "anonymous" user?
      // We do not allow logins with anonymous user if anon access is disabled
      final AuthenticationToken token = createToken(request, response);
      final String anonymousUsername = getSecuritySystem().getAnonymousUsername();
      final String loginUsername = token.getPrincipal().toString();
      if (!getSecuritySystem().isAnonymousAccessEnabled()
          && StringUtils.equals(anonymousUsername, loginUsername)) {
        getLogger().info(
            "Login attempt with username \"" + anonymousUsername
                + "\" (used for Anonymous Access) while Anonymous Access is disabled.");
        loggedIn = false;
      }
      else {
        try {
          loggedIn = executeLogin(request, response);
        }
        // if no username or password is supplied, an IllegalStateException (runtime)
        // is thrown, so if anything fails in executeLogin just assume failed login
        catch (Exception e) {
          getLogger().error("Unable to login", e);
          loggedIn = false;
        }
      }
    }
    else {
      // let the user "fall thru" until we get some permission problem
      if (getSecuritySystem().isAnonymousAccessEnabled()) {
        loggedIn = executeAnonymousLogin(request, response);
      }
    }

    if (!loggedIn) {
      sendChallenge(request, response);
    }
    else {
      request.setAttribute(AUTH_SCHEME_KEY, getAuthcScheme());

      request.setAttribute(AUTH_REALM_KEY, getApplicationName());
    }

    return loggedIn;
  }

  /**
   * Allow customization of 401 status line message.
   */
  protected String getUnauthorizedMessage(final ServletRequest request) {
    return "Unauthorized";
  }

  /**
   * If request comes from a web-browser render an error page, else perform default challenge.
   */
  @Override
  protected boolean sendChallenge(final ServletRequest request, final ServletResponse response) {
    if (browserDetector.isBrowserInitiated(request)) {
      HttpServletResponse httpResponse = WebUtils.toHttp(response);
      httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED, getUnauthorizedMessage(request));
      // omit WWW-Authenticate we do NOT want to have browser prompt

      Map<String,Object> params = ImmutableMap.of(
          "nexusVersion", nexusVersion,
          "nexusRoot", (Object)BaseUrlHolder.get()
      );
      TemplateLocator template = templateRenderer.template(
          "/org/sonatype/nexus/web/internal/accessDeniedHtml.vm",
          NexusHttpAuthenticationFilter.class.getClassLoader()
      );

      try {
        templateRenderer.render(template, params, httpResponse);
      }
      catch (IOException e) {
        throw Throwables.propagate(e);
      }

      return false;
    }
    else {
      String message = getUnauthorizedMessage(request);
      getLogger().debug("Authentication required: sending 401 Authentication challenge response: {}", message);
      HttpServletResponse httpResponse = WebUtils.toHttp(response);
      httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED, message);
      String authcHeader = getAuthcScheme() + " realm=\"" + getApplicationName() + "\"";
      httpResponse.setHeader(AUTHENTICATE_HEADER, authcHeader);
      return false;
    }
  }

  @Override
  protected boolean isLoginAttempt(String authzHeader) {
    // handle BASIC in the same way as our faked one
    String authzHeaderScheme = getAuthzScheme().toLowerCase();

    if (authzHeader.toLowerCase().startsWith(HttpServletRequest.BASIC_AUTH.toLowerCase())) {
      return true;
    }
    else {
      return super.isLoginAttempt(authzHeaderScheme);
    }
  }

  /**
   * TODO: consider moving this to a new filter, and chain them together
   */
  protected boolean executeAnonymousLogin(ServletRequest request, ServletResponse response) {
    getLogger().debug("Attempting to authenticate Subject as Anonymous request...");

    boolean anonymousLoginSuccessful = false;

    Subject subject = getSubject(request, response);

    // disable the session creation for the anon user.
    request.setAttribute(DefaultSubjectContext.SESSION_CREATION_ENABLED, Boolean.FALSE);

    UsernamePasswordToken usernamePasswordToken =
        new UsernamePasswordToken(getSecuritySystem().getAnonymousUsername(),
            getSecuritySystem().getAnonymousPassword());

    try {
      request.setAttribute(ANONYMOUS_LOGIN, Boolean.TRUE);

      subject.login(usernamePasswordToken);
      anonymousLoginSuccessful = true;
    }
    catch (UnknownSessionException e) {
      Session anonSession = subject.getSession(false);

      this.getLogger().debug(
          "Unknown session exception while logging in anonymous user: '{}' with principal '{}'",
          new Object[]{anonSession, usernamePasswordToken.getUsername(), e});

      if (anonSession != null) {
        // clear the session
        this.getLogger().debug("Logging out the current anonymous user, to clear the session.");
        try {
          subject.logout();
        }
        catch (UnknownSessionException expectedException) {
          this.logger.trace(
              "Forced a logout with an Unknown Session so the current subject would get cleaned up.", e);
        }

        // login again
        this.getLogger().debug("Attempting to login as anonymous for the second time.");
        subject.login(usernamePasswordToken);

        anonymousLoginSuccessful = true;
      }
    }
    catch (AuthenticationException ae) {
      getLogger().info(
          "Unable to authenticate user [anonymous] from IP Address "
              + RemoteIPFinder.findIP((HttpServletRequest) request));

      getLogger().debug("Unable to log in subject as anonymous", ae);
    }

    if (anonymousLoginSuccessful) {
      getLogger().debug("Successfully logged in as anonymous");

      postAuthcEvent(request, getSecuritySystem().getAnonymousUsername(), getUserAgent(request), true);

      return true;
    }

    // always default to false. If we've made it to this point in the code, that
    // means the authentication attempt either never occured, or wasn't successful:
    return false;
  }

  private void postAuthcEvent(ServletRequest request, String username, String userAgent, boolean success) {
    if (eventBus != null) {
      eventBus.post(
          new NexusAuthenticationEvent(
              this,
              new ClientInfo(StringEscapeUtils.escapeHtml(username), RemoteIPFinder.findIP((HttpServletRequest) request), userAgent),
              success
          )
      );
    }
  }

  @Override
  protected boolean onLoginSuccess(final AuthenticationToken token,
                                   final Subject subject,
                                   final ServletRequest request,
                                   final ServletResponse response)
  {
    // Prefer the subject principal over the token's, as these could be different for token-based authentication
    Object principal = subject.getPrincipal();
    if (principal == null) {
      principal = token.getPrincipal();
    }
    String userId = principal.toString();
    if (request instanceof HttpServletRequest) {
      // Attach principal+userId to request so we can use that in the request-log
      ((HttpServletRequest) request).setAttribute(ATTR_USER_PRINCIPAL, principal);
      ((HttpServletRequest) request).setAttribute(ATTR_USER_ID, userId);
    }
    postAuthcEvent(request, userId, getUserAgent(request), true);
    return true;
  }

  @Override
  protected boolean onLoginFailure(AuthenticationToken token, AuthenticationException ae, ServletRequest request,
                                   ServletResponse response)
  {
    postAuthcEvent(request, token.getPrincipal().toString(), getUserAgent(request), false);

    HttpServletResponse httpResponse = WebUtils.toHttp(response);

    if (ExpiredCredentialsException.class.isAssignableFrom(ae.getClass())) {
      httpResponse.addHeader("X-Nexus-Reason", "expired");
    }

    return false;
  }

  @Override
  public void postHandle(ServletRequest request, ServletResponse response)
      throws Exception
  {
    if (request.getAttribute(org.sonatype.nexus.web.Constants.ATTR_KEY_REQUEST_IS_AUTHZ_REJECTED) != null) {
      if (request.getAttribute(ANONYMOUS_LOGIN) != null) {
        sendChallenge(request, response);
      }
      else {

        if (getLogger().isDebugEnabled()) {
          final Subject subject = getSubject(request, response);

          String username;

          if (subject != null && subject.isAuthenticated() && subject.getPrincipal() != null) {
            username = subject.getPrincipal().toString();
          }
          else {
            username = getSecuritySystem().getAnonymousUsername();
          }

          getLogger().debug(
              "Request processing is rejected because user \"" + username + "\" lacks permissions.");
        }

        sendForbidden(request, response);
      }
    }
  }

  /**
   * set http 403 forbidden header for the response
   */
  protected void sendForbidden(ServletRequest request, ServletResponse response) throws IOException {
    HttpServletResponse httpResponse = WebUtils.toHttp(response);
    httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
  }

  // Will retrieve authz header. if missing from header, will try
  // to retrieve from request params instead
  @Override
  protected String getAuthzHeader(ServletRequest request) {
    String authzHeader = super.getAuthzHeader(request);

    // If in header use it
    if (!StringUtils.isEmpty(authzHeader)) {
      getLogger().debug("Using authorization header from request");
      return authzHeader;
    }
    // otherwise check request params for it
    else {
      authzHeader = request.getParameter("authorization");

      if (!StringUtils.isEmpty(authzHeader)) {
        getLogger().debug("Using authorization from request parameter");
      }
      else {
        getLogger().debug("No authorization found (header or request parameter)");
      }

      return authzHeader;
    }
  }

  // work around to accept password with ':' character
  @Override
  protected String[] getPrincipalsAndCredentials(String scheme, String encoded) {
    // no credentials, no auth
    if (StringUtils.isEmpty(encoded)) {
      return null;
    }

    String decoded = Base64.decodeToString(encoded);

    // no credentials, no auth
    if (StringUtils.isEmpty(encoded)) {
      return null;
    }

    String[] parts = decoded.split(":");

    // invalid credentials, no auth
    if (parts == null || parts.length < 2) {
      return null;
    }

    return new String[]{parts[0], decoded.substring(parts[0].length() + 1)};
  }

  // ==

  protected Object getAttribute(String key) {
    return getFilterConfig().getServletContext().getAttribute(key);
  }

  private String getUserAgent(final ServletRequest request) {
    if (request instanceof HttpServletRequest) {
      final String userAgent = ((HttpServletRequest) request).getHeader("User-Agent");

      return userAgent;
    }

    return null;
  }
}
