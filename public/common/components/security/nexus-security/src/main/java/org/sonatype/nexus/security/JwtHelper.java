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
package org.sonatype.nexus.security;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.servlet.http.Cookie;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.security.jwt.JwtSecretChanged;
import org.sonatype.nexus.security.jwt.JwtVerificationException;
import org.sonatype.nexus.security.jwt.JwtVerifier;
import org.sonatype.nexus.security.jwt.SecretStore;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.eventbus.Subscribe;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.inject.Provider;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.common.app.FeatureFlags.JWT_ENABLED;
import static org.sonatype.nexus.common.app.FeatureFlags.NXSESSIONID_SECURE_COOKIE_NAMED_VALUE;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SECURITY;

/**
 * Helper to create, decode, verify and refresh JWT cookie
 *
 * @since 3.38
 */
@Component
@ManagedLifecycle(phase = SECURITY)
@ConditionalOnProperty(name = JWT_ENABLED, havingValue = "true")
public class JwtHelper
    extends StateGuardLifecycleSupport
    implements EventAware
{
  public static final String JWT_COOKIE_NAME = "NXSESSIONID";

  public static final String ISSUER = "sonatype";

  public static final String REALM = "realm";

  public static final String USER = "user";

  public static final String USER_SESSION_ID = "userSessionId";

  public static final String ID_TOKEN = "id_token";

  private final AtomicInteger expirySeconds;

  private final String contextPath;

  private final Provider<SecretStore> secretStoreProvider;

  private final boolean cookieSecure;

  private JwtVerifier verifier;

  public JwtHelper(final int expirySeconds, final String contextPath, final Provider<SecretStore> secretStoreProvider) {
    this(expirySeconds, contextPath, secretStoreProvider, true);
  }

  @Autowired
  public JwtHelper(
      @Value("${nexus.jwt.expiry:1800}") final int expirySeconds,
      @Value("${nexus-context-path:#{null}}") final String contextPath,
      final Provider<SecretStore> secretStoreProvider,
      @Value(NXSESSIONID_SECURE_COOKIE_NAMED_VALUE) final boolean cookieSecure)
  {
    checkState(expirySeconds >= 0, "JWT expiration period should be positive");
    this.expirySeconds = new AtomicInteger(expirySeconds);
    this.contextPath = checkNotNull(contextPath);
    this.secretStoreProvider = checkNotNull(secretStoreProvider);
    this.cookieSecure = cookieSecure;
  }

  @Override
  protected void doStart() throws Exception {
    SecretStore store = secretStoreProvider.get();
    if (store.getSecret().isEmpty()) {
      // the new secret will be generated as UUID only if it is not presented yet.
      store.generateNewSecret();
    }
    // we have to read the generated secret from the DB since another node may write it
    verifier = new JwtVerifier(loadSecret());
  }

  /**
   * Generates a new JWT and makes cookie to store it.
   * The returned Cookie will have a value for {@link Cookie#getSecure()} dependent on two conditions:
   *
   * 1. the value of the second argument, which indicates if this request originated from an HTTPS request
   * 2. the value of the nexus.session.secureCookie property.
   *
   * nexus.session.secureCookie is true by default, however if a JWT cookie is being sent on an HTTP only
   * request it cannot have true for {@link Cookie#getSecure()}.
   *
   * @param subject the target subject
   * @param secureRequest true if the cookie is associated with a Secure request.
   */
  public Cookie createJwtCookie(final Subject subject, final boolean secureRequest) {
    checkNotNull(subject);

    String username = subject.getPrincipal().toString();
    Optional<String> realm = subject.getPrincipals().getRealmNames().stream().findFirst();
    String userSessionId = UUID.randomUUID().toString();

    String token = createToken(username, realm.orElse(null), userSessionId);

    return createCookie(token, secureRequest);
  }

  /**
   * Refresh a previously-verified JWT into a new cookie. Callers should obtain {@code decoded}
   * from {@link #verifyJwt(String)}.
   */
  public Cookie refreshJwtCookie(final DecodedJWT decoded, final boolean secureRequest) {
    checkNotNull(decoded);
    String newJwt = createToken(
        decoded.getClaim(USER).asString(),
        decoded.getClaim(REALM).asString(),
        decoded.getClaim(USER_SESSION_ID).asString());
    return createCookie(newJwt, secureRequest);
  }

  /**
   * Verifies and decode token
   */
  public DecodedJWT verifyJwt(final String jwt) throws JwtVerificationException {
    return verifier.verify(jwt);
  }

  /**
   * Gets expiry in seconds
   */
  public int getExpirySeconds() {
    return expirySeconds.get();
  }

  /**
   * Updates the JWT expiry duration dynamically.
   * Thread-safe for concurrent access.
   *
   * Note: Zero is not allowed for JWT tokens (they must have an expiry).
   * Use a very large value (e.g., 1 year) to simulate "never expires".
   *
   * @param expirySeconds the new expiry duration in seconds (must be positive)
   */
  public synchronized void setExpirySeconds(final int expirySeconds) {
    checkState(expirySeconds > 0, "JWT expiration period must be positive (zero not allowed for JWT tokens)");
    log.info("Updating JWT expiry from {} to {} seconds", this.expirySeconds.get(), expirySeconds);
    this.expirySeconds.set(expirySeconds);
  }

  /**
   * Handles a JWT secret change event.
   *
   * @param event the {@link JwtSecretChanged} with the new secret.
   */
  @Subscribe
  public void on(final JwtSecretChanged event) {
    log.debug("JWT secret has changed. Reset the cookies");
    verifier = new JwtVerifier(loadSecret());
  }

  private String createToken(
      final String user,
      final String realm,
      final String userSessionId)
  {
    Date issuedAt = new Date();
    Date expiresAt = getExpiresAt(issuedAt);
    JWTCreator.Builder jwtBuilder = JWT.create()
        .withIssuer(ISSUER)
        .withClaim(USER, user)
        .withClaim(USER_SESSION_ID, userSessionId)
        .withIssuedAt(issuedAt)
        .withExpiresAt(expiresAt);

    if (realm != null) {
      jwtBuilder.withClaim(REALM, realm);
    }

    return jwtBuilder.sign(verifier.getAlgorithm());
  }

  private Cookie createCookie(final String jwt, final boolean secureRequest) {
    Cookie cookie = new Cookie(JWT_COOKIE_NAME, jwt);
    cookie.setMaxAge(this.expirySeconds.get());
    cookie.setPath(contextPath);
    cookie.setHttpOnly(true);
    cookie.setSecure(cookieSecure && secureRequest);

    return cookie;
  }

  private Date getExpiresAt(final Date issuedAt) {
    return new Date(issuedAt.getTime() + TimeUnit.SECONDS.toMillis(this.expirySeconds.get()));
  }

  private String loadSecret() {
    return secretStoreProvider.get()
        .getSecret()
        .orElseThrow(() -> new IllegalStateException("JWT secret not found in datastore"));
  }
}
