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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.annotation.Nullable;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.datastore.api.DataAccessException;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.security.SecurityFilter.ATTR_USER_ID;
import static org.sonatype.nexus.security.SecurityFilter.ATTR_USER_PRINCIPAL;

/**
 * Nexus security filter providing HTTP BASIC authentication support.
 *
 * Knows about special handling needed for anonymous subjects.
 *
 * Does not create sessions.
 *
 * @since 3.0
 */
@WebFilter(filterName = NexusBasicHttpAuthenticationFilter.NAME)
@Component
public class NexusBasicHttpAuthenticationFilter
    extends BasicHttpAuthenticationFilter
{
  public static final String NAME = "nx-basic-authc";

  // This is the base64 encoded version of ":" i.e. empty credentials, required to allow anonymous v1 Docker search.
  private static final String EMPTY_CREDENTIALS = "Og==";

  /**
   * @since 3.1
   */
  public static final String BASIC_AUTH_REALM = "Sonatype Nexus Repository Manager";

  protected final Logger log = LoggerFactory.getLogger(getClass());

  @Autowired(required = false)
  @Nullable
  private AuthRateLimiterService rateLimiterService;

  @Autowired(required = false)
  @Nullable
  private EventManager eventManager;

  public NexusBasicHttpAuthenticationFilter() {
    setApplicationName(BASIC_AUTH_REALM);
  }

  /**
   * Always use permissive mode, which is needed for anonymous user support.
   */
  @Override
  protected boolean isPermissive(final Object mappedValue) {
    return true;
  }

  /**
   * Disable session creation for all BASIC auth requests.
   */
  @Override
  public boolean onPreHandle(
      final ServletRequest request,
      final ServletResponse response,
      final Object mappedValue) throws Exception
  {
    // Basic auth should never create sessions; we do not want session overhead for non-user clients that supply
    // credentials
    request.setAttribute(DefaultSubjectContext.SESSION_CREATION_ENABLED, Boolean.FALSE);

    return super.onPreHandle(request, response, mappedValue);
  }

  /**
   * Permissive {@link AuthorizationException} 401 and 403 handling.
   */
  @Override
  protected void cleanup(
      final ServletRequest request,
      final ServletResponse response,
      Exception failure) throws ServletException, IOException
  {
    // decode target exception
    Throwable cause = failure;
    if (cause instanceof ServletException) {
      cause = cause.getCause();
    }

    // special handling for authz failures due to permissive
    if (cause instanceof AuthorizationException) {
      // clear the failure
      failure = null;

      Subject subject = getSubject(request, response);
      boolean authenticated = subject.getPrincipal() != null && subject.isAuthenticated();

      if (authenticated) {
        // authenticated subject -> 403 forbidden
        WebUtils.toHttp(response).sendError(HttpServletResponse.SC_FORBIDDEN);
      }
      else {
        // unauthenticated subject -> 401 inform to authenticate
        try {
          // TODO: Should we build in browser detecting to avoid sending 401, should that be its own filter?

          onAccessDenied(request, response);
        }
        catch (Exception e) {
          failure = e;
        }
      }
    }

    super.cleanup(request, response, failure);
  }

  /**
   * Pre-authentication rate limit guard. Short-circuits the normal challenge/login cycle for
   * blocked usernames, sending 429 before Shiro evaluates credentials or issues a 401 challenge.
   *
   * <p>
   * Overriding {@code onAccessDenied} (rather than {@code executeLogin}) is required because
   * {@code HttpAuthenticationFilter.onAccessDenied} unconditionally calls {@code sendChallenge}
   * after {@code executeLogin} returns false, which would overwrite our 429 with a 401.
   */
  @Override
  protected boolean onAccessDenied(final ServletRequest request, final ServletResponse response) throws Exception {
    if (rateLimiterService != null && isLoginAttempt(request, response)) {
      AuthenticationToken token = createToken(request, response);
      if (token != null) {
        // Compute the rate limit key once and reuse — avoids double hashing for API key tokens
        String rateLimitKey = getRateLimitKey(token);
        RateLimitResult limitResult = checkRateLimitForKey(token, rateLimitKey);
        if (limitResult != null) {
          log.debug("Pre-auth rate limit blocking login attempt for key '{}'", truncateHash(rateLimitKey));
          if (eventManager != null) {
            String clientIp = WebUtils.toHttp(request).getRemoteAddr();
            eventManager.post(new AuthRateLimitedEvent(rateLimitKey, limitResult.attemptCount(),
                limitResult.retryAfterSeconds(), clientIp, "BASIC"));
          }
          try {
            HttpServletResponse httpResponse = WebUtils.toHttp(response);
            httpResponse.setHeader("Retry-After", String.valueOf(limitResult.retryAfterSeconds()));
            httpResponse.sendError(429, "Too many authentication attempts");
          }
          catch (IOException ex) {
            log.error("Failed to send 429 response", ex);
          }
          return false;
        }
      }
    }
    return super.onAccessDenied(request, response);
  }

  /**
   * Override to apply rate limiting after a confirmed credential failure and to handle
   * infrastructure exceptions.
   *
   * <p>
   * Order of checks:
   * <ol>
   * <li>Infrastructure failure ({@link DataAccessException}) → 503, counter not incremented.</li>
   * <li>Rate limit counter increment ({@link AuthRateLimiterService#checkAndRecord}) — 429 for the
   * <em>current</em> attempt if the threshold is now exceeded (race-safety), otherwise 401.</li>
   * <li>Delegate to super → 401.</li>
   * </ol>
   */
  @Override
  protected boolean onLoginFailure(
      final AuthenticationToken token,
      final AuthenticationException e,
      final ServletRequest request,
      final ServletResponse response)
  {
    // 1. Infrastructure failures — do not count toward rate limit
    Throwable cause = e;
    int depth = 0;
    final int maxDepth = 20;
    while (cause != null && depth < maxDepth) {
      if (cause instanceof DataAccessException) {
        log.warn("Infrastructure failure during authentication", cause);
        try {
          HttpServletResponse httpResponse = WebUtils.toHttp(response);
          httpResponse.setHeader("Retry-After", "60");
          httpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
              "Service temporarily unavailable");
          return false;
        }
        catch (IOException ex) {
          log.error("Failed to send 503 response, falling back to normal authentication failure handling", ex);
          return super.onLoginFailure(token, e, request, response);
        }
      }
      cause = cause.getCause();
      depth++;
    }

    // 2. Rate limiting — applied after confirmed credential failure
    if (rateLimiterService != null) {
      // Compute the rate limit key once and reuse — avoids double hashing for API key tokens
      String rateLimitKey = getRateLimitKey(token);
      RateLimitResult limitResult = recordFailedAttemptForKey(token, rateLimitKey);
      if (limitResult != null) {
        log.debug("Rate limiting login attempt for key '{}'", truncateHash(rateLimitKey));
        if (eventManager != null) {
          // IP is captured for audit trail only; rate-limit decisions use username or token hash
          String clientIp = WebUtils.toHttp(request).getRemoteAddr();
          eventManager.post(new AuthRateLimitedEvent(rateLimitKey, limitResult.attemptCount(),
              limitResult.retryAfterSeconds(), clientIp, "BASIC"));
        }
        try {
          HttpServletResponse httpResponse = WebUtils.toHttp(response);
          httpResponse.setHeader("Retry-After", String.valueOf(limitResult.retryAfterSeconds()));
          httpResponse.sendError(429, "Too many authentication attempts");
        }
        catch (IOException ex) {
          log.error("Failed to send 429 response", ex);
        }
        return false;
      }
    }

    return super.onLoginFailure(token, e, request, response);
  }

  @Override
  protected boolean onLoginSuccess(
      final AuthenticationToken token,
      final Subject subject,
      final ServletRequest request,
      final ServletResponse response) throws Exception
  {
    if (request instanceof HttpServletRequest) {
      // Prefer the subject principal over the token's for request-log attributes,
      // as these could be different for token-based auth (e.g. LDAP mapped username)
      Object principal = subject.getPrincipal();
      if (principal == null) {
        principal = token.getPrincipal();
      }

      // Attach principal+userId to request so we can use that in the request-log
      request.setAttribute(ATTR_USER_PRINCIPAL, principal);
      request.setAttribute(ATTR_USER_ID, principal.toString());

      if (rateLimiterService != null) {
        clearRateLimitOnSuccess(token);
      }
    }
    return super.onLoginSuccess(token, subject, request, response);
  }

  @Override
  protected boolean isLoginAttempt(final String authzHeader) {
    return !isEmptyCredentials(authzHeader) && super.isLoginAttempt(authzHeader);
  }

  private boolean isEmptyCredentials(final String authzHeader) {
    if (!authzHeader.toLowerCase().contains("basic ")) {
      return false;
    }

    String[] parts = authzHeader.split(" ");
    return parts.length > 1 && parts[1].equals(EMPTY_CREDENTIALS);
  }

  /**
   * Returns the rate limit key for the given authentication token.
   *
   * <p>
   * For API key authentication ({@link NexusApiKeyAuthenticationToken}), uses a SHA-256 hash
   * of the token to isolate rate limits per token, preventing one bad token from affecting
   * all users of the same format.
   *
   * <p>
   * For regular username/password authentication, uses the principal (username).
   *
   * @param token the authentication token
   * @return the rate limit key
   */
  protected String getRateLimitKey(final AuthenticationToken token) {
    if (token instanceof NexusApiKeyAuthenticationToken) {
      // For API keys, use a hash of the token to isolate rate limits per token
      char[] credentials = getCredentialsSafely(token);
      if (credentials == null) {
        // Fallback to principal if credentials cannot be extracted as char[]
        return token.getPrincipal().toString();
      }
      return hashToken(credentials);
    }
    // For regular auth (username/password), use the principal (username)
    return token.getPrincipal().toString();
  }

  /**
   * Checks if the given token is currently rate-limited without incrementing the counter,
   * using a precomputed rate limit key to avoid recomputing the token hash.
   */
  private RateLimitResult checkRateLimitForKey(final AuthenticationToken token, final String rateLimitKey) {
    if (token instanceof NexusApiKeyAuthenticationToken
        && getCredentialsSafely(token) != null) {
      return rateLimiterService.checkByToken(rateLimitKey);
    }
    return rateLimiterService.check(rateLimitKey);
  }

  /**
   * Records a failed authentication attempt and checks if rate limiting should be applied,
   * using a precomputed rate limit key to avoid recomputing the token hash.
   */
  private RateLimitResult recordFailedAttemptForKey(final AuthenticationToken token, final String rateLimitKey) {
    if (token instanceof NexusApiKeyAuthenticationToken
        && getCredentialsSafely(token) != null) {
      return rateLimiterService.checkAndRecordByToken(rateLimitKey);
    }
    return rateLimiterService.checkAndRecord(rateLimitKey);
  }

  /**
   * Clears the rate limit counter on successful authentication.
   */
  private void clearRateLimitOnSuccess(final AuthenticationToken token) {
    String rateLimitKey = getRateLimitKey(token);
    if (token instanceof NexusApiKeyAuthenticationToken
        && getCredentialsSafely(token) != null) {
      rateLimiterService.recordSuccessByToken(rateLimitKey);
    }
    else {
      // Use the token principal (raw username from Authorization header) to match
      // the key used in checkAndRecord, which also reads from the Authorization header
      rateLimiterService.recordSuccess(rateLimitKey);
    }
  }

  /**
   * Safely extracts credentials from an authentication token with type-safe handling.
   *
   * <p>
   * Returns {@code null} if the credentials are not a {@code char[]}. This protects the
   * filter from a {@link ClassCastException} should
   * {@link NexusApiKeyAuthenticationToken#getCredentials()} ever be changed to return a
   * different type ({@code String}, {@code byte[]}, etc.).
   *
   * @param token the authentication token
   * @return the credentials as char[], or null if not a char[] or null
   */
  @Nullable
  private static char[] getCredentialsSafely(final AuthenticationToken token) {
    Object credentials = token.getCredentials();
    if (credentials instanceof char[]) {
      return (char[]) credentials;
    }
    return null;
  }

  /**
   * Computes a SHA-256 hash of the given token for use as a rate limit key.
   * Uses CharBuffer/ByteBuffer to avoid creating intermediate String objects,
   * preserving Shiro's char[] credential model for security.
   *
   * <p>
   * The byte array used for hashing is zeroed after use to minimize exposure.
   *
   * @param token the token characters; must not be null
   * @return a hex-encoded SHA-256 hash of the token
   */
  private static String hashToken(final char[] token) {
    if (token == null || token.length == 0) {
      throw new IllegalArgumentException("Token must not be null or empty");
    }

    byte[] tokenBytes = null;
    boolean allocatedOwn = false;
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      // Convert char[] to byte[] without creating an intermediate String.
      CharBuffer charBuffer = CharBuffer.wrap(token);
      ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);

      // StandardCharsets.UTF_8.encode() may return a direct ByteBuffer with no backing
      // array; calling array() on it throws UnsupportedOperationException, which would
      // let blocked requests bypass rate limiting. Handle both cases safely.
      if (byteBuffer.hasArray()) {
        tokenBytes = byteBuffer.array();
      }
      else {
        tokenBytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(tokenBytes);
        allocatedOwn = true;
      }

      byte[] hash = digest.digest(tokenBytes);
      return bytesToHex(hash);
    }
    catch (NoSuchAlgorithmException e) {
      // SHA-256 is guaranteed to be available on all Java platforms
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
    finally {
      // Zero out the byte array we allocated to minimize credential exposure.
      if (allocatedOwn && tokenBytes != null) {
        Arrays.fill(tokenBytes, (byte) 0);
      }
    }
  }

  /**
   * Truncates a hash for logging purposes to avoid creating stable token fingerprints
   * in shared logs. Returns first 8 characters followed by "...".
   *
   * <p>
   * Non-hash keys (e.g. usernames) shorter than 9 characters are returned unchanged so
   * username logs continue to read naturally.
   *
   * @param key the rate limit key (full hash or username)
   * @return safe-to-log representation
   */
  private static String truncateHash(final String key) {
    if (key == null || key.length() <= 8) {
      return key;
    }
    return key.substring(0, 8) + "...";
  }

  /**
   * Converts a byte array to a lowercase hex string.
   */
  private static String bytesToHex(final byte[] bytes) {
    StringBuilder hexString = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      hexString.append(String.format("%02x", b));
    }
    return hexString.toString();
  }
}
