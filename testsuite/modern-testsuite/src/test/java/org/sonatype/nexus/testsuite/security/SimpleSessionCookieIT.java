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
package org.sonatype.nexus.testsuite.security;

import java.net.URL;
import java.util.Date;
import java.util.List;

import org.sonatype.nexus.bundle.launcher.NexusBundleConfiguration;
import org.sonatype.nexus.client.core.subsystem.ServerConfiguration;
import org.sonatype.nexus.client.core.subsystem.config.Security;
import org.sonatype.nexus.testsuite.NexusHttpsITSupport;
import org.sonatype.nexus.testsuite.support.NexusStartAndStopStrategy;

import com.google.common.net.HttpHeaders;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.SetCookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.cookie.BrowserCompatSpecFactory;
import org.junit.Before;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@NexusStartAndStopStrategy(NexusStartAndStopStrategy.Strategy.EACH_TEST)
public class SimpleSessionCookieIT
    extends NexusHttpsITSupport
{

  public SimpleSessionCookieIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Override
  protected NexusBundleConfiguration configureNexus(NexusBundleConfiguration configuration) {
    // help verify that a custom context path sets the cookie path value correctly
    return super.configureNexus(configuration).setContextPath("/customcontextpath");
  }

  @Before
  public void disableAnonymousSoThatAllRequestsRequireAuthentication() throws Exception {
    Security security = client().getSubsystem(ServerConfiguration.class).security();
    security.settings().withAnonymousAccessEnabled(false);
    security.save();
  }

  @Test
  public void authenticatedContentCRUDActionsShouldNotCreateSession() throws Exception {
    final String target = nexus().getUrl() + "content/repositories/releases/test.txt";

    final HttpPut put = new HttpPut(target);
    put.setEntity(new StringEntity("text content"));
    try (CloseableHttpClient client = clientBuilder().setDefaultCredentialsProvider(credentialsProvider()).build()) {
      try (CloseableHttpResponse response = client.execute(put, clientContext())) {
        assertThat(response.getStatusLine().getStatusCode(), is(201));
        assertResponseHasNoCookies(response);
      }
    }

    final HttpHead head = new HttpHead(target);
    try (CloseableHttpClient client = clientBuilder().setDefaultCredentialsProvider(credentialsProvider()).build()) {
      try (CloseableHttpResponse response = client.execute(head, clientContext())) {
        assertThat(response.getStatusLine().getStatusCode(), is(200));
        assertResponseHasNoCookies(response);
      }
    }

    final HttpGet get = new HttpGet(target);
    try (CloseableHttpClient client = clientBuilder().setDefaultCredentialsProvider(credentialsProvider()).build()) {
      try (CloseableHttpResponse response = client.execute(get, clientContext())) {
        assertThat(response.getStatusLine().getStatusCode(), is(200));
        assertResponseHasNoCookies(response);
      }
    }

    final HttpDelete delete = new HttpDelete(target);
    try (CloseableHttpClient client = clientBuilder().setDefaultCredentialsProvider(credentialsProvider()).build()) {
      try (CloseableHttpResponse response = client.execute(delete, clientContext())) {
        assertThat(response.getStatusLine().getStatusCode(), is(204));
        assertResponseHasNoCookies(response);
      }
    }
  }

  private void assertResponseHasNoCookies(final HttpResponse response) {
    Header[] headers = response.getHeaders(HttpHeaders.SET_COOKIE);
    assertThat(String.format("not expecting any %s headers but got %s", HttpHeaders.SET_COOKIE, asList(headers)),
        headers,
        arrayWithSize(0));
  }

  @Test
  public void defaultSessionCookieSpecUsingHttps() throws Exception {
    exerciseCookieSpec(nexus().getSecureUrl());
  }

  @Test
  public void defaultSessionCookieSpecUsingHttp() throws Exception {
    exerciseCookieSpec(nexus().getUrl());
  }

  /**
   * Validate Nexus Cookies during Sign-in and Sign-out
   *
   * @param nexusUrl the base Nexus URL to validate against
   */
  private void exerciseCookieSpec(final URL nexusUrl) throws Exception {

    // handle cookies like a browser to aid validation
    final CookieSpec spec = new BrowserCompatSpecFactory().create(null);
    final CookieOrigin cookieOrigin = cookieOrigin(nexusUrl);
    final CookieStore cookieStore = new BasicCookieStore();
    final CredentialsProvider credProvider = credentialsProvider();
    SetCookie loginCookie;

    try (CloseableHttpClient client = clientBuilder().setDefaultCookieStore(cookieStore).
        setDefaultCredentialsProvider(credProvider).build()) {

      // 1. login with credentials and get session cookie
      // Set-Cookie: JSESSIONID=98a766bc-bc33-4b3c-9d9f-d3bb85b0cf00; Path=/nexus; Secure; HttpOnly
      HttpGet loginGet = new HttpGet(nexusUrl.toExternalForm() + "service/local/authentication/login");
      try (CloseableHttpResponse response = client.execute(loginGet, clientContext())) {
        assertThat(response.getStatusLine().getStatusCode(), is(200));
        assertThat("login cookie should have been stored in the cookie store", cookieStore.getCookies(),
            hasSize(1));
        assertThat("expected session cookie in cookie store", getSessionCookie(cookieStore), notNullValue());

        Header[] sessionCookieHeaders = response.getHeaders("Set-Cookie");
        Header sessionCookieHeader = sessionCookieHeaders[0];
        List<Cookie> sessionCookies = spec.parse(sessionCookieHeader, cookieOrigin);
        loginCookie = (SetCookie) sessionCookies.get(0);
        String headerText = sessionCookieHeader.toString();

        assertCommonSessionCookieAttributes(nexusUrl, loginCookie, headerText);
        assertThat("expecting one cookie in same Set-Cookie header", sessionCookies, hasSize(1));
        assertThat("expecting one Set-Cookie header for login", sessionCookieHeaders, arrayWithSize(1));
        assertThat("login cookie should NOT look like deleteMe cookie", loginCookie.getValue(), not(containsString(
            "deleteMe")));
        assertThat("login cookie should not have an expiry date - the UA deletes the session cookie when " +
                "replaced by a new one by same name from the server OR when the UA decides",
            loginCookie.isPersistent(), is(false));

        assertThat("login session cookie with valid session id should always be marked HttpOnly",
            headerText, containsString("; HttpOnly"));
      }

      HttpClientContext logoutContext = HttpClientContext.create();
      logoutContext.setCookieStore(cookieStore);

      // 2. Logout, sending valid session cookie, no credentials
      // Set-Cookie: JSESSIONID=deleteMe; Path=/nexus; Max-Age=0; Expires=Sun, 28-Dec-2014 15:59:11 GMT
      HttpGet logoutGet = new HttpGet(nexusUrl.toExternalForm() + "service/local/authentication/logout");
      try (CloseableHttpResponse response = client.execute(logoutGet, logoutContext)) {
        assertThat(response.getStatusLine().getStatusCode(), is(200));

        // can't use client CookieStore to examine logout cookie, because the Expires header will prevent it from being
        // added but we can implicitly confirm it expired the existing cookie according to our client
        assertThat("logout cookie should have emptied the cookie store due to expiry date", cookieStore.getCookies(),
            hasSize(0));

        Header[] sessionCookieHeaders = response.getHeaders("Set-Cookie");
        assertThat("expecting one Set-Cookie header for logout", sessionCookieHeaders, arrayWithSize(1));
        Header sessionCookieHeader = sessionCookieHeaders[0];
        List<Cookie> sessionCookies = spec.parse(sessionCookieHeader, cookieOrigin);
        SetCookie logoutCookie = (SetCookie) sessionCookies.get(0);
        final String headerText = sessionCookieHeader.toString();

        assertCommonSessionCookieAttributes(nexusUrl, logoutCookie, headerText);
        assertThat("expecting one cookie in same Set-Cookie header", sessionCookies, hasSize(1));
        assertThat("logout session cookie value should be dummy value", logoutCookie.getValue(), equalTo("deleteMe"));
        assertThat("logout session cookie should be expired to tell browser to delete it",
            logoutCookie.isExpired(new Date()), is(true));
        assertThat(
            "technically the presence of an expiry date means the cookie is persistent, but expiry will override",
            logoutCookie.isPersistent(), is(true));
        assertThat("logout cookie does not have a real session id value, therefore it does not need to be HttpOnly",
            headerText, not(containsString("; HttpOnly")));
      }

      // 3. Access a protected resource again using our original login cookie, no credentials, to verify session is dead
      HttpGet loginFailedGet = new HttpGet(nexusUrl.toExternalForm() + "service/local/authentication/login");
      cookieStore.addCookie(loginCookie);
      try (CloseableHttpResponse response = client.execute(loginFailedGet, HttpClientContext.create())) {
        assertThat("expected dead login session cookie to not authenticate", response.getStatusLine().getStatusCode(),
            is(401));
        Header[] sessionCookieHeaders = response.getHeaders("Set-Cookie");
        assertThat("expecting no cookies since login was unsuccessful", sessionCookieHeaders, arrayWithSize(0));
      }
    }
  }

  /**
   * Validate standard session cookie properties
   */
  protected void assertCommonSessionCookieAttributes(final URL nexusUrl, final SetCookie serverCookie,
                                                     final String headerText)
  {
    assertThat(serverCookie, notNullValue());
    assertThat("cookie value must be present", serverCookie.getValue(), notNullValue());
    assertThat("cookie name mismatch", serverCookie.getName(), equalTo("NXSESSIONID"));
    assertThat("not expecting to get ports since they are generally ignored", serverCookie.getPorts(), nullValue());
    assertThat("session cookie does not currently set the domain leaving it to the Browser to do the right thing",
        headerText, not(containsString("; Domain=")));
    assertThat("browser should interpret domain as same as origin", serverCookie.getDomain(),
        equalTo(nexusUrl.getHost()));
    assertThat("cookie path should match Nexus context path", serverCookie.getPath(),
        equalTo(expectedCookiePath(nexusUrl)));
    if (nexusUrl.getProtocol().equalsIgnoreCase("https")) {
      assertThat("session cookie should be marked Secure when cookies are served by https URLs",
          headerText, containsString("; Secure"));
    }
    else {
      assertThat("session cookie should not be marked Secure when cookies are served by http URLs",
          headerText, not(containsString("; Secure")));
    }
  }
}
