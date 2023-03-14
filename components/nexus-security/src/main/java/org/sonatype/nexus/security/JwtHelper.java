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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.Cookie;

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.security.jwt.JwtSecretChanged;
import org.sonatype.nexus.security.jwt.JwtVerificationException;
import org.sonatype.nexus.security.jwt.JwtVerifier;
import org.sonatype.nexus.security.jwt.SecretStore;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import org.apache.shiro.subject.Subject;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.common.app.FeatureFlags.JWT_ENABLED;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SECURITY;

/**
 * Helper to create, decode, verify and refresh JWT cookie
 *
 * @since 3.38
 */
@Named
@ManagedLifecycle(phase = SECURITY)
@Singleton
@FeatureFlag(name = JWT_ENABLED)
public class JwtHelper
    extends StateGuardLifecycleSupport
    implements EventAware
{
  public static final String JWT_COOKIE_NAME = "NXSESSIONID";

  public static final String ISSUER = "sonatype";

  public static final String REALM = "realm";

  public static final String USER = "user";

  public static final String USER_SESSION_ID = "userSessionId";

  private final int expirySeconds;

  private final String contextPath;

  private final Provider<SecretStore> secretStoreProvider;

  private JwtVerifier verifier;

  @Inject
  public JwtHelper(
      @Named("${nexus.jwt.expiry:-1800}") final int expirySeconds,
      @Named("${nexus-context-path}") final String contextPath,
      final Provider<SecretStore> secretStoreProvider)
  {
    checkState(expirySeconds >= 0, "JWT expiration period should be positive");
    this.expirySeconds = expirySeconds;
    this.contextPath = checkNotNull(contextPath);
    this.secretStoreProvider = checkNotNull(secretStoreProvider);
  }

  @Override
  protected void doStart() throws Exception {
    SecretStore store = secretStoreProvider.get();
    if (!store.getSecret().isPresent()) {
      // the new secret will be generated as UUID only if it is not presented yet.
      store.generateNewSecret();
    }
    // we have to read the generated secret from the DB since another node may write it
    verifier = new JwtVerifier(loadSecret());
  }

  /**
   * Generates a new JWT and makes cookie to store it
   */
  public Cookie createJwtCookie(final Subject subject) {
    checkNotNull(subject);

    String username = subject.getPrincipal().toString();
    Optional<String> realm = subject.getPrincipals().getRealmNames().stream().findFirst();

    return createJwtCookie(username, realm.orElse(null));
  }

  /**
   * Verify jwt, refresh if it's valid and make new cookie
   */
  public Cookie verifyAndRefreshJwtCookie(final String jwt) throws JwtVerificationException {
    checkNotNull(jwt);

    DecodedJWT decoded = verifyJwt(jwt);

    return createJwtCookie(decoded.getClaim(USER).asString(),
        decoded.getClaim(REALM).asString(),
        decoded.getClaim(USER_SESSION_ID).asString());
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
    return expirySeconds;
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

  private Cookie createJwtCookie(final String user, final String realm) {
    String userSessionId = UUID.randomUUID().toString();
    return createJwtCookie(user, realm, userSessionId);
  }

  private Cookie createJwtCookie(final String user, final String realm, final String userSessionId) {
    String jwt = createToken(user, realm, userSessionId);
    return createCookie(jwt);
  }

  private String createToken(final String user, final String realm, final String userSessionId) {
    Date issuedAt = new Date();
    Date expiresAt = getExpiresAt(issuedAt);
    return JWT.create()
        .withIssuer(ISSUER)
        .withClaim(USER, user)
        .withClaim(REALM, realm)
        .withClaim(USER_SESSION_ID, userSessionId)
        .withIssuedAt(issuedAt)
        .withExpiresAt(expiresAt)
        .sign(verifier.getAlgorithm());
  }

  private Cookie createCookie(final String jwt) {
    Cookie cookie = new Cookie(JWT_COOKIE_NAME, jwt);
    cookie.setMaxAge(this.expirySeconds);
    cookie.setPath(contextPath);
    cookie.setHttpOnly(true);

    return cookie;
  }

  private Date getExpiresAt(final Date issuedAt) {
    return new Date(issuedAt.getTime() + TimeUnit.SECONDS.toMillis(this.expirySeconds));
  }

  private String loadSecret() {
    return secretStoreProvider.get()
        .getSecret()
        .orElseThrow(() -> new IllegalStateException("JWT secret not found in datastore"));
  }
}
