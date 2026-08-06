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
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.sonatype.nexus.datastore.api.DataAccessException;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NexusBasicHttpAuthenticationFilterTest
{
  private NexusBasicHttpAuthenticationFilter filter;

  private HttpServletRequest request;

  private HttpServletResponse response;

  @Before
  public void setUp() {
    filter = new NexusBasicHttpAuthenticationFilter();
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
  }

  @Test
  public void testInfrastructureExceptionReturns503() throws Exception {
    AuthenticationToken token = new UsernamePasswordToken("admin", "admin123");

    // Create wrapped database exception (as it would come from Shiro)
    DataAccessException dataAccessException = new DataAccessException("Database unavailable");
    AuthenticationException shiroException =
        new AuthenticationException("Authentication failed", dataAccessException);

    // Call onLoginFailure
    boolean result = filter.onLoginFailure(token, shiroException, request, response);

    // Verify Retry-After header was set
    verify(response).setHeader(eq("Retry-After"), eq("60"));
    // Verify 503 was sent
    verify(response).sendError(eq(HttpServletResponse.SC_SERVICE_UNAVAILABLE),
        eq("Service temporarily unavailable"));
    // Verify method returned false (authentication failed, don't continue)
    assertThat(result, is(false));
  }

  @Test
  public void testNormalAuthenticationFailureReturns401() throws Exception {
    AuthenticationToken token = new UsernamePasswordToken("admin", "wrongpassword");

    // Normal authentication exception (wrong credentials)
    AuthenticationException shiroException =
        new AuthenticationException("Authentication failed",
            new IncorrectCredentialsException());

    // Call onLoginFailure - should delegate to super (which returns false, allowing Shiro to handle 401)
    boolean result = filter.onLoginFailure(token, shiroException, request, response);

    // Verify 503 was NOT sent
    verify(response, never()).sendError(eq(HttpServletResponse.SC_SERVICE_UNAVAILABLE),
        eq("Service temporarily unavailable"));
    // Result should be false (super's behavior), which allows Shiro to send 401
    assertThat(result, is(false));
  }

  @Test
  public void testInfrastructureExceptionDeepInCauseChain() throws Exception {
    AuthenticationToken token = new UsernamePasswordToken("admin", "admin123");

    // Create a deeper cause chain to test walking
    DataAccessException dataAccessException = new DataAccessException("Database unavailable");
    RuntimeException wrapperException = new RuntimeException("Wrapper", dataAccessException);
    AuthenticationException shiroException =
        new AuthenticationException("Authentication failed", wrapperException);

    // Call onLoginFailure
    boolean result = filter.onLoginFailure(token, shiroException, request, response);

    // Verify Retry-After header was set
    verify(response).setHeader(eq("Retry-After"), eq("60"));
    // Verify 503 was sent (even though exception is deeper in chain)
    verify(response).sendError(eq(HttpServletResponse.SC_SERVICE_UNAVAILABLE),
        eq("Service temporarily unavailable"));
    assertThat(result, is(false));
  }

  @Test
  public void testOnLoginFailure_rateLimited_sends429() throws Exception {
    AuthRateLimiterService rateLimiterService = mock(AuthRateLimiterService.class);
    injectField(filter, "rateLimiterService", rateLimiterService);

    AuthenticationToken token = new UsernamePasswordToken("admin", "wrongpassword");
    AuthenticationException e = new AuthenticationException("bad credentials",
        new IncorrectCredentialsException());

    when(rateLimiterService.checkAndRecord("admin")).thenReturn(new RateLimitResult(30L, 6));

    boolean result = filter.onLoginFailure(token, e, request, response);

    verify(rateLimiterService).checkAndRecord("admin");
    verify(response).setHeader("Retry-After", "30");
    verify(response).sendError(429, "Too many authentication attempts");
    assertThat(result, is(false));
  }

  @Test
  public void testOnLoginFailure_notRateLimited_doesNotSend429() throws Exception {
    AuthRateLimiterService rateLimiterService = mock(AuthRateLimiterService.class);
    injectField(filter, "rateLimiterService", rateLimiterService);

    AuthenticationToken token = new UsernamePasswordToken("admin", "wrongpassword");
    AuthenticationException e = new AuthenticationException("bad credentials",
        new IncorrectCredentialsException());

    when(rateLimiterService.checkAndRecord("admin")).thenReturn(null);

    // onLoginFailure falls through to super which returns false without 429
    filter.onLoginFailure(token, e, request, response);

    verify(rateLimiterService).checkAndRecord("admin");
    verify(response, never()).sendError(429, "Too many authentication attempts");
  }

  @Test
  public void testOnAccessDenied_preAuthBlocked_sends429WithoutHittingShiro() throws Exception {
    AuthRateLimiterService rateLimiterService = mock(AuthRateLimiterService.class);
    injectField(filter, "rateLimiterService", rateLimiterService);

    when(rateLimiterService.check("admin")).thenReturn(new RateLimitResult(30L, 6));
    // Basic YWRtaW46d3Jvbmdwc3M= = admin:wrongpss (has credentials — isLoginAttempt returns true)
    when(request.getHeader("Authorization")).thenReturn("Basic YWRtaW46d3Jvbmdwc3M=");
    when(request.getAttribute(DefaultSubjectContext.SESSION_CREATION_ENABLED)).thenReturn(null);

    boolean result = filter.onAccessDenied(request, response);

    verify(rateLimiterService).check("admin");
    verify(response).setHeader("Retry-After", "30");
    verify(response).sendError(429, "Too many authentication attempts");
    assertThat(result, is(false));
  }

  @Test
  public void testOnLoginSuccess_recordsSuccess() throws Exception {
    AuthRateLimiterService rateLimiterService = mock(AuthRateLimiterService.class);
    injectField(filter, "rateLimiterService", rateLimiterService);
    Subject subject = mock(Subject.class);
    when(subject.getPrincipal()).thenReturn("admin");
    AuthenticationToken token = new UsernamePasswordToken("admin", "password");

    filter.onLoginSuccess(token, subject, request, response);

    verify(rateLimiterService).recordSuccess("admin");
  }

  @Test
  public void testInfrastructureExceptionFallbackOnIOException() throws Exception {
    AuthenticationToken token = new UsernamePasswordToken("admin", "admin123");

    // Create wrapped database exception
    DataAccessException dataAccessException = new DataAccessException("Database unavailable");
    AuthenticationException shiroException =
        new AuthenticationException("Authentication failed", dataAccessException);

    // Mock sendError to throw IOException
    doThrow(new IOException("Network error")).when(response)
        .sendError(eq(HttpServletResponse.SC_SERVICE_UNAVAILABLE), eq("Service temporarily unavailable"));

    // Call onLoginFailure
    boolean result = filter.onLoginFailure(token, shiroException, request, response);

    // Verify 503 was attempted
    verify(response).sendError(eq(HttpServletResponse.SC_SERVICE_UNAVAILABLE),
        eq("Service temporarily unavailable"));
    // Result should be false (super's fallback behavior)
    assertThat(result, is(false));
  }

  @Test
  public void testGetRateLimitKey_forUsernamePasswordToken_returnsUsername() throws Exception {
    AuthenticationToken token = new UsernamePasswordToken("testuser", "password");

    String rateLimitKey = filter.getRateLimitKey(token);

    assertThat(rateLimitKey, is("testuser"));
  }

  @Test
  public void testGetRateLimitKey_forApiKeyToken_returnsTokenHash() throws Exception {
    // Create an API key token with a known token value
    char[] tokenValue = "my-secret-api-key".toCharArray();
    NexusApiKeyAuthenticationToken token = new NexusApiKeyAuthenticationToken("NuGetApiKey", tokenValue, "192.168.1.1");

    String rateLimitKey = filter.getRateLimitKey(token);

    // The rate limit key should be a hash, not the raw token or the extractor name
    assertThat(rateLimitKey.length(), is(64)); // SHA-256 produces 64 hex characters
    assertThat(rateLimitKey.matches("[0-9a-f]+"), is(true)); // Should be lowercase hex

    // Verify the hash is deterministic (same input produces same hash)
    String rateLimitKey2 = filter.getRateLimitKey(
        new NexusApiKeyAuthenticationToken("NuGetApiKey", "my-secret-api-key".toCharArray(), "192.168.1.2"));
    assertThat(rateLimitKey2, is(rateLimitKey));
  }

  @Test
  public void testGetRateLimitKey_differentTokensProduceDifferentHashes() throws Exception {
    NexusApiKeyAuthenticationToken token1 = new NexusApiKeyAuthenticationToken(
        "NuGetApiKey", "token-one".toCharArray(), "192.168.1.1");
    NexusApiKeyAuthenticationToken token2 = new NexusApiKeyAuthenticationToken(
        "NuGetApiKey", "token-two".toCharArray(), "192.168.1.1");

    String hash1 = filter.getRateLimitKey(token1);
    String hash2 = filter.getRateLimitKey(token2);

    // Different tokens should produce different hashes
    assertThat(hash1.equals(hash2), is(false));
  }

  @Test
  public void testGetRateLimitKey_sameTokenDifferentExtractorName_sameHash() throws Exception {
    // The hash should be based on the token value, not the extractor name
    NexusApiKeyAuthenticationToken nugetToken = new NexusApiKeyAuthenticationToken(
        "NuGetApiKey", "same-token-value".toCharArray(), "192.168.1.1");
    NexusApiKeyAuthenticationToken npmToken = new NexusApiKeyAuthenticationToken(
        "NpmToken", "same-token-value".toCharArray(), "192.168.1.1");

    String nugetHash = filter.getRateLimitKey(nugetToken);
    String npmHash = filter.getRateLimitKey(npmToken);

    // Same token value should produce same hash regardless of extractor name
    assertThat(nugetHash, is(npmHash));
  }

  @Test
  public void testOnLoginFailure_apiKeyToken_usesTokenBasedRateLimit() throws Exception {
    AuthRateLimiterService rateLimiterService = mock(AuthRateLimiterService.class);
    injectField(filter, "rateLimiterService", rateLimiterService);

    char[] tokenValue = "test-api-key".toCharArray();
    NexusApiKeyAuthenticationToken token = new NexusApiKeyAuthenticationToken("NuGetApiKey", tokenValue, "192.168.1.1");
    AuthenticationException e = new AuthenticationException("bad credentials",
        new IncorrectCredentialsException());

    // The expected hash of "test-api-key"
    String expectedHash = filter.getRateLimitKey(token);
    when(rateLimiterService.checkAndRecordByToken(expectedHash)).thenReturn(new RateLimitResult(30L, 6));

    boolean result = filter.onLoginFailure(token, e, request, response);

    // Verify token-based rate limiting was used, not username-based
    verify(rateLimiterService).checkAndRecordByToken(expectedHash);
    verify(rateLimiterService, never()).checkAndRecord(anyString());
    verify(response).sendError(429, "Too many authentication attempts");
    assertThat(result, is(false));
  }

  @Test
  public void testOnLoginSuccess_apiKeyToken_usesTokenBasedRateLimit() throws Exception {
    AuthRateLimiterService rateLimiterService = mock(AuthRateLimiterService.class);
    injectField(filter, "rateLimiterService", rateLimiterService);
    Subject subject = mock(Subject.class);
    when(subject.getPrincipal()).thenReturn("resolved-user");

    char[] tokenValue = "test-api-key".toCharArray();
    NexusApiKeyAuthenticationToken token = new NexusApiKeyAuthenticationToken("NuGetApiKey", tokenValue, "192.168.1.1");

    filter.onLoginSuccess(token, subject, request, response);

    // The expected hash of "test-api-key"
    String expectedHash = filter.getRateLimitKey(token);
    verify(rateLimiterService).recordSuccessByToken(expectedHash);
    verify(rateLimiterService, never()).recordSuccess(anyString());
  }

  @Test
  public void testGetRateLimitKey_forApiKeyToken_computesCorrectHash() throws Exception {
    // Create an API key token with a known token value
    char[] tokenValue = "test-api-key".toCharArray();
    NexusApiKeyAuthenticationToken apiToken =
        new NexusApiKeyAuthenticationToken("NuGetApiKey", tokenValue, "192.168.1.1");

    String expectedHash = filter.getRateLimitKey(apiToken);

    // Verify the hash is correct
    assertThat(expectedHash.length(), is(64)); // SHA-256 hex string
    assertThat(expectedHash.matches("[0-9a-f]+"), is(true));
  }

  @Test
  public void testHashToken_nullToken_throwsIllegalArgumentException() throws Exception {
    // Use reflection to call the private hashToken method
    Method hashTokenMethod = NexusBasicHttpAuthenticationFilter.class.getDeclaredMethod("hashToken", char[].class);
    hashTokenMethod.setAccessible(true);

    try {
      hashTokenMethod.invoke(null, (char[]) null);
      assertThat("Expected IllegalArgumentException", false, is(true));
    }
    catch (java.lang.reflect.InvocationTargetException e) {
      assertThat(e.getCause(), is(notNullValue()));
      assertThat(e.getCause() instanceof IllegalArgumentException, is(true));
      assertThat(e.getCause().getMessage(), containsString("Token must not be null or empty"));
    }
  }

  @Test
  public void testHashToken_emptyToken_throwsIllegalArgumentException() throws Exception {
    Method hashTokenMethod = NexusBasicHttpAuthenticationFilter.class.getDeclaredMethod("hashToken", char[].class);
    hashTokenMethod.setAccessible(true);

    try {
      hashTokenMethod.invoke(null, new char[0]);
      assertThat("Expected IllegalArgumentException", false, is(true));
    }
    catch (java.lang.reflect.InvocationTargetException e) {
      assertThat(e.getCause(), is(notNullValue()));
      assertThat(e.getCause() instanceof IllegalArgumentException, is(true));
      assertThat(e.getCause().getMessage(), containsString("Token must not be null or empty"));
    }
  }

  @Test
  public void testGetRateLimitKey_nullCredentials_fallsBackToPrincipal() throws Exception {
    // Create a mock token that returns null credentials
    AuthenticationToken mockToken = mock(AuthenticationToken.class);
    when(mockToken.getCredentials()).thenReturn(null);
    when(mockToken.getPrincipal()).thenReturn("fallback-principal");

    // This tests the getCredentialsSafely defensive path
    // Since mockToken is not NexusApiKeyAuthenticationToken, it uses principal
    String key = filter.getRateLimitKey(mockToken);
    assertThat(key, is("fallback-principal"));
  }

  @Test
  public void testTokenIsolation_differentTokensIndependentRateLimits() throws Exception {
    // Test the exact scenario from NEXUS-53007:
    // Token A gets rate limited, Token B should NOT be affected
    AuthRateLimiterService rateLimiterService = mock(AuthRateLimiterService.class);
    injectField(filter, "rateLimiterService", rateLimiterService);

    char[] tokenA = "alice-invalid-token".toCharArray();
    char[] tokenB = "bob-different-token".toCharArray();

    NexusApiKeyAuthenticationToken tokenAlice = new NexusApiKeyAuthenticationToken(
        "NuGetApiKey", tokenA, "192.168.1.1");
    NexusApiKeyAuthenticationToken tokenBob = new NexusApiKeyAuthenticationToken(
        "NuGetApiKey", tokenB, "192.168.1.2");

    String hashAlice = filter.getRateLimitKey(tokenAlice);
    String hashBob = filter.getRateLimitKey(tokenBob);

    // Verify different hashes
    assertThat(hashAlice.equals(hashBob), is(false));

    // Simulate Alice's rate limit
    when(rateLimiterService.checkByToken(hashAlice)).thenReturn(new RateLimitResult(30L, 6));
    when(rateLimiterService.checkByToken(hashBob)).thenReturn(null);

    // Alice should be blocked
    assertThat(rateLimiterService.checkByToken(hashAlice), is(notNullValue()));
    // Bob should NOT be blocked
    assertThat(rateLimiterService.checkByToken(hashBob), is(nullValue()));
  }

  @Test
  public void testHashDoesNotLeakTokenInLogs() throws Exception {
    // Verify that the hash doesn't expose the raw token
    char[] secretToken = "super-secret-api-key-12345".toCharArray();
    NexusApiKeyAuthenticationToken token = new NexusApiKeyAuthenticationToken(
        "NuGetApiKey", secretToken, "192.168.1.1");

    String hash = filter.getRateLimitKey(token);

    // The hash should NOT contain the raw token
    assertThat(hash.contains("super-secret"), is(false));
    assertThat(hash.contains("api-key"), is(false));
    assertThat(hash.matches("[0-9a-f]{64}"), is(true));
  }

  @Test
  public void testSuccessPathIsolation_betweenTokens() throws Exception {
    // Test success path: Token A fails multiple times, Token B succeeds
    AuthRateLimiterService rateLimiterService = mock(AuthRateLimiterService.class);
    injectField(filter, "rateLimiterService", rateLimiterService);
    Subject subject = mock(Subject.class);
    when(subject.getPrincipal()).thenReturn("resolved-user");

    char[] tokenA = "failing-token".toCharArray();
    char[] tokenB = "working-token".toCharArray();

    NexusApiKeyAuthenticationToken nexusTokenA = new NexusApiKeyAuthenticationToken(
        "NuGetApiKey", tokenA, "192.168.1.1");
    NexusApiKeyAuthenticationToken nexusTokenB = new NexusApiKeyAuthenticationToken(
        "NuGetApiKey", tokenB, "192.168.1.2");

    String hashA = filter.getRateLimitKey(nexusTokenA);
    String hashB = filter.getRateLimitKey(nexusTokenB);

    // Token B success should clear only Token B's rate limit
    filter.onLoginSuccess(nexusTokenB, subject, request, response);

    verify(rateLimiterService).recordSuccessByToken(hashB);
    verify(rateLimiterService, never()).recordSuccessByToken(hashA);
  }

  @Test
  public void testValidApiKeyAuthenticatesViaNewPath() throws Exception {
    // End-to-end test that a valid API key still authenticates via the new code path
    AuthRateLimiterService rateLimiterService = mock(AuthRateLimiterService.class);
    injectField(filter, "rateLimiterService", rateLimiterService);
    Subject subject = mock(Subject.class);
    when(subject.getPrincipal()).thenReturn("resolved-admin-user");

    char[] validToken = "valid-api-key-for-user".toCharArray();
    NexusApiKeyAuthenticationToken token = new NexusApiKeyAuthenticationToken(
        "NuGetApiKey", validToken, "192.168.1.1");

    String expectedHash = filter.getRateLimitKey(token);

    // Success path should clear the rate limit for the correct hash
    filter.onLoginSuccess(token, subject, request, response);

    verify(rateLimiterService).recordSuccessByToken(expectedHash);

    // Verify the subject principal is used for request attributes
    verify(request).setAttribute(eq("nexus.user.principal"), eq("resolved-admin-user"));
    verify(request).setAttribute(eq("nexus.user.id"), eq("resolved-admin-user"));
  }

  /**
   * Malformed base64 in the Authorization header is rethrown as a
   * {@link NexusBasicHttpAuthenticationFilter.MalformedAuthorizationHeaderException} so {@code cleanup}
   * can answer 400 rather than 500 (NEXUS-53778).
   */
  @Test
  public void testCreateToken_malformedBase64_throwsMalformedAuthorizationHeaderException() {
    when(request.getHeader("Authorization")).thenReturn("Basic +++invalid===base64+++");
    when(request.getRemoteAddr()).thenReturn("192.168.1.100");

    assertThrows(NexusBasicHttpAuthenticationFilter.MalformedAuthorizationHeaderException.class,
        () -> filter.createToken(request, response));
  }

  /**
   * A base64 blob whose length is not a multiple of four cannot be decoded and is rethrown as
   * {@link NexusBasicHttpAuthenticationFilter.MalformedAuthorizationHeaderException} (NEXUS-53778).
   */
  @Test
  public void testCreateToken_base64WrongLength_throwsMalformedAuthorizationHeaderException() {
    // 13 chars, all valid base64 alphabet, but the length is not a multiple of four
    when(request.getHeader("Authorization")).thenReturn("Basic invalidbase64");
    when(request.getRemoteAddr()).thenReturn("192.168.1.100");

    assertThrows(NexusBasicHttpAuthenticationFilter.MalformedAuthorizationHeaderException.class,
        () -> filter.createToken(request, response));
  }

  /**
   * Test that valid base64 credentials create a proper token.
   */
  @Test
  public void testCreateTokenValidBase64ReturnsValidToken() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Basic YWRtaW46YWRtaW4xMjM=");
    when(request.getHeader("Host")).thenReturn("localhost:8081");

    AuthenticationToken token = filter.createToken(request, response);

    assertThat(token, is(notNullValue()));
    assertThat(token.getPrincipal(), is("admin"));
  }

  /**
   * Test that empty credentials (Og==) are handled correctly for anonymous access.
   */
  @Test
  public void testCreateTokenEmptyCredentials() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Basic Og==");
    when(request.getHeader("Host")).thenReturn("localhost:8081");

    AuthenticationToken token = filter.createToken(request, response);

    assertThat(token.getPrincipal(), is(""));
  }

  /**
   * A request with no Authorization header is not "malformed": createToken returns a normal,
   * empty-principal token and does not throw, so the 400 path never fires. Missing credentials stay a
   * 401 challenge handled by Shiro. Guards the narrowness of the malformed -> 400 mapping (NEXUS-53778).
   */
  @Test
  public void testCreateToken_noAuthorizationHeader_returnsAnonymousToken() {
    when(request.getHeader("Authorization")).thenReturn(null);

    AuthenticationToken token = filter.createToken(request, response);

    assertThat(token, is(notNullValue()));
    assertThat(token.getPrincipal(), is(""));
  }

  /**
   * End-to-end: drives the real Shiro chain
   * ({@code onAccessDenied -> super.onAccessDenied -> executeLogin -> createToken}) to prove the
   * unchecked {@link NexusBasicHttpAuthenticationFilter.MalformedAuthorizationHeaderException}
   * propagates unwrapped all the way to {@code cleanup}, which answers 400. This is the premise of
   * NEXUS-53778 and guards against a future shiro-web change that wraps createToken failures.
   */
  @Test
  public void testOnAccessDenied_malformedBase64_propagatesToCleanupAsBadRequest() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Basic ${NPM_TOKEN}");
    when(request.getRemoteAddr()).thenReturn("192.168.1.100");

    Exception thrown = assertThrows(
        NexusBasicHttpAuthenticationFilter.MalformedAuthorizationHeaderException.class,
        () -> filter.onAccessDenied(request, response));

    filter.cleanup(request, response, thrown);

    verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed Authorization header");
  }

  /**
   * End-to-end with the pre-auth rate limiter enabled: {@code onAccessDenied} parses the token itself
   * before delegating to Shiro, so a malformed header must still surface as
   * {@link NexusBasicHttpAuthenticationFilter.MalformedAuthorizationHeaderException} for cleanup to
   * answer 400 (NEXUS-53778).
   */
  @Test
  public void testOnAccessDenied_malformedBase64_withRateLimiter_propagatesToCleanupAsBadRequest() throws Exception {
    AuthRateLimiterService rateLimiterService = mock(AuthRateLimiterService.class);
    injectField(filter, "rateLimiterService", rateLimiterService);

    when(request.getHeader("Authorization")).thenReturn("Basic ${NPM_TOKEN}");
    when(request.getRemoteAddr()).thenReturn("192.168.1.100");

    Exception thrown = assertThrows(
        NexusBasicHttpAuthenticationFilter.MalformedAuthorizationHeaderException.class,
        () -> filter.onAccessDenied(request, response));

    filter.cleanup(request, response, thrown);

    verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed Authorization header");
  }

  /**
   * End-to-end: a request with no Authorization header must produce a 401 challenge
   * ({@code WWW-Authenticate}), never a 400. Guards the malformed -> 400 mapping from widening to
   * anonymous requests (e.g. anonymous v1 Docker search) (NEXUS-53778).
   */
  @Test
  public void testOnAccessDenied_noAuthorizationHeader_sends401ChallengeNot400() throws Exception {
    when(request.getHeader("Authorization")).thenReturn(null);

    boolean result = filter.onAccessDenied(request, response);

    verify(response).setHeader(eq("WWW-Authenticate"), anyString());
    verify(response, never()).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    assertThat(result, is(false));
  }

  @Test
  public void testCreateToken_illegalBase64Char_throwsMalformedAuthorizationHeaderException() {
    when(request.getHeader("Authorization")).thenReturn("Basic :::invalid");
    when(request.getRemoteAddr()).thenReturn("192.168.1.100");

    assertThrows(NexusBasicHttpAuthenticationFilter.MalformedAuthorizationHeaderException.class,
        () -> filter.createToken(request, response));
  }

  /**
   * The prod symptom (NEXUS-53778): {@code Authorization: Basic ${NPM_TOKEN}} — an unexpanded
   * placeholder that is not valid base64. {@code createToken} throws, and {@code cleanup} must
   * translate that to a 400 and not rethrow (so it never surfaces as a 500).
   */
  @Test
  public void testCleanup_malformedAuthorizationHeader_sends400() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Basic ${NPM_TOKEN}");
    when(request.getRemoteAddr()).thenReturn("192.168.1.100");

    Exception malformed = assertThrows(
        NexusBasicHttpAuthenticationFilter.MalformedAuthorizationHeaderException.class,
        () -> filter.createToken(request, response));

    filter.cleanup(request, response, malformed);

    verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed Authorization header");
  }

  /**
   * Test that cleanup() properly rethrows ServletExceptions with IllegalArgumentException causes.
   * Since base64 handling moved to createToken(), cleanup() no longer has special handling
   * for IllegalArgumentException, so it passes through to the parent.
   */
  @Test
  public void testCleanup_servletExceptionWithIllegalArgumentException_rethrown() throws Exception {
    IllegalArgumentException otherException = new IllegalArgumentException("Some other error");
    ServletException servletException = new ServletException(otherException);

    ServletException e =
        assertThrows(ServletException.class, () -> filter.cleanup(request, response, servletException));
    assertThat(e.getCause(), is(otherException));
  }

  @Test
  public void testGetRateLimitKeyEmptyPrincipalReturnsNull() throws Exception {
    // Create a token with empty principal (as returned for malformed base64)
    AuthenticationToken token = new UsernamePasswordToken("", "password");

    String rateLimitKey = filter.getRateLimitKey(token);

    assertThat(rateLimitKey, is(nullValue()));
  }

  @Test
  public void testOnLoginFailureEmptyPrincipalSkipsRateLimit() throws Exception {
    AuthRateLimiterService rateLimiterService = mock(AuthRateLimiterService.class);
    injectField(filter, "rateLimiterService", rateLimiterService);

    // Token with empty principal (simulating malformed base64 case)
    AuthenticationToken token = new UsernamePasswordToken("", "");
    AuthenticationException e = new AuthenticationException("bad credentials",
        new IncorrectCredentialsException());

    boolean result = filter.onLoginFailure(token, e, request, response);

    // Verify rate limiter was NOT called (skipped due to null key)
    verify(rateLimiterService, never()).checkAndRecord(anyString());
    verify(rateLimiterService, never()).checkAndRecordByToken(anyString());
    // Verify no 429 was sent - should fall through to normal 401 handling
    verify(response, never()).sendError(eq(429), anyString());
    assertThat(result, is(false));
  }

  private static void injectField(final Object target, final String name, final Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }
}
